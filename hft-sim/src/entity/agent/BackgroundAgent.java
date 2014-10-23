package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Map;
import java.util.Random;

import systemmanager.Consts.DiscountFactor;
import systemmanager.Keys;
import systemmanager.Simulation;
import utils.Rands;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;

import data.Props;
import data.Stats;
import entity.agent.position.ListPrivateValue;
import entity.agent.position.PrivateValue;
import entity.market.Market;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;
import fourheap.Order.OrderType;

/**
 * Abstract class for background traders.
 * 
 * All background traders can submit ZI-strategy limit orders.
 * 
 * @author ewah
 */
public abstract class BackgroundAgent extends ReentryAgent {
	
	private static final long serialVersionUID = 7742389103679854398L;
	
	protected final PrivateValue privateValue;
	protected final DiscountedValue surplus;
	
	protected final int bidRangeMax; 		// range for limit order
	protected final int bidRangeMin;
	
	protected final boolean withdrawOrders;	// Withdraw orders each reentry
	
	// For ZIRP Strategy
	protected final int simulationLength;
	protected final double fundamentalKappa;
	protected final double fundamentalMean;
	protected final double acceptableProfitFraction;
	
	/**
	 * Constructor for custom private valuation 
	 */
	protected BackgroundAgent(Simulation sim, TimeStamp arrivalTime, Market market, PrivateValue privateValue, Random rand, Props props) {
		super(sim, arrivalTime, market, rand,
				AgentFactory.exponentials(props.getAsDouble(Keys.BACKGROUND_REENTRY_RATE, Keys.REENTRY_RATE), rand),
				props);
		this.privateValue = privateValue;
		this.bidRangeMin = props.getAsInt(Keys.BID_RANGE_MIN);
		this.bidRangeMax = props.getAsInt(Keys.BID_RANGE_MAX);
		this.withdrawOrders = props.getAsBoolean(Keys.WITHDRAW_ORDERS);
		
		this.simulationLength = props.getAsInt(Keys.SIMULATION_LENGTH);
		this.fundamentalKappa = props.getAsDouble(Keys.FUNDAMENTAL_KAPPA);
		this.fundamentalMean = props.getAsDouble(Keys.FUNDAMENTAL_MEAN);
		this.acceptableProfitFraction = props.getAsDouble(Keys.ACCEPTABLE_PROFIT_FRACTION);
		checkArgument(Range.closed(0d, 1d).contains(acceptableProfitFraction), "Acceptable profit fraction must be in [0, 1]: %f", acceptableProfitFraction);
		
		this.surplus = DiscountedValue.create();
		
		postStat(Stats.CONTROL_PRIVATE_VALUE, this.privateValue.getMean().doubleValue());
	}
	
	/**
	 * Default constructor with standard valuation model
	 */
	protected BackgroundAgent(Simulation sim, TimeStamp arrivalTime, Market market, Random rand, Props props) {
		this(sim, arrivalTime, market,
				ListPrivateValue.createRandomly(props.getAsInt(Keys.MAX_QUANTITY), props.getAsDouble(Keys.PRIVATE_VALUE_VAR), rand),
				rand, props);
	}
	
	@Override
	public void agentStrategy() {
		super.agentStrategy();
		
		if (withdrawOrders) {
			log(INFO, "%s Withdraw all orders.", this);
			withdrawAllOrders();
		}
	}

	/**
	 * Submits a NMS-routed Zero-Intelligence limit order.
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * 
	 * @return
	 */
	protected void executeZIStrategy(OrderType type, int quantity) {
		if (this.withinMaxPosition(type, quantity)) {
			
			Price val = getValuation(type);
			Price price = Price.of((val.doubleValue() + (type.equals(SELL) ? 1 : -1) * 
					Rands.nextUniform(rand, bidRangeMin, bidRangeMax))).nonnegative().quantize(tickSize);
			
			log(INFO, "%s executing ZI strategy position=%d, for q=%d, value=%s + %s=%s",
					this, positionBalance, quantity, fundamental.getValue(),
					privateValue.getValue(positionBalance, type), val);
			
			submitNMSOrder(type, price, quantity);
		}
	}
	
	/**
	 * Checks if new order, if submitted, would be within max position; returns
	 * true if would be within position limits.
	 *  
	 * @param type
	 * @param quantity
	 * @return
	 */
	public boolean withinMaxPosition(OrderType type, int quantity) {
		int newPosition = (type.equals(BUY) ? 1 : -1) * quantity + positionBalance;
		if (Math.abs(newPosition) > privateValue.getMaxAbsPosition()) {
			// if exceed max position, then don't submit a new bid
			log(INFO, "%s submitting new order would exceed max position %d ; no submission",
					this, privateValue.getMaxAbsPosition());
			return false;
		}
		return true;
	}
	
