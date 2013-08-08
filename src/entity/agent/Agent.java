package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.DEBUG;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import activity.AgentStrategy;
import activity.Liquidate;
import entity.Entity;
import entity.infoproc.SIP;
import entity.market.Order;
import entity.market.Price;
import entity.market.PrivateValue;
import entity.market.Transaction;
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
	// TODO Currently unused? Bid should be added to this before it's submitted... Maybe Market
	// should add t othis when a bid is submitted, so when it's removed this can also be updated...
	protected final Collection<Order> activeOrders;

	// Agent parameters
	protected final PrivateValue privateValue;
	protected final TimeStamp arrivalTime;
	protected final int tickSize;

	// Tracking cash flow
	protected int cashBalance;
	protected int positionBalance;
	protected int averageCost;
	protected int realizedProfit;
	// for liquidation XXX Should be moved to MarketMaker? Not sure...
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
		this.activeOrders = new HashSet<Order>();
	}

	public abstract Collection<? extends Activity> agentStrategy(
			TimeStamp currentTime);

	public Collection<? extends Activity> agentArrival(TimeStamp currentTime) {
		return Collections.singleton(new AgentStrategy(this, TimeStamp.IMMEDIATE));
	}

	/**
	 * Liquidate agent's position at the the value of the global fundamental at the specified time.
	 * Price is determined by the fundamental at the time of liquidation.
	 */
	public Collection<? extends Activity> liquidateAtFundamental(
			TimeStamp currentTime) {
		log(INFO, this + " liquidating...");
		return Collections.singleton(new Liquidate(this,
				model.getFundamentalAt(currentTime), TimeStamp.IMMEDIATE));
	}

	/**
	 * Liquidates an agent's position at the specified price.
	 */
	public Collection<? extends Activity> liquidate(Price price, TimeStamp ts) {

		log(INFO, this + " pre-liquidation: position="
				+ positionBalance + ", profit=" + realizedProfit);

		// If no net position, no need to liquidate
		if (positionBalance == 0) return Collections.emptyList();

		preLiqPosition = positionBalance;
		preLiqRealizedProfit = getRealizedProfit();
		if (positionBalance > 0) {
			// need to sell
			realizedProfit += positionBalance * price.getInTicks();
		} else {
			// need to buy
			realizedProfit -= positionBalance * price.getInTicks();
		}
		positionBalance = 0;

		log(INFO, this + " post-liquidation: position="
				+ positionBalance + ", profit=" + realizedProfit + ", price="
				+ price);
		return Collections.emptyList();
	}

	/**
	 * Adds an agent's order to its memory so it knows about it, and can cancel it
	 */
	public void addOrder(Order order) {
		if (!order.getAgent().equals(this))
			throw new IllegalArgumentException("Can't add illegal for a different agent");
		activeOrders.add(order);
	}
	
	/**
	 * Removes order when it's no longer active
	 */
	public void removeOrder(Order order) {
		activeOrders.remove(order);
	}
	
	// TODO gives the agent instant data. Should instead process with delay
	public void addTransaction(Transaction trans) {
		if (!trans.getBuyer().equals(this) && !trans.getSeller().equals(this))
			throw new IllegalArgumentException(
					"Can only add a transaction that this agent participated in");
		transactions.add(trans);

		// XXX currentTime should equal Trans.getExecTime() so redundant?
		log(INFO, this + " "
				+ "Agent::updateTransactions: New transaction received: "
				+ trans);
		log(INFO, this + " Agent::logTransactions: "
				+ model.getName() + ": Current Position=" + positionBalance
				+ ", Realized Profit=" + realizedProfit);
	}

	/**
	 * Computes any unrealized profit based on market bid/ask quotes.
	 */
	// TODO Implement this
	public Price getUnrealizedProfit() {
		if (positionBalance == 0) return Price.ZERO;

		Price p = null;
		if (positionBalance > 0) {
			// For long position, compare cost to bid quote (buys)
//			for (Market market : model.getMarkets())
//				if (market.getQuote().getBidPrice().greaterThan(p))
//					p = market.getQuote().getBidPrice();
		} else {
			// For short position, compare cost to ask quote (sells)
//			for (Market market : model.getMarkets()) {
//				if (market.getQuote().getAskPrice().lessThan(p)) {
//					p = market.getQuote().getAskPrice();
//				}
//			}
		}

		log(DEBUG, model.getName() + ": " + this + " bal="
				+ positionBalance + ", p=" + p + ", avgCost=" + averageCost);

		return p == null ? Price.ZERO : new Price(positionBalance
				* (p.getInTicks() - averageCost));
	}

	public final TimeStamp getArrivalTime() {
		return arrivalTime;
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
	 * Iterates through the agent's transactions and calculates it's current discounted surplus with
	 * the specified discount factor
	 * 
	 * @param rho
	 *            Discount factor. 0 means no discounting, while larger positive numbers represent
	 *            larger discounting
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

			surplus += Math.exp(rho * timeToExecution.getInTicks())
					* transactionSurplus.getInTicks();
		}
		return surplus;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ model.hashCode();
	}

	@Override
	public String toString() {
		return new String("(" + id + ", " + model + ")");
	}
}
