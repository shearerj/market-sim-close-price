package entity;


import static logger.Logger.log;
import static logger.Logger.Level.DEBUG;
import static logger.Logger.Level.INFO;
import static systemmanager.Consts.INF_TIME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import market.Bid;
import market.PQBid;
import market.Price;
import market.PrivateValue;
import market.Quote;
import market.Transaction;
import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import activity.Liquidate;
import activity.SubmitBid;
import activity.SubmitMultipleBid;
import activity.WithdrawBid;
import event.TimeStamp;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {

	protected final RandPlus rand;
	protected final MarketModel model;
	protected final SIP sip;
	// List of all transactions. Implicitly time ordered due to transactions
	// being created and assigned in time order.
	protected final List<Transaction> transactions;
	protected final Collection<Bid> activeBids;
	
	// Agent parameters
	protected final PrivateValue privateValue;
	protected final TimeStamp arrivalTime;
	protected final int tickSize;

	// Tracking cash flow
	protected int cashBalance;
	protected int positionBalance;
	protected int averageCost;
	protected int realizedProfit;
	// for liquidation
	protected int preLiqPosition;
	protected int preLiqRealizedProfit;
	
	public Agent(int agentID, TimeStamp arrivalTime, MarketModel model,
			PrivateValue privateValue, RandPlus rand, int tickSize) {
		super(agentID);
		this.model = model;
		this.rand = rand;
		this.arrivalTime = arrivalTime;
		this.sip = model.getSIP();
		this.privateValue = privateValue;
		this.tickSize = tickSize;

		this.transactions = new ArrayList<Transaction>();
		this.activeBids = new HashSet<Bid>();
	}

	public abstract Collection<? extends Activity> agentStrategy(
			TimeStamp currentTime);

	public abstract Collection<? extends Activity> agentArrival(
			TimeStamp currentTime);

	/**
	 * @return Method to get the type of the agent.
	 */
	@Deprecated
	// FIXME should return Consts.AgentType corresponding to current agent. Need
	// a good way to do this.
	public String getType() {
		return getClass().getSimpleName();
	}

	/***********************************
	 * Methods for Activities
	 * 
	 **********************************/

	/**
	 * Wrapper method to submit bid to market after checking permissions.
	 */
	public Collection<? extends Activity> submitBid(Market market, Price price,
			int quantity, TimeStamp scheduledTime) {
		return Collections.singleton(new SubmitBid(this, market, price,
				quantity, scheduledTime));
	}

	/**
	 * Wrapper method to submit multiple-point bid to market after checking
	 * permissions.
	 */
	public Collection<? extends Activity> submitMultipleBid(Market market,
			Map<Price, Integer> priceQuantMap, TimeStamp scheduledTime) {
		return Collections.singleton(new SubmitMultipleBid(this, market,
				priceQuantMap, scheduledTime));
	}

	/**
	 * Wrapper method to expire the agent's bid from a market after a specified
	 * duration.
	 * 
	 * TODO how does this work? Expires all bids? The first bid it finds? The
	 * oldest bid? Maybe should contain a reference to the bid in question...
	 */
	public Collection<? extends Activity> expireBid(Market market,
			TimeStamp duration, TimeStamp currentTime) {
		TimeStamp withdrawTime = currentTime.plus(duration);
		log(INFO, currentTime + " | " + market + " " + this + ": bid duration="
				+ duration);
		return Collections.singleton(new WithdrawBid(this, market, withdrawTime));
	}

	/**
	 * Withdraws a bid from the given market.
	 */
	public Collection<? extends Activity> executeWithdrawBid(Market market,
			TimeStamp ts) {
		log(INFO, ts + " | " + this + " withdraw bid from " + market);
		return market.removeBid(this, ts);
	}

	/**
	 * Submits a multiple-point bid to the specified market.
	 * 
	 * @param mkt
	 * @param price
	 * @param quantity
	 * @param ts
	 * @return
	 */
	// TODO swtich form parallel array lists to Map<Price, Integer>
	public Collection<? extends Activity> executeSubmitMultipleBid(Market mkt,
			Map<Price, Integer> priceQuantityMap, TimeStamp ts) {
		log(INFO, ts + " | " + mkt + " " + this + ": +" + priceQuantityMap);

		PQBid pqBid = new PQBid(this, mkt, ts);
		for (Entry<Price, Integer> priceQuant : priceQuantityMap.entrySet()) {
			int quantity = priceQuant.getValue();
			Price price = priceQuant.getKey();
			if (quantity == 0)
				continue; // TODO add check in PQBid instead
			pqBid.addPoint(quantity, price.quantize(tickSize));
		}
		// TODO incorporate multi-point PVs?
		activeBids.add(pqBid);
//		return mkt.addBid(pqBid, ts); FIXME fix this / move to market
		return null;
	}

	/**
	 * Liquidate agent's position at the the value of the global fundamental at
	 * the specified time. Price is determined by the fundamental at the time of
	 * liquidation.
	 */
	public Collection<? extends Activity> liquidateAtFundamental(
			TimeStamp currentTime) {
		log(INFO, currentTime + " | " + this + " liquidating...");
		return Collections.singleton(new Liquidate(this,
				model.getFundamentalAt(currentTime), INF_TIME));
	}

	/**
	 * Liquidates an agent's position at the specified price.
	 */
	public Collection<? extends Activity> executeLiquidate(Price price,
			TimeStamp ts) {

		log(INFO, ts + " | " + this + " pre-liquidation: position="
				+ positionBalance + ", profit=" + realizedProfit);

		// If no net position, no need to liquidate
		if (positionBalance == 0)
			return Collections.emptyList();

		preLiqPosition = positionBalance;
		preLiqRealizedProfit = getRealizedProfit();
		if (positionBalance > 0) {
			// need to sell
			realizedProfit += positionBalance * price.getPrice();
		} else {
			// need to buy
			realizedProfit -= positionBalance * price.getPrice();
		}
		positionBalance = 0;

		log(INFO, ts + " | " + this + " post-liquidation: position="
				+ positionBalance + ", profit=" + realizedProfit + ", price="
				+ price);
		return Collections.emptyList();
	}

	// TODO gives the agent instant data. Should instead process with delay
	public void addTransaction(Transaction trans, TimeStamp currentTime) {
		if (!trans.getBuyer().equals(this) && !trans.getSeller().equals(this))
			throw new IllegalArgumentException(
					"Can only add a transaction that this agent participated in");
		transactions.add(trans);

		// XXX currentTime should equal Trans.getExecTime() so redundant?
		log(INFO, currentTime + " | " + this + " "
				+ "Agent::updateTransactions: New transaction received: "
				+ trans);
		log(INFO, currentTime + " | " + this + " Agent::logTransactions: "
				+ model.getFullName() + ": Current Position=" + positionBalance
				+ ", Realized Profit=" + realizedProfit);
	}

	/**
	 * Computes any unrealized profit based on market bid/ask quotes.
	 */
	public Price getUnrealizedProfit() {
		if (positionBalance == 0)
			return Price.ZERO;

		Price p = null;
		if (positionBalance > 0) {
			// For long position, compare cost to bid quote (buys)
			for (Market market : model.getMarkets())
				if (market.getQuote().getBidPrice().greaterThan(p))
					p = market.getQuote().getBidPrice();
		} else {
			// For short position, compare cost to ask quote (sells)
			for (Market market : model.getMarkets()) {
				if (market.getQuote().getAskPrice().lessThan(p)) {
					p = market.getQuote().getAskPrice();
				}
			}
		}

		log(DEBUG, this.getModel().getFullName() + ": " + this + " bal="
				+ positionBalance + ", p=" + p + ", avgCost=" + averageCost);

		return p == null ? Price.ZERO : new Price(positionBalance
				* (p.getPrice() - averageCost));
	}

	public final TimeStamp getArrivalTime() {
		return arrivalTime;
	}

	public final MarketModel getModel() {
		return model;
	}

	public final PrivateValue getPrivateValue() {
		return privateValue;
	}

	public int getCashBalance() {
		return cashBalance;
	}

	public int getRealizedProfit() {
		return realizedProfit;
	}

	public int getPreLiquidationPosition() {
		return preLiqPosition;
	}

	public int getPreLiquidationProfit() {
		return preLiqRealizedProfit;
	}

	public int getPositionBalance() {
		return positionBalance;
	}

	/**
	 * Iterates through the agent's transactions and calculates it's current
	 * discounted surplus with the specified discount factor
	 * 
	 * @param rho
	 *            Discount factor. 0 means no discounting, while larger positive
	 *            numbers represent larger discounting
	 * @return
	 */
	public double getSurplus(double rho) {
		double surplus = 0; // XXX Price instead of double?

		for (Transaction tr : transactions) {

			TimeStamp submissionTime;
			int sign;
			if (tr.getBuyer().equals(this)) {
				submissionTime = tr.getBuyBid().getSubmitTime();
				sign = 1;
			} else {
				submissionTime = tr.getSellBid().getSubmitTime();
				sign = -1;
			}
			TimeStamp timeToExecution = tr.getExecTime().minus(submissionTime);

			Price fundamental = model.getFundamentalAt(tr.getExecTime()).times(
					tr.getQuantity());
			Price pv = privateValue.getValueFromQuantity(positionBalance,
					tr.getQuantity());
			Price cost = tr.getPrice().times(tr.getQuantity());
			Price transactionSurplus = fundamental.plus(pv).minus(cost).times(
					sign);

			surplus += Math.exp(rho * timeToExecution.longValue())
					* transactionSurplus.getPrice();
		}
		return surplus;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ model.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Agent))
			return false;
		Agent agent = (Agent) obj;
		return super.equals(agent) && model.equals(agent.model);
	}

	@Override
	public String toString() {
		return new String("(" + id + ", " + model + ")");
	}
}