	protected final void executeZIRPStrategy(OrderType type, int quantity) {
		int newPosition = quantity + positionBalance;
		if (type.equals(SELL)) newPosition *= -1;
		
		if (newPosition <= privateValue.getMaxAbsPosition() 
			&& newPosition >= -privateValue.getMaxAbsPosition()) {
			
			Price val = getEstimatedValuation(type);
			Price price = Price.of((val.doubleValue() + (type.equals(SELL) ? 1 : -1) 
					* Rands.nextUniform(rand, bidRangeMin, bidRangeMax
					))).nonnegative().quantize(tickSize);

			final Price rHat = this.getEstimatedFundamental(type);  
			
			log(INFO, "%s executing ZIRP strategy position=%d, for q=%d, fund=%s value=%s + %s=%s stepsLeft=%s pv=%s",
				this, positionBalance, quantity, fundamental.getValue().intValue(),
				rHat, privateValue.getValue(positionBalance, type),	val, 
				simulationLength - currentTime().getInTicks(), 
				privateValue.getValueFromQuantity(positionBalance, quantity, type));
			
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
						price = Price.of(val.doubleValue() / quantity).quantize(tickSize).nonnegative();
						
						log(INFO, "%s executing ZIRP strategy GREEDY SELL, markup=%s, bid=%s, bidMarkup=%s, price=%s",
							this, shading, bidPrice, bidMarkup, price.intValue());
					} else {
						log(INFO, "%s no g.s. opportunity, markup=%s, bidMarkup=%s, desiredFrac=%s, threshold=%s",
							this, shading, bidMarkup, acceptableProfitFraction, 
							(shading * acceptableProfitFraction)
						); 
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
						price = Price.of(val.doubleValue() / quantity).quantize(tickSize).nonnegative();
						
						log(INFO, "%s executing ZIRP strategy GREEDY BUY, markup=%s, ask=%s, askMarkup=%s, price=%s",
								this, shading, askPrice,	askMarkup, price.intValue());
					} else {
						log(INFO, "%s no g.b. opportunity, markup=%s, askMarkup=%s, desiredFrac=%s, threshold=%s",
							this, shading, askMarkup, acceptableProfitFraction,
							shading * acceptableProfitFraction); 
					}
				}
			}
			
			submitNMSOrder(type, price, quantity);
		} else {
			// if exceed max position, then don't submit a new bid
			log(INFO, "%s executing ZIRP strategy new order would exceed max position %d ; no submission",
					this, privateValue.getMaxAbsPosition());
		}
	}
	
	
	
	@Override
	protected void processTransaction(TimeStamp submitTime, OrderType type, Transaction trans) {
		super.processTransaction(submitTime, type, trans);
		
		TimeStamp timeToExecution = trans.getExecTime().minus(submitTime);
		for (int i = 0; i < trans.getQuantity(); ++i)
			postStat(Stats.EXECUTION_TIME, timeToExecution.getInTicks());

		int privateValue = getTransactionValuation(type, trans.getQuantity(), 
				trans.getExecTime()).intValue();
		int cost = trans.getPrice().intValue() * trans.getQuantity();
		int transactionSurplus = (privateValue - cost) * (type.equals(BUY) ? 1 : -1) ;
		
		surplus.addValue(transactionSurplus, timeToExecution.getInTicks());
	}

	/**
	 * @return undiscounted surplus for player observation
	 */
	@Override
	public double getPayoff() {
		return this.getLiquidationProfit() + surplus.getValueAtDiscount(DiscountFactor.NO_DISC);
	}
	
	/**
	 * @return private-value control variables for player features
	 */
	@Override
	public Map<String, Double> getFeatures() {
		ImmutableMap.Builder<String, Double> features = ImmutableMap.builder();
		features.putAll(super.getFeatures());
		
		Price buyPV = privateValue.getValue(0, BUY);
		Price sellPV = privateValue.getValue(0, SELL);
		
		features.put(Keys.PV_POSITION1_MAX_ABS, Math.max(Math.abs(buyPV.doubleValue()), 
				Math.abs(sellPV.doubleValue())));
		features.put(Keys.PV_BUY1, buyPV.doubleValue());
		features.put(Keys.PV_SELL1, sellPV.doubleValue());
		
		return features.build();
	}
	
	/**
	 * @param discount
	 * @return
	 */
	public double getDiscountedSurplus(DiscountFactor discount) {
		return surplus.getValueAtDiscount(discount);
	}

	
	/**
	 * For control variates
	 * @return
	 */
	public Price getPrivateValueMean() {
		return privateValue.getMean();
	}
	
	/**
	 * Returns the limit price (i.e. valuation) for the agent for buying/selling 1 unit.
	 * 
	 * valuation = fundamental + private_value
	 * 
	 * @param type
	 * @param currentTime
	 * @return
	 */
	protected Price getValuation(OrderType type) {
		return getValuation(type, 1);
	}
	
	protected Price getEstimatedValuation(OrderType type) {
		return getEstimatedValuation(type, 1);
	}
	
	protected Price getEstimatedValuation(OrderType type, int quantity) {
		
		Price rHat = this.getEstimatedFundamental(type); 
			
		return Price.of(rHat.intValue() * quantity
				+ privateValue.getValueFromQuantity(positionBalance, quantity, type).intValue()
				).nonnegative();
	}
	
	protected Price getEstimatedFundamental(OrderType type) {
		// FIXME If delayed fundamental, this will be innacurate. Should account for latency...
		final int stepsLeft = (int) (simulationLength - currentTime().getInTicks());
		final double kappaCompToPower = Math.pow(1 - fundamentalKappa, stepsLeft);
		return Price.of(fundamental.getValue().intValue() * kappaCompToPower 
			+ fundamentalMean * (1 - kappaCompToPower));
	}
	
	/**
	 * Returns valuation = fundamental + private value (value of cumulative
	 * gain if over quantity > 1).
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	protected Price getValuation(OrderType type, int quantity) {
		return Price.of(fundamental.getValue().intValue() * quantity
				+ privateValue.getValueFromQuantity(positionBalance, quantity, type).intValue()
				).nonnegative();
	}

	/**
	 * Returns only the private value for trading (assuming agents all liquidate
	 * at the end).
	 * 
	 * Note that this method has to subtract the transacted quantity from
	 * position balance (using the pre-transaction balance to determine the
	 * valuation).
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	// FIXME This may not work as it was intended
	// FIXME Time should probably be removed, and current time should be referenced.
	protected Price getTransactionValuation(OrderType type, int quantity,
			TimeStamp currentTime) {
		
		// Determine the pre-transaction balance
		int originalBalance = this.positionBalance + (type.equals(BUY) ? -1 : 1) * quantity;
		return this.privateValue.getValueFromQuantity(originalBalance, 
				quantity, type);
		// FIXME Not sure about how to resolve this conflic, so it's commented
//		Price fundamentalValue = fundamental.getValue();
//		return Price.of(fundamentalValue.intValue() * quantity
//				+ privateValue.intValue()).nonnegative();
	}
	
	/**
	 * @param type
	 * @param currentTime
	 * @return
	 */
	protected Price getTransactionValuation(OrderType type,
			TimeStamp currentTime) {
		return getTransactionValuation(type, 1, currentTime);
	}
	
