package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import data.FundamentalValue;
import activity.Activity;
import activity.AgentStrategy;
import activity.WithdrawOrder;
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
	public static int nextID = 1;
	
	protected final Random rand;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	// List of all transactions for this agent. Implicitly time ordered due to transactions
	// being created and assigned in time order.
	protected final List<Transaction> transactions;
	protected final Collection<Order> activeOrders;

	// Agent parameters
	protected final TimeStamp arrivalTime;
	protected final int tickSize;

	// Tracking position and profit
	protected int positionBalance;
	protected long profit;
	protected long preLiquidationProfit;

	public Agent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Random rand, int tickSize) {
		super(nextID++);
		this.fundamental = checkNotNull(fundamental);
		this.arrivalTime = checkNotNull(arrivalTime);
		this.sip = sip;
		this.tickSize = tickSize;
		this.rand = rand;

		this.transactions = Lists.newArrayList();
		this.activeOrders = Sets.newHashSet();
		this.positionBalance = 0;
		this.profit = 0;
		this.preLiquidationProfit = 0;
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
		return liquidateAtPrice(fundamental.getValueAt(currentTime), currentTime);
	}

	/**
	 * Liquidates an agent's position at the specified price.
	 */
	public Iterable<? extends Activity> liquidateAtPrice(Price price, TimeStamp ts) {

		log(INFO, this + " pre-liquidation: position="
				+ positionBalance);

		preLiquidationProfit = profit;
		profit += positionBalance * price.intValue();

		log(INFO, this + " post-liquidation: profit=" + profit
				+ ", price=" + price);
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

	/**
	 * Withdraw most recent order.
	 * @return
	 */
	public Iterable<? extends Activity> withdrawNewestOrder() {
		Collection<Activity> acts = new ArrayList<Activity>();
		TimeStamp ts = TimeStamp.ZERO;
		Order lastOrder = null;
		for (Order order : activeOrders) {
			if (order.getSubmitTime().after(ts)) {
				ts = order.getSubmitTime();
				lastOrder = order;
			}
		}
		if (lastOrder != null) acts.add(new WithdrawOrder(lastOrder, TimeStamp.IMMEDIATE));
		return acts;
	}
	
	/**
	 * Withdraw first (earliest) order.
	 * @return
	 */
	public Iterable<? extends Activity> withdrawOldestOrder() {
		Collection<Activity> acts = new ArrayList<Activity>();
		TimeStamp ts = new TimeStamp(Long.MAX_VALUE); // infinity
		Order lastOrder = null;
		for (Order order : activeOrders) {
			if (order.getSubmitTime().before(ts)) {
				ts = order.getSubmitTime();
				lastOrder = order;
			}
		}
		if (lastOrder != null) acts.add(new WithdrawOrder(lastOrder, TimeStamp.IMMEDIATE));
		return acts;
	}
	
	/**
	 * Withdraw all active orders.
	 * @return
	 */
	public Iterable<? extends Activity> withdrawAllOrders() {
		Collection<Activity> acts = new ArrayList<Activity>();
		for (Order order : activeOrders)
			acts.add(new WithdrawOrder(order, TimeStamp.IMMEDIATE));
		return acts;
	}
	
	// TODO gives the agent instant data. Should instead process with delay?
	public void processTransaction(Transaction trans) {
		checkArgument(trans.getBuyer().equals(this) || trans.getSeller().equals(this),
				"Can only add a transaction that this agent participated in");
		// Add to transactions data structure
		transactions.add(trans);
		
		// Not an else if in case buyer and seller are the same
		if (trans.getBuyer().equals(this)) {
			positionBalance += trans.getQuantity();
			profit -= trans.getQuantity() * trans.getPrice().intValue();
		}
		if (trans.getSeller().equals(this)) {
			positionBalance -= trans.getQuantity();
			profit += trans.getQuantity() * trans.getPrice().intValue();
		}

		log(INFO, this + " transacted to position " + positionBalance);
	}

	public final TimeStamp getArrivalTime() {
		return arrivalTime;
	}
	
	public double getPayoff() {
		return preLiquidationProfit;
	}
	
	public long getPreLiquidationProfit() {
		return profit;
	}
	
	public long getPostLiquidationProfit() {
		return preLiquidationProfit;
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
