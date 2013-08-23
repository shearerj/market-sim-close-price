package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import data.FundamentalValue;

import activity.Activity;
import activity.AgentStrategy;
import entity.Entity;
import entity.infoproc.SIP;
import entity.market.Order;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {

	private static final long serialVersionUID = 5363438238024144057L;
	private static int nextID = 1;
	
	protected final Random rand;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	// List of all transactions. Implicitly time ordered due to transactions
	// being created and assigned in time order.
	protected final List<Transaction> transactions;
	protected final Collection<Order> activeOrders;

	// Agent parameters
	protected final PrivateValue privateValue;
	protected final TimeStamp arrivalTime;
	protected final int tickSize;

	protected int positionBalance;
	// Tracking cash flow
	protected int cashBalance;
	protected int averageCost;
	// for liquidation XXX Currently unused
	protected int realizedProfit;
	protected int preLiqPosition;
	protected int preLiqRealizedProfit;

	public Agent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			PrivateValue privateValue, Random rand, int tickSize) {
		super(nextID++);
		this.fundamental = checkNotNull(fundamental);
		this.rand = rand;
		this.arrivalTime = checkNotNull(arrivalTime);
		this.sip = sip;
		this.privateValue = checkNotNull(privateValue);
		this.tickSize = tickSize;

		this.transactions = Lists.newArrayList();
		this.activeOrders = Sets.newHashSet();
	}

	public abstract Iterable<? extends Activity> agentStrategy(
			TimeStamp currentTime);

	public Iterable<? extends Activity> agentArrival(TimeStamp currentTime) {
		return Collections.singleton(new AgentStrategy(this, TimeStamp.IMMEDIATE));
	}

	/**
	 * Liquidate agent's position at the the value of the global fundamental at the specified time.
	 * Price is determined by the fundamental at the time of liquidation.
	 */
	
	public Iterable<? extends Activity> liquidateAtFundamental(
			TimeStamp currentTime) {
		log(INFO, this + " liquidating...");
		return liquidate(fundamental.getValueAt(currentTime), currentTime);
	}

	/**
	 * Liquidates an agent's position at the specified price.
	 */
	// TODO Unused
	public Iterable<? extends Activity> liquidate(Price price, TimeStamp ts) {

		log(INFO, this + " pre-liquidation: position="
				+ positionBalance);

		// If no net position, no need to liquidate
		if (positionBalance == 0) return Collections.emptyList();

		preLiqPosition = positionBalance;
		preLiqRealizedProfit = (int) getSurplus(0); // XXX This is almost certainly wrong
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
		checkArgument(order.getAgent().equals(this),
				"Can't add order for a different agent");
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
		checkArgument(trans.getBuyer().equals(this) || trans.getSeller().equals(this),
				"Can only add a transaction that this agent participated in");
		transactions.add(trans);
		// Not an else if in case buyer and seller are the same
		if (trans.getBuyer().equals(this))
			positionBalance += trans.getQuantity();
		if (trans.getSeller().equals(this))
			positionBalance -= trans.getQuantity();

		log(INFO, this + " transacted to position " + positionBalance);
	}

	/**
	 * Computes any unrealized profit based on market bid/ask quotes.
	 */
	// TODO Implement this
//	public Price getUnrealizedProfit() {
//		throw new IllegalArgumentException();
//		if (positionBalance == 0) return Price.ZERO;
//
//		Price p = null;
//		if (positionBalance > 0) {
//			// For long position, compare cost to bid quote (buys)
//			for (Market market : model.getMarkets())
//				if (market.getQuote().getBidPrice().greaterThan(p))
//					p = market.getQuote().getBidPrice();
//		} else {
//			// For short position, compare cost to ask quote (sells)
//			for (Market market : model.getMarkets()) {
//				if (market.getQuote().getAskPrice().lessThan(p)) {
//					p = market.getQuote().getAskPrice();
//				}
//			}
//		}
//
//		log(DEBUG, model.getName() + ": " + this + " bal="
//				+ positionBalance + ", p=" + p + ", avgCost=" + averageCost);
//
//		return p == null ? Price.ZERO : new Price(positionBalance
//				* (p.getInTicks() - averageCost));
//	}

	public final TimeStamp getArrivalTime() {
		return arrivalTime;
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
		checkArgument(rho >= 0, "Can't have a negative discoutn factor");
		double surplus = 0;

		for (Transaction trans : transactions) {
			TimeStamp submissionTime;
			int sign;
			
			if (trans.getBuyer().equals(trans.getSeller())) {
				// FIXME Handle appropriately...
				continue;
			} else if (trans.getBuyer().equals(this)) {
				submissionTime = trans.getBuyBid().getSubmitTime();
				sign = 1;
			} else {
				submissionTime = trans.getSellBid().getSubmitTime();
				sign = -1;
			}
			TimeStamp timeToExecution = trans.getExecTime().minus(submissionTime);

			Price fund = fundamental.getValueAt(trans.getExecTime()).times(
					trans.getQuantity());
			Price pv = privateValue.getValueFromQuantity(positionBalance,
					trans.getQuantity());
			Price cost = trans.getPrice().times(trans.getQuantity());
			Price transactionSurplus = fund.plus(pv).minus(cost).times(
					sign);

			surplus += Math.exp(rho * timeToExecution.getInTicks())
					* transactionSurplus.getInTicks();
		}
		return surplus;
	}
	
	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "(" + id + ")";
	}
}
