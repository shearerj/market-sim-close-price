package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static fourheap.Order.OrderType.BUY;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import systemmanager.Keys;
import systemmanager.Simulation;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import data.FundamentalValue.FundamentalValueView;
import data.Props;
import data.Stats;
import entity.Entity;
import entity.View;
import entity.infoproc.BestBidAsk;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Transaction;
import event.Activity;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {

	protected static final Ordering<Price> pcomp = Ordering.natural();
	
	protected final Random rand;
	protected final FundamentalValueView fundamental;
	// List of all transactions for this agent. Implicitly time ordered due to 
	// transactions being created and assigned in time order.
	protected final Collection<OrderRecord> activeOrders;

	// Agent parameters
	protected final TimeStamp arrivalTime;
	protected final int tickSize;

	// Tracking position and profit
	protected int positionBalance;
	protected long profit;
	protected long liquidationProfit;

	protected Agent(Simulation sim, TimeStamp arrivalTime, Random rand, Props props) {
		super(sim.nextAgentId(), sim);
		this.arrivalTime = checkNotNull(arrivalTime);
		this.fundamental = sim.getFundamentalView(TimeStamp.of(props.getAsLong(Keys.FUNDAMENTAL_LATENCY)));
		this.tickSize = props.getAsInt(Keys.AGENT_TICK_SIZE, Keys.TICK_SIZE);
		this.rand = rand;

		this.activeOrders = Sets.newHashSet();
		this.positionBalance = 0;
		this.profit = 0;
		this.liquidationProfit = 0;
	}

	public abstract void agentStrategy();

	/**
	 * Liquidates an agent's position at the specified price.
	 */
	public void liquidateAtPrice(Price price) {

		log(INFO, "%s pre-liquidation: position=%d", this, positionBalance);

		liquidationProfit = positionBalance * price.intValue();
		profit += liquidationProfit;
		positionBalance = 0;

		log(INFO, "%s post-liquidation: liquidation profit=%d, profit=%d, price=%s", 
				this, liquidationProfit, profit, price);
		
		sim.postStat(Stats.TOTAL_PROFIT, profit);
	}
	
	/** Shortcut for creating orders */
	protected OrderRecord submitOrder(MarketView market, OrderType type, Price price, int quantity) {
		OrderRecord order = new OrderRecord(market, currentTime(), type, price, quantity);
		market.submitOrder(this, order);
		activeOrders.add(order);
		return order;
	}

	/** Shortcut for creating NMS orders */
	protected OrderRecord submitNMSOrder(MarketView market, OrderType type, Price price, int quantity) {
		OrderRecord order = new OrderRecord(market, currentTime(), type, price, quantity);
		market.submitNMSOrder(this, order);
		activeOrders.add(order);
		return order;
	}
	
	/** Wrapper for withdrawing orders. Necessary to keep bookkeeping consistent */
	protected void withdrawOrder(OrderRecord order) {
		order.getCurrentMarket().withdrawOrder(order);
		order.removeQuantity(order.getQuantity());
		activeOrders.remove(order);
	}
	
	/** Withdraw most recent order */
	protected void withdrawNewestOrder() {
		if (activeOrders.isEmpty())
			return;
		
		TimeStamp latestTime = TimeStamp.of(-1);
		OrderRecord newestOrder = null;
		for (OrderRecord order : activeOrders) {
			if (order.getCreatedTime().after(latestTime)) {
				latestTime = order.getCreatedTime();
				newestOrder = order;
			}
		}

		withdrawOrder(newestOrder);
	}
	
	/** Withdraw oldest (earliest) order */
	protected void withdrawOldestOrder() {
		if (activeOrders.isEmpty())
			return;
		
		TimeStamp earliestTime = TimeStamp.of(Long.MAX_VALUE);
		OrderRecord lastOrder = null;
		for (OrderRecord order : activeOrders) {
			if (order.getCreatedTime().before(earliestTime)) {
				earliestTime = order.getCreatedTime();
				lastOrder = order;
			}
		}

		withdrawOrder(lastOrder);
	}
	
	/** Withdraw all active orders */
	protected void withdrawAllOrders() {
		// Copy so that orders can be removed as they're withdrawn
		for (OrderRecord order : ImmutableList.copyOf(activeOrders))
			withdrawOrder(order);
	}
	
	/** Called when an AgetnView sees a transaction */
	protected void processTransaction(TimeStamp submitTime, OrderType type, Transaction trans) {
		int effQuantity = type == BUY ? trans.getQuantity() : -trans.getQuantity();
		positionBalance += effQuantity;
		profit -= effQuantity * trans.getPrice().intValue();
		postStat(Stats.NUM_TRANS + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.getClass().getSimpleName()), 1);
		postStat(Stats.NUM_TRANS_TOTAL, 1);
		
		log(INFO, "%s transacted to position %d", this, positionBalance);
	}
	
	/** Called by AgentView when its order transacted */
	protected void orderTransacted(OrderRecord order, int removedQuantity) {
		order.removeQuantity(removedQuantity);
		if (order.getQuantity() == 0)
			activeOrders.remove(order);
	}
	
	/** Called by AgentView when agent notified order was actually submitted */
	protected void orderSubmitted(OrderRecord order, MarketView market, TimeStamp submittedTime) {
		order.updateMarket(market);
		order.updateSubmitTime(submittedTime);
	}
	
	protected void reenterIn(TimeStamp delay) {
		sim.scheduleActivityIn(delay, new Activity() {
			@Override public void execute() { agentStrategy(); }
			@Override public String toString() { return "Reentry"; }
		});
	}

	public final TimeStamp getArrivalTime() {
		return arrivalTime;
	}
	
	protected BestBidAsk getNBBO() {
		return sim.getSIP().getNBBO();
	}
	
	/** Get payoff for player observation */
	public double getPayoff() {
		return profit;
	}
	
	/**
	 * @return list of player-specific features, can be overridden
	 */
	public Map<String, Double> getFeatures() {
		return ImmutableMap.of();
	}
	
	public long getLiquidationProfit() {
		return liquidationProfit;
	}
	
	public long getPostLiquidationProfit() {
		return profit;
	}
	
	@Override
	// Agents are defined by their existence, not their parameters
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	// Agents are defined by their existence, not their parameters
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	// Removes agent from the end of the name
	protected String name() {
		String oldName = super.name();
		return oldName.endsWith("Agent") ? oldName.substring(0, oldName.length() - 5) : oldName;
	}

	@Override
	public String toString() {
		return name() + "(" + id + ')';
	}
	
	public AgentView getView(TimeStamp latency) {
		return new AgentView(latency);
	}
	
	public class AgentView implements View {
		private final TimeStamp latency;
		
		protected AgentView(TimeStamp latency) {
			this.latency = latency;
		}
		
		public void orderSubmitted(final OrderRecord order, final MarketView market, final TimeStamp submittedTime) {
			sim.scheduleActivityIn(latency, new Activity() {
				@Override public void execute() {
					Agent.this.orderSubmitted(order, market, submittedTime);
				}
				@Override public String toString() { return "Order Submitted"; }
			});
		}
		
		public void orderTransacted(final OrderRecord order, final int removedQuantity) {
			sim.scheduleActivityIn(latency, new Activity() {
				@Override public void execute() {
					Agent.this.orderTransacted(order, removedQuantity);
				}
				@Override public String toString() { return "Order Transacted"; }
			});
		}
		
		public void processTransaction(final TimeStamp submitTime, final OrderType type, final Transaction transaction) {
			sim.scheduleActivityIn(latency, new Activity() {
				@Override public void execute() {
					Agent.this.processTransaction(submitTime, type, transaction);
				}
				@Override public String toString() { return "Process Transaction"; }
			});
		}
		
		@Override
		public TimeStamp getLatency() {
			return latency;
		}

		@Override
		public String toString() {
			return Agent.this.toString() + '*';
		}
		
	}

	private static final long serialVersionUID = 5363438238024144057L;

}