//	protected Price getEstimatedLimitPrice(
//		final OrderType type, 
//		final TimeStamp currentTime,
//		final int simulationLength,
//		final double fundamentalKappa,
//		final double fundamentalMean
//	) {
//		return getEstimatedLimitPrice(
//			type, 1, currentTime, simulationLength, fundamentalKappa, fundamentalMean
//		);
//	}
//	
//	protected Price getEstimatedLimitPrice(
//		final OrderType type, 
//		final int quantity, 
//		final TimeStamp currentTime,
//		final int simulationLength,
//		final double fundamentalKappa,
//		final double fundamentalMean
//	) {
//		return Price.of(
//			getEstimatedValuation(
//				type, 
//				quantity, 
//				currentTime, 
//				simulationLength, 
//				fundamentalKappa, 
//				fundamentalMean
//			).doubleValue() / quantity
//		).nonnegative();
//	}
	
	/**
	 * Returns the limit price for a new order of quantity 1.
	 * 
	 * @param type
	 * @param currentTime
	 * @return
	 */
	protected Price getLimitPrice(OrderType type) {
		return getLimitPrice(type, 1);
	}
	
	/**
	 * Returns the limit price for the agent given potential quantity for which
	 * the agent plans to submit an order.
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	protected Price getLimitPrice(OrderType type, int quantity) {
		return Price.of(getValuation(type, quantity).doubleValue() 
				/ quantity).nonnegative();
	}

	@Override
	public void liquidateAtPrice(Price price) {
		super.liquidateAtPrice(price);
		
		sim.postStat(Stats.CLASS_PROFIT + "background", profit);
	}
}
