package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Map;
import java.util.Map.Entry;

import logger.Log;
import systemmanager.Keys.AcceptableProfitThreshold;
import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.RMax;
import systemmanager.Keys.RMin;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalLatency;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.Withdraw;
import utils.Rand;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

import data.FundamentalValue;
import data.Observations;
import data.Props;
import data.Stats;
import entity.agent.position.ListPrivateValue;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
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
public abstract class BackgroundAgent extends ReentryAgent {
	
	private final int maxAbsolutePosition;
	protected final int bidRangeMax; 		// range for limit order
	protected final int bidRangeMin;
	
	protected final boolean withdrawOrders;	// Withdraw orders each reentry
	
	private final Map<String, Double> features;
	
	// For ZIRP Strategy
	protected final int simulationLength;
	protected final long fundamentalLatency;
	protected final double fundamentalKappa;
	protected final double fundamentalMean;
	protected final double acceptableProfitFraction;
	
	/**
	 * Constructor for custom private valuation 
	 */
	protected BackgroundAgent(int id, Stats stats, Timeline timeline, Log log, Rand rand, MarketInfo sip, FundamentalValue fundamental,
			ListPrivateValue privateValue, Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, privateValue,
				TimeStamp.of((long) rand.nextExponential(props.get(ArrivalRate.class))),
				market,
				ReentryAgent.exponentials(props.get(BackgroundReentryRate.class, ReentryRate.class), rand),
				props);
		this.maxAbsolutePosition = privateValue.getMaxAbsPosition();
		this.bidRangeMin = props.get(RMin.class);
		this.bidRangeMax = props.get(RMax.class);
		this.withdrawOrders = props.get(Withdraw.class);
		
		this.simulationLength = props.get(SimLength.class);
		this.fundamentalLatency = props.get(FundamentalLatency.class).getInTicks();
		this.fundamentalKappa = props.get(FundamentalKappa.class);
		this.fundamentalMean = props.get(FundamentalMean.class);
		
		this.acceptableProfitFraction = props.get(AcceptableProfitThreshold.class);
		checkArgument(Range.closed(0d, 1d).contains(acceptableProfitFraction), "Acceptable profit fraction must be in [0, 1]: %f", acceptableProfitFraction);
		
		// For controlled variates
		postStat(Stats.CONTROL_PRIVATE_VALUE, getPrivateValueMean().doubleValue());
		
