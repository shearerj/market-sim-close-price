package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import logger.Log;
import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.MaxPosition;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.Withdraw;
import utils.Rand;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import data.FundamentalValue;
import data.Observations;
import data.Props;
import data.Stats;
import entity.agent.position.ListPrivateValue;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import entity.sip.MarketInfo;
import event.TimeStamp;
import event.Timeline;
import fourheap.Order.OrderType;

/**
 * Abstract class for background traders.
 * 
 * All background traders can submit ZI-strategy limit orders.
 * 
 * @author ewah
 */
public abstract class BackgroundAgent extends SMAgent {
	
	private final int maxAbsolutePosition;
	private final boolean withdrawOrders;	// Withdraw orders each reentry
	private final Map<String, Double> features;
	
	/**
	 * Constructor for custom private valuation 
	 */
	protected BackgroundAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Iterator<TimeStamp> arrivalIntervals, ListPrivateValue privateValue, Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, privateValue, arrivalIntervals, market, props);
		this.maxAbsolutePosition = privateValue.getMaxAbsPosition();
		this.withdrawOrders = props.get(Withdraw.class);
				
		// For controlled variates
		postStat(Stats.CONTROL_PRIVATE_VALUE, getPrivateValueMean().doubleValue());
		
		Price buyPV = getPrivateValue(BUY);
		Price sellPV = getPrivateValue(SELL);
		features = ImmutableMap.of(
				Observations.PV_BUY1, buyPV.doubleValue(),
				Observations.PV_SELL1, sellPV.doubleValue(),
				Observations.PV_POSITION1_MAX_ABS, Math.max(Math.abs(buyPV.doubleValue()), Math.abs(sellPV.doubleValue())));
	}
	
	/**
	 * Default constructor with standard valuation model
	 */
	protected BackgroundAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		this(id, stats, timeline, log, rand, sip, fundamental,
				Agent.exponentials(props.get(BackgroundReentryRate.class, ReentryRate.class), rand),
				ListPrivateValue.createRandomly(props.get(MaxPosition.class), props.get(PrivateValueVar.class), rand),
				market, props);
	}
	
	@Override
	protected void agentStrategy() {
		super.agentStrategy();
		if (withdrawOrders) {
			log(INFO, "%s Withdraw all orders.", this);
			withdrawAllOrders();
		}
	}
	
	/** Enforces Background agent invariants. They can not exceed their max position. */
	@Override
	protected boolean submitOrder(OrderRecord order, boolean nmsRoutable) {
		checkArgument(order.getCurrentMarket() == primaryMarket, "Must submit to primary market");
		if (mightExceedMaxPosition(order)) {
			log(INFO, "%s submitting new order (%s) might exceed max position %d ; no submission",
					this, order, getMaxAbsPosition());
			return false;
		}
		return super.submitOrder(order, nmsRoutable);
	}
	
	/** Returns if submitting an order might result in a sequence of events that would make the agent exceed its max position */
	private boolean mightExceedMaxPosition(OrderRecord order) {
		int absolutePosition = getPosition() * order.getOrderType().sign() + order.getQuantity();
		for (OrderRecord o : getActiveOrders())
			if (o.getOrderType() == order.getOrderType())
				absolutePosition += o.getQuantity();
		return absolutePosition > getMaxAbsPosition();
	}
		
	@Override
	protected void processTransaction(TimeStamp submitTime, OrderType type, Transaction trans) {
		super.processTransaction(submitTime, type, trans);
		
		TimeStamp timeToExecution = trans.getExecTime().minus(submitTime);
		postStat(Stats.EXECUTION_TIME, timeToExecution.getInTicks(), trans.getQuantity());
	}

	/**
	 * @return undiscounted surplus for player observation
	 */
	@Override
	public double getPayoff() {
		return Iterables.getFirst(getDiscountedSurplus(), null).getValue();
	}
	
	/**
	 * @return private-value control variables for player features
	 */
	@Override
	public Map<String, Double> getFeatures() {
		return ImmutableMap.<String, Double> builder()
				.putAll(super.getFeatures())
				.putAll(features)
				.build();
	}

	protected final int getMaxAbsPosition() {
		return maxAbsolutePosition;
	}

	@Override
	public void liquidateAtPrice(Price price) {
		super.liquidateAtPrice(price);
		
		postStat(Stats.PROFIT + "background", getProfit());
		
		for (Entry<Double, Double> e : getDiscountedSurplus())
			postStat(String.format("%s%.4f_background", Stats.SURPLUS, e.getKey()), e.getValue());
	}

	private static final long serialVersionUID = 7742389103679854398L;
}
