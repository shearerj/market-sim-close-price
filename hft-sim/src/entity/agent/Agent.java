package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static logger.Log.Level.INFO;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import logger.Log;
import systemmanager.Keys.DiscountFactors;
import systemmanager.Keys.FundamentalLatency;
import systemmanager.Keys.TickSize;
import utils.Rand;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import data.FundamentalValue;
import data.FundamentalValue.FundamentalValueView;
import data.Props;
import data.Stats;
import entity.Entity;
import entity.View;
import entity.agent.position.PrivateValue;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Transaction;
import entity.sip.BestBidAsk;
import entity.sip.MarketInfo;
import event.Activity;
import event.InformationActivity;
import event.TimeStamp;
import event.Timeline;
import fourheap.Order.OrderType;

/**
 * Base class for all agents.
 * 
 * @author ewah
 */
public abstract class Agent extends Entity {
	
	private final MarketInfo sip;
	private final FundamentalValueView fundamental;
	private final Collection<OrderRecord> activeOrders;

	// Agent parameters
	private final int tickSize;

	// Tracking position and profit
	private int positionBalance;
	private long profit;
	private final PrivateValue privateValue;
	private final DiscountedValue privateValueSurplus;
	private boolean liquidated;

	protected Agent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			PrivateValue privateValue, TimeStamp arrivalTime, Props props) {
		super(id, stats, timeline, log, rand);
		this.fundamental = fundamental.getView(props.get(FundamentalLatency.class));
		this.tickSize = props.get(TickSize.class);
		this.sip = sip;

		this.activeOrders = Sets.newHashSet();
		
		this.positionBalance = 0;
		this.profit = 0;
		this.privateValue = checkNotNull(privateValue);
		this.privateValueSurplus = DiscountedValue.create(props.get(DiscountFactors.class));
		this.liquidated = false;
		
		// Schedule first entry
		reenterIn(checkNotNull(arrivalTime));
	}

	protected abstract void agentStrategy();

	/** Liquidates an agent's position at the specified price. */
	public void liquidateAtPrice(Price price) {
		checkState(!liquidated, "Can't liquidate if agent has already liquidated");
		liquidated = true;

		log(INFO, "%s pre-liquidation: position=%d", this, positionBalance);

		int liquidationProfit = positionBalance * price.intValue();
		profit += liquidationProfit;
		positionBalance = 0;

		log(INFO, "%s post-liquidation: liquidation profit=%d, profit=%d, price=%s", 
				this, liquidationProfit, profit, price);
		
		postStat(Stats.TOTAL_PROFIT, profit);
		for (Entry<Double, Double> e : getDiscountedSurplus()) {			
			postStat(String.format("%s%.4f", Stats.SURPLUS, e.getKey()), e.getValue());
			postStat(String.format("%s%.4f_%s", Stats.SURPLUS, e.getKey(), getClass().getSimpleName().toLowerCase()), e.getValue());
		}
	}
	
	protected Iterable<Entry<Double, Double>> getDiscountedSurplus() {
		return Iterables.transform(privateValueSurplus.getValues(), new Function<Entry<Double, Double>, Entry<Double, Double>>() {
			public Entry<Double, Double> apply(Entry<Double, Double> e) {
				return Maps.immutableEntry(e.getKey(), e.getValue() + Agent.this.getProfit());
			}
		});
	}

	/**
	 * Ultimate method that all agents order should go through. This should be
	 * overridden to enforce that certain things happen when an agent submits an
	 * order. Can be used to enforce certain things about how an agent submits
	 * orders. A return of false indicates that the order was not actually
	 * submitted and will not appear in active orders.
	 */
	protected boolean submitOrder(OrderRecord order, boolean nmsRoutable) {
		checkArgument(order.getPrice().intValue() % tickSize == 0, "Price not in a valid tick size for this agent");
		activeOrders.add(order);
		if (nmsRoutable)
			order.getCurrentMarket().submitNMSOrder(this, order);
		else
			order.getCurrentMarket().submitOrder(this, order);
		return true;
	}
	
	protected final boolean submitOrder(OrderRecord order) {
		return submitOrder(order, false);
	}
	
	protected final boolean submitNMSOrder(OrderRecord order) {
		return submitOrder(order, true);
	}
	
	/** Shortcut for creating orders. If the order wasn's submitted then null is returned */
	protected final OrderRecord submitOrder(MarketView market, OrderType type, Price price, int quantity) {
		OrderRecord order = new OrderRecord(market, getCurrentTime(), type, price.nonnegative().quantize(tickSize), quantity);
		boolean submitted = submitOrder(order, false);
		return submitted ? order : null;
	}

	/** Shortcut for creating NMS orders. If the order wasn's submitted then null is returned */
	protected final OrderRecord submitNMSOrder(MarketView market, OrderType type, Price price, int quantity) {
		OrderRecord order = OrderRecord.create(market, getCurrentTime(), type, price.nonnegative().quantize(tickSize), quantity);
		boolean submitted = submitOrder(order, true);
		return submitted ? order : null;
	}
	
	/** Wrapper for withdrawing orders. Necessary to keep bookkeeping consistent */
	protected void withdrawOrder(OrderRecord order) {
		order.getCurrentMarket().withdrawOrder(order);
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
		privateValueSurplus.addValue(type.sign() * privateValue.getValue(positionBalance, trans.getQuantity(), type).doubleValue(),
				trans.getExecTime().getInTicks());
		
		int effQuantity = type.sign() * trans.getQuantity();
		positionBalance += effQuantity;
		profit -= effQuantity * trans.getPrice().intValue();
		
		postStat(Stats.NUM_TRANS + getClass().getSimpleName().toLowerCase(), 1);
		postStat(Stats.NUM_TRANS_TOTAL, 1);
		log(INFO, "%s transacted to position %d", this, positionBalance);
	}
	
	/** Called by AgentView when its order transacted */
	protected void orderRemoved(OrderRecord order, int transactedQuantity) {
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
		scheduleActivityIn(delay, new Activity() {
			@Override public void execute() { agentStrategy(); }
			@Override public String toString() { return "Strategy"; }
		});
	}
	
	protected final Collection<OrderRecord> getActiveOrders() {
		return Collections.unmodifiableCollection(activeOrders);
	}
	
	protected final int getTickSize() {
		return tickSize;
	}
	
	protected final BestBidAsk getNBBO() {
		return sip.getNBBO();
	}
	
	protected final Price getFundamental() {
		return fundamental.getValue();
	}
	
	protected final FundamentalValueView getFundamentalValueView() {
		return fundamental;
	}
	
	protected final int getPosition() {
		return positionBalance;
	}
	
	public final Price getPrivateValue(OrderType type) {
		return getPrivateValue(1, type);
	}
	
	public final Price getPrivateValue(int quantity, OrderType type) {
		return privateValue.getValue(getPosition(), quantity, type);
	}
	
	protected final Price getHypotheticalPrivateValue(int position, int quantity, OrderType type) {
		return privateValue.getValue(position, quantity, type);
	}
	
	protected final Price getPrivateValueMean() {
		return privateValue.getMean();
	}
	
	protected final long getProfit() {
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
	
	// Agents are defined by their existence, not their parameters
	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	// Agents are defined by their existence, not their parameters
	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	// Removes agent from the end of the name
	@Override
	protected String name() {
		String oldName = super.name();
		return oldName.endsWith("Agent") ? oldName.substring(0, oldName.length() - 5) : oldName;
	}

	@Override
	public String toString() {
		return name() + "(" + getID() + ')';
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
			scheduleActivityIn(latency, new Activity() {
				@Override public void execute() {
					Agent.this.orderSubmitted(order, market, submittedTime);
				}
				@Override public String toString() { return "Order Submitted"; }
			});
		}
		
		public void orderRemoved(final OrderRecord order, final int removedQuantity) {
			scheduleActivityIn(latency, new Activity() {
				@Override public void execute() {
					Agent.this.orderRemoved(order, removedQuantity);
				}
				@Override public String toString() { return "Order Removed"; }
			});
		}
		
		public void processTransaction(final TimeStamp submitTime, final OrderType type, final Transaction transaction) {
			scheduleActivityIn(latency, new InformationActivity() {
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