		Price buyPV = getValuation(BUY);
		Price sellPV = getValuation(SELL);
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
				ListPrivateValue.createRandomly(props.get(MaxQty.class), props.get(PrivateValueVar.class), rand),
				market, props);
	}
	
	@Override
	protected void agentStrategy() {
		if (withdrawOrders) {
			log(INFO, "%s Withdraw all orders.", this);
			withdrawAllOrders();
		}
		super.agentStrategy();
	}
	
	/** Enforces Background agent invariants. They can not exceed their max position. */
	@Override
	protected boolean submitOrder(OrderRecord order, boolean nmsRoutable) {
		checkArgument(order.getCurrentMarket() == primaryMarket, "Must submit to primary market");
		if (mightExceedMaxPosition(order))
			return false;
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
	
	/**
	 * Checks if new order, if submitted, would be within max position; returns
	 * true if would be within position limits.
	 */
	protected boolean withinMaxPosition(OrderType type, int quantity) {
		int newPosition = (type.equals(BUY) ? 1 : -1) * quantity + getPosition();
		if (Math.abs(newPosition) > getMaxAbsPosition()) {
			// if exceed max position, then don't submit a new bid
			log(INFO, "%s submitting new order would exceed max position %d ; no submission",
					this, getMaxAbsPosition());
			return false;
		}
		return true;
	}
	
	protected final void executeZIRPStrategy(OrderType type, int quantity) {
		int newPosition = quantity + getPosition();
		if (type.equals(SELL)) newPosition *= -1;
		
		if (Range.closed(-getMaxAbsPosition(), getMaxAbsPosition()).contains(newPosition)) {
			Price val = getEstimatedValuation(type);
			Price price = Price.of((val.doubleValue() + (type.equals(SELL) ? 1 : -1) 
					* rand.nextUniform(bidRangeMin, bidRangeMax
					)));

			Price rHat = getEstimatedFundamental();  
			
			log(INFO, "%s at position=%d, for %s %d units, r_t=%s & %s steps left, value=rHat+pv=%s+%s= %s",
					this, getPosition(), type, quantity, getFundamental().intValue(),
					simulationLength - getCurrentTime().getInTicks(),
					rHat, getPrivateValue(type), 
					val);
			
			Quote quote = getQuote();
			if (quote.isDefined()) {
				if (type.equals(SELL)) {
					// estimated surplus from selling at the above price
					final int shading = price.intValue() - val.intValue();
					final int bidPrice = quote.getBidPrice().get().intValue();
					
					// estimated surplus from selling all units at the bid price
					final int bidMarkup = bidPrice * quantity - val.intValue();
					
					// if you would make at least acceptableProfitFraction of your
					// markup at the bid, submit order at estimated fundamental
					if (shading * acceptableProfitFraction <= bidMarkup) {
						price = Price.of(val.doubleValue() / quantity);
						
						log(INFO, "%s executing ZIRP strategy GREEDY SELL @ %s, shading %s * desiredFrac %s <= bidMarkup=%s at BID=%s",
							this, price, shading, acceptableProfitFraction, bidMarkup, bidPrice);
						postStat(Stats.ZIRP_GREEDY, 1);
					} else {
						log(INFO, "%s executing ZIRP strategy no greedy sell, shading %s * desiredFrac %s = %s > bidMarkup of %s",
							this, shading, acceptableProfitFraction, 
							(shading * acceptableProfitFraction), bidMarkup);
						postStat(Stats.ZIRP_GREEDY, 0);
					}
				} else {
					// estimated surplus from buying at the above price
					final int shading = val.intValue() - price.intValue();
					final int askPrice = quote.getAskPrice().get().intValue();
					
					// estimated surplus from buying all units at the ask price
					final int askMarkup = val.intValue() - askPrice * quantity;
					
					// if you would make at least acceptableProfitFraction of your 
					// markup at the ask, submit order at estimated fundamental
					if (shading * acceptableProfitFraction <= askMarkup) {
						price = Price.of(val.doubleValue() / quantity);
						log(INFO, "%s executing ZIRP strategy GREEDY BUY @ %s, shading %s * desiredFrac %s <= askMarkup=%s at ASK=%s",
								this, price, shading, acceptableProfitFraction, askMarkup, askPrice);
						postStat(Stats.ZIRP_GREEDY, 1);
					} else {
						log(INFO, "%s executing ZIRP strategy no greedy sell, shading %s * desiredFrac %s = %s > askMarkup of %s",
							this, shading, acceptableProfitFraction, 
							(shading * acceptableProfitFraction), askMarkup); 
						postStat(Stats.ZIRP_GREEDY, 0);
					}
				}
			}
			
			submitNMSOrder(type, price, quantity);
		} else {
			// if exceed max position, then don't submit a new bid
			log(INFO, "%s executing ZIRP strategy new order would exceed max position %d ; no submission",
					this, getMaxAbsPosition());
		}
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
	
	/**
	 * Returns the limit price (i.e. valuation) for the agent for buying/selling 1 unit.
	 * 
	 * valuation = fundamental + private_value
	 */
	protected final Price getValuation(OrderType type) {
		return getValuation(type, 1);
	}
	
	/**
	 * Returns valuation = fundamental + private value (value of cumulative
	 * gain if over quantity > 1).
	 */
	protected final Price getValuation(OrderType type, int quantity) {
		return Price.of(getFundamental().intValue() * quantity + getPrivateValue(quantity, type).intValue()).nonnegative();
	}

	protected final Price getEstimatedValuation(OrderType type) {
		return getEstimatedValuation(type, 1);
	}
	
	protected final Price getEstimatedValuation(OrderType type, int quantity) {
		Price rHat = getEstimatedFundamental();
		return Price.of(rHat.intValue() * quantity + getPrivateValue(quantity, type).intValue()).nonnegative();
	}
	
	protected Price getEstimatedFundamental() {
		final int stepsLeft = (int) (simulationLength - getCurrentTime().getInTicks() + fundamentalLatency);
		final double kappaCompToPower = Math.pow(1 - fundamentalKappa, stepsLeft);
		return Price.of(getFundamental().intValue() * kappaCompToPower 
			+ fundamentalMean * (1 - kappaCompToPower));
	}
	
	/** Returns the limit price for a new order of quantity 1. */
	public final Price getLimitPrice(OrderType type) {
		return getLimitPrice(type, 1);
	}
	
	/**
	 * Returns the limit price for the agent given potential quantity for which
	 * the agent plans to submit an order.
	 */
	public final Price getLimitPrice(OrderType type, int quantity) {
		return Price.of(getValuation(type, quantity).doubleValue() 
				/ quantity);
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
