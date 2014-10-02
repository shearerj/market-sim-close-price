package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.log;
import static logger.Log.Level.INFO;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import systemmanager.Consts.DiscountFactor;
import systemmanager.Keys;
import systemmanager.Scheduler;
import utils.Rands;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableMap;

import data.FundamentalValue;
import entity.infoproc.SIP;
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
	
	protected int bidRangeMax; 		// range for limit order
	protected int bidRangeMin;
	
	public BackgroundAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			Iterator<TimeStamp> reentry, PrivateValue pv, int tickSize,
			int bidRangeMin, int bidRangeMax) {
		
		super(scheduler, arrivalTime, fundamental, sip, market, rand, reentry, tickSize);
	
		this.privateValue = checkNotNull(pv);
		this.surplus = DiscountedValue.create();
		this.bidRangeMin = bidRangeMin;
		this.bidRangeMax = bidRangeMax;
	}
	
	/**
	 * Submits a NMS-routed Zero-Intelligence limit order.
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * 
	 * @return
	 */
	public void executeZIStrategy(OrderType type, int quantity, TimeStamp currentTime) {
		if (this.withinMaxPosition(type, quantity)) {
			
			Price val = getValuation(type, currentTime);
			Price price = new Price((val.doubleValue() + (type.equals(SELL) ? 1 : -1) * 
					Rands.nextUniform(rand, bidRangeMin, bidRangeMax))).nonnegative().quantize(tickSize);
			
			log.log(INFO, "%s executing ZI strategy position=%d, for q=%d, value=%s + %s=%s",
					this, positionBalance, quantity, fundamental.getValueAt(currentTime),
					privateValue.getValue(positionBalance, type), val);
			
			scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket,
					type, price, quantity));
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
			log.log(INFO, "%s submitting new order would exceed max position %d ; no submission",
					this, privateValue.getMaxAbsPosition());
			return false;
		}
		return true;
	}
	
	public final void executeZIRPStrategy(
		final OrderType type, 
		final int quantity, 
		final TimeStamp currentTime,
		final int simulationLength,
		final double fundamentalKappa,
		final double fundamentalMean,
		final double acceptableProfitFraction
	) {		
		int newPosition = quantity + positionBalance;
		if (type == SELL) {
			newPosition *= -1;
		}
		if (
			newPosition <= privateValue.getMaxAbsPosition() 
			&& newPosition >= -privateValue.getMaxAbsPosition()
		) {
			Price val = getEstimatedValuation(type, currentTime, 
				simulationLength, fundamentalKappa, fundamentalMean);
			Price price = 
				new Price(
					(val.doubleValue() + (type.equals(SELL) ? 1 : -1) 
					* Rands.nextUniform(
						rand, bidRangeMin, bidRangeMax
					))).nonnegative().quantize(tickSize);
			
			final int stepsLeft = (int) (simulationLength - currentTime.getInTicks());
			final double kappaCompToPower = Math.pow(1 - fundamentalKappa, stepsLeft);
			final double rHat = 
				fundamental.getValueAt(currentTime).intValue() * kappaCompToPower 
				+ fundamentalMean * (1 - kappaCompToPower);
			
			final int myPrivateValue = 
				privateValue.getValueFromQuantity(positionBalance, quantity, type).intValue();
			
			log.log(
				INFO, 
				"%s executing ZIRP strategy position=%d, " 
					+ "for q=%d, fund=%s value=%s + %s=%s stepsLeft=%s pv=%s",
				this, 
				positionBalance, 
				quantity, 
				fundamental.getValueAt(currentTime).intValue(),
				rHat,
				privateValue.getValue(positionBalance, type), 
				val,
				stepsLeft,
				myPrivateValue
			);
			
			Quote quote = marketQuoteProcessor.getQuote();
			if (
				quote != null 
				&& quote.getBidPrice() != null 
				&& quote.getAskPrice() != null
			) {
				if (type == SELL) {
					// how much you'd profit from selling at the above price
					final int markup = price.intValue() - val.intValue();
					final int bidPrice = quote.getBidPrice().intValue();
					// how much you'd profit from selling all units at the bid price
					final int bidMarkup = bidPrice * quantity - val.intValue();
					// if you would make acceptableProfitFraction of your
					// markup at the bid
					if (markup * acceptableProfitFraction <= bidMarkup) {
						price = getEstimatedLimitPrice(
							type, quantity, currentTime, simulationLength, 
							fundamentalKappa, fundamentalMean
						);
						
						log.log(
							INFO,
				"%s executing ZIRP strategy GREEDY SELL, markup=%s, bid=%s, bidMarkup=%s, price=%s",
							this,
							markup,
							bidPrice,
							bidMarkup,
							price.intValue()
						);
					} else {
						log.log(
							INFO, 
				"%s no g.s. opportunity, markup=%s, bidMarkup=%s, desiredFrac=%s, threshold=%s",
							this,
							markup,
							bidMarkup,
							acceptableProfitFraction,
							(markup * acceptableProfitFraction)
						); 
					}
				} else {
					// how much you'd profit from buying at the above price
					final int markup = val.intValue() - price.intValue();
					final int askPrice = quote.getAskPrice().intValue();
					// how much you'd profit from buying all units at the ask price
					final int askMarkup = val.intValue() - askPrice * quantity;
					// if you would make acceptableProfitFraction of your 
					// markup at the ask
					if (markup * acceptableProfitFraction <= askMarkup) {
						price = getEstimatedLimitPrice(
							type, quantity, currentTime, simulationLength, 
							fundamentalKappa, fundamentalMean
						);
						
						log.log(
								INFO,
					"%s executing ZIRP strategy GREEDY BUY, markup=%s, ask=%s, askMarkup=%s, price=%s",
								this,
								markup,
								askPrice,
								askMarkup,
								price.intValue()
							);
					} else {
						log.log(INFO, 
				"%s no g.b. opportunity, markup=%s, askMarkup=%s, desiredFrac=%s, threshold=%s",
							this,
							markup,
							askMarkup,
							acceptableProfitFraction,
							markup * acceptableProfitFraction
						); 
					}
				}
			}
			
			scheduler.executeActivity(
				new SubmitNMSOrder(
					this, 
					primaryMarket,
					type, 
					price, 
					quantity
			));
		} else {
			// if exceed max position, then don't submit a new bid
			log.log(
				INFO, 
				"%s executing ZIRP strategy new order " 
					+ "would exceed max position %d ; no submission",
				this, 
				privateValue.getMaxAbsPosition()
			);
		}
	}
	
	@Override
	public void processTransaction(Transaction trans) {
		
		super.processTransaction(trans);
		TimeStamp submissionTime;
		OrderType type;
		
		if (trans.getBuyer().equals(trans.getSeller())) {
			// FIXME Handle buyer = seller appropriately... Maybe this is appropriate?
			return;
		} else if (trans.getBuyer().equals(this)) {
			submissionTime = trans.getBuyOrder().getSubmitTime();
			type = OrderType.BUY;
		} else {
			submissionTime = trans.getSellOrder().getSubmitTime();
			type = OrderType.SELL;
		}
		TimeStamp timeToExecution = trans.getExecTime().minus(submissionTime);

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
	protected Price getValuation(OrderType type, TimeStamp currentTime) {
		return getValuation(type, 1, currentTime);
	}
	
	protected Price getEstimatedValuation(
		final OrderType type, 
		final TimeStamp currentTime,
		final int simulationLength,
		final double fundamentalKappa,
		final double fundamentalMean
	) {
		return getEstimatedValuation(type, 1, currentTime, 
			simulationLength, fundamentalKappa, fundamentalMean);
	}
	
	protected Price getEstimatedValuation(
		OrderType type, 
		int quantity, 
		TimeStamp currentTime,
		final int simulationLength,
		final double fundamentalKappa,
		final double fundamentalMean
	) {
		final int stepsLeft = (int) (simulationLength - currentTime.getInTicks());
		final double kappaCompToPower = Math.pow(1 - fundamentalKappa, stepsLeft);
		final double rHat = 
			fundamental.getValueAt(currentTime).intValue() * kappaCompToPower 
			+ fundamentalMean * (1 - kappaCompToPower);
		return new Price(((int) rHat) * quantity
				+ privateValue.getValueFromQuantity(positionBalance, quantity, type).intValue()
				).nonnegative();
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
	protected Price getValuation(OrderType type, int quantity, TimeStamp currentTime) {
		return new Price(fundamental.getValueAt(currentTime).intValue() * quantity
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
	protected Price getTransactionValuation(OrderType type, int quantity,
			TimeStamp currentTime) {
		
		// Determine the pre-transaction balance
		int originalBalance = this.positionBalance + (type.equals(BUY) ? -1 : 1) * quantity;
		return this.privateValue.getValueFromQuantity(originalBalance, 
				quantity, type);
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
	
	protected Price getEstimatedLimitPrice(
		final OrderType type, 
		final TimeStamp currentTime,
		final int simulationLength,
		final double fundamentalKappa,
		final double fundamentalMean
	) {
		return getEstimatedLimitPrice(
			type, 1, currentTime, simulationLength, fundamentalKappa, fundamentalMean
		);
	}
	
	protected Price getEstimatedLimitPrice(
		final OrderType type, 
		final int quantity, 
		final TimeStamp currentTime,
		final int simulationLength,
		final double fundamentalKappa,
		final double fundamentalMean
	) {
		return new Price(
			getEstimatedValuation(
				type, 
				quantity, 
				currentTime, 
				simulationLength, 
				fundamentalKappa, 
				fundamentalMean
			).doubleValue() / quantity
		).nonnegative();
	}
	
	/**
	 * Returns the limit price for a new order of quantity 1.
	 * 
	 * @param type
	 * @param currentTime
	 * @return
	 */
	protected Price getLimitPrice(OrderType type, TimeStamp currentTime) {
		return getLimitPrice(type, 1, currentTime);
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
	protected Price getLimitPrice(OrderType type, int quantity, TimeStamp currentTime) {
		return new Price(getValuation(type, quantity, currentTime).doubleValue() 
				/ quantity).nonnegative();
	}
}
