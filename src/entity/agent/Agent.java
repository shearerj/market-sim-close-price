package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import systemmanager.Scheduler;
import activity.AgentStrategy;
import activity.WithdrawOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import data.FundamentalValue;
import entity.Entity;
import entity.infoproc.BestBidAsk;
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
	
	protected static final Ordering<Price> pcomp = Ordering.natural();
	
	protected final Random rand;
	protected final FundamentalValue fundamental;
	protected final SIP sip;
	// List of all transactions for this agent. Implicitly time ordered due to 
	// transactions being created and assigned in time order.
	protected final List<Transaction> transactions;
	protected final Collection<Order> activeOrders;

	// Agent parameters
	protected final TimeStamp arrivalTime;
	protected final int tickSize;

	// Tracking position and profit
	protected int positionBalance;
	protected long profit;
	protected long preLiquidationProfit;

	public Agent(Scheduler scheduler, TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Random rand, int tickSize) {
		super(nextID++, scheduler);
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

	public abstract void agentStrategy(
			TimeStamp currentTime);

	public void agentArrival(TimeStamp currentTime) {
		scheduler.executeActivity(new AgentStrategy(this));
	}

	/**
	 * Liquidate agent's position at the the value of the global fundamental at the specified time.
	 * Price is determined by the fundamental at the time of liquidation.
	 */
	public void liquidateAtFundamental(TimeStamp currentTime) {
		log.log(INFO, "%s liquidating...", this);
		liquidateAtPrice(fundamental.getValueAt(currentTime), currentTime);
	}

	/**
	 * Liquidates an agent's position at the specified price.
	 */
	public void liquidateAtPrice(Price price, TimeStamp ts) {

		log.log(INFO, "%s pre-liquidation: position=%d", this, positionBalance);

		preLiquidationProfit = profit;
		profit += positionBalance * price.intValue();

		log.log(INFO, "%s post-liquidation: profit=%d, price=%s", this, profit, price);
	}

	/**
	 * Adds an agent's order to its memory so it knows about it, and can cancel it
	 * @param order
	 */
	public void addOrder(Order order) {
		checkArgument(order.getAgent().equals(this),
				"Can't add order for a different agent");
		activeOrders.add(order);
	}
	
	/**
	 * Removes order when it's no longer active
	 * @param order
	 */
	public void removeOrder(Order order) {
		activeOrders.remove(order);
	}
	
	/**
	 * Withdraw most recent order (executes immediately).
	 * @param currentTime
	 * @return
	 */
	public void withdrawNewestOrder() {
		TimeStamp latestTime = TimeStamp.create(-1);
		Order lastOrder = null;
		for (Order order : activeOrders) {
			if (order.getSubmitTime().after(latestTime)) {
				latestTime = order.getSubmitTime();
				lastOrder = order;
			}
		}
		if (lastOrder != null) 
			scheduler.executeActivity(new WithdrawOrder(lastOrder));
	}
	
	/**
	 * Withdraw first (earliest) order (executes immediately).
	 * @param currentTime
	 * @return
	 */
	public void withdrawOldestOrder() {
		Order lastOrder = null;
		TimeStamp earliestTime = TimeStamp.create(Long.MAX_VALUE);
		for (Order order : activeOrders) {
			if (order.getSubmitTime().before(earliestTime)) {
				earliestTime = order.getSubmitTime();
				lastOrder = order;
			}
		}
		if (lastOrder != null) 
			scheduler.executeActivity(new WithdrawOrder(lastOrder));
	}
	
	/**
	 * Withdraw all active orders (executes immediately, not inserted as 
	 * activity).
	 * 
	 * TODO Is there a better way to withdraw all agent orders from a market,
	 * other than iterating through all of the agent's orders? This seems
	 * inefficient, but currently the OB does not facilitate pulling out all 
	 * orders from a single agent for removal.
	 * 
	 * @param currentTime
	 * @return
	 */
	public void withdrawAllOrders() {
		// activeOrders is copied, because these calls are happening instaniosuly and
		// hence modifying active orders
		for (Order order : ImmutableList.copyOf(activeOrders))
			scheduler.executeActivity(new WithdrawOrder(order));
	}
	
	
	/**
	 * Processes transaction and adds to internal data structure. Note that 
	 * agents should access transactions via the TransactionProcessor.
	 * @param trans
	 */
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

		log.log(INFO, "%s transacted to position %d", this, positionBalance);
	}

	public final TimeStamp getArrivalTime() {
		return arrivalTime;
	}
	
	public BestBidAsk getNBBO() {
		return sip.getNBBO();
	}
	
	/**
	 * @return payoff for player observation
	 */
	public double getPayoff() {
		return profit;
	}
	
	/**
	 * @return list of player-specific features, can be overridden
	 */
	public Map<String, Double> getFeatures() {
		return ImmutableMap.of();
	}
	
	public long getPreLiquidationProfit() {
		return preLiquidationProfit;
	}
	
	public long getPostLiquidationProfit() {
		return profit;
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
	protected String name() {
		String oldName = super.name();
		return oldName.substring(0, oldName.length() - 5); // Remove agent
	}

	@Override
	public String toString() {
		return name() + " (" + id + ')';
	}
	
}
