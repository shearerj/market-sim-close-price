package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static fourheap.Order.OrderType.BUY;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import systemmanager.Keys.AgentTickSize;
import systemmanager.Keys.DiscountFactors;
import systemmanager.Keys.FundamentalLatency;
import systemmanager.Keys.TickSize;
import systemmanager.Simulation;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import data.FundamentalValue.FundamentalValueView;
import data.Props;
import data.Stats;
import entity.Entity;
import entity.View;
import entity.agent.position.PrivateValue;
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
	
	protected final Random rand;
	protected final FundamentalValueView fundamental;
	protected final Collection<OrderRecord> activeOrders;

	// Agent parameters
	protected final int tickSize;

	// Tracking position and profit
	private int positionBalance;
	private long profit;
	private final PrivateValue privateValue;
	private final DiscountedValue privateValueSurplus;

	protected Agent(Simulation sim, PrivateValue privateValue, TimeStamp arrivalTime, Random rand, Props props) {
		super(sim.nextAgentId(), sim);
		this.fundamental = sim.getFundamentalView(props.get(FundamentalLatency.class));
		this.tickSize = props.get(AgentTickSize.class, TickSize.class);
		this.rand = rand;

		this.activeOrders = Sets.newHashSet();
		
		this.positionBalance = 0;
		this.profit = 0;
		this.privateValue = checkNotNull(privateValue);
		this.privateValueSurplus = DiscountedValue.create(props.get(DiscountFactors.class));
		
		// Schedule first entry
		reenterIn(checkNotNull(arrivalTime));
	}

	protected abstract void agentStrategy();

	/** Liquidates an agent's position at the specified price. */
	public void liquidateAtPrice(Price price) {

		log(INFO, "%s pre-liquidation: position=%d", this, positionBalance);

		int liquidationProfit = positionBalance * price.intValue();
		profit += liquidationProfit;
		positionBalance = 0;

		log(INFO, "%s post-liquidation: liquidation profit=%d, profit=%d, price=%s", 
				this, liquidationProfit, profit, price);
		
		postStat(Stats.TOTAL_PROFIT, profit);
		for (Entry<Double, Double> e : getDiscountedSurplus()) {
			postStat(Stats.SURPLUS + e.getKey(), e.getValue());
			postStat(Stats.SURPLUS + e.getKey() + '_' + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getClass().getSimpleName()), e.getValue());
		}
	}
	
	protected Iterable<Entry<Double, Double>> getDiscountedSurplus() {
		return Iterables.transform(privateValueSurplus.getValues(), new Function<Entry<Double, Double>, Entry<Double, Double>>() {
			public Entry<Double, Double> apply(Entry<Double, Double> e) {
				return Maps.immutableEntry(e.getKey(), e.getValue() + Agent.this.getProfit());
			}
		});
	}

	// TODO Check for possible position violation?
	/** Shortcut for creating orders */
	protected OrderRecord submitOrder(MarketView market, OrderType type, Price price, int quantity) {
		OrderRecord order = new OrderRecord(market, currentTime(), type, price, quantity);
		market.submitOrder(this, order);
		activeOrders.add(order);
		return order;
	}

	// TODO Check for possible position violation?
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
		privateValueSurplus.addValue(privateValue.getValue(positionBalance, trans.getQuantity(), type),
				trans.getExecTime().getInTicks());
		
		int effQuantity = type == BUY ? trans.getQuantity() : -trans.getQuantity();
		positionBalance += effQuantity;
		profit -= effQuantity * trans.getPrice().intValue();
		
		postStat(Stats.NUM_TRANS + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, this.getClass().getSimpleName()), 1);
		postStat(Stats.NUM_TRANS_TOTAL, 1);
		log(INFO, "%s transacted to position %d", this, positionBalance);
	}
	
	/** Called by AgentView when its order transacted */
	protected void orderTransacted(OrderRecord order, int transactedQuantity) {
		order.removeQuantity(transactedQuantity);
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
			@Override public String toString() { return "Strategy"; }
		});
	}
	
	protected BestBidAsk getNBBO() {
		return sim.getSIP().getNBBO();
	}
	
	protected int getPosition() {
		return positionBalance;
	}
	
	protected int getMaxAbsPosition() {
		return privateValue.getMaxAbsPosition();
	}
	
	protected Price getValuation(OrderType type) {
		return privateValue.getValue(getPosition(), type);
	}
	
	protected Price getValuation(int quantity, OrderType type) {
		return privateValue.getValue(getPosition(), quantity, type);
	}
	
	protected Price getPrivateValueMean() {
		return privateValue.getMean();
	}
	
	protected long getProfit() {
		return profit;
	}
	
	/** Get payoff for player observation */
	public double getPayoff() {
		return getProfit();
	}
	
	/**
	 * @return list of player-specific features, can be overridden
	 */
	public Map<String, Double> getFeatures() {
		return ImmutableMap.of();
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
