package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Logger.logger;
import static logger.Logger.Level.INFO;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import systemmanager.Consts.DiscountFactor;
import systemmanager.Scheduler;
import systemmanager.Keys;
import utils.Rands;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
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
	
	public BackgroundAgent(Scheduler scheduler, TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, Iterator<TimeStamp> reentry,
			PrivateValue pv, int tickSize, int bidRangeMin,
			int bidRangeMax) {
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
		
		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(':');
		sb.append(" executing ZI strategy");
		
		int newPosition = (type.equals(BUY) ? 1 : -1) * quantity + positionBalance;
		if (newPosition <= privateValue.getMaxAbsPosition() &&
				newPosition >= -privateValue.getMaxAbsPosition()) {
			
			Price val = getValuation(type, currentTime);
			Price price = new Price((val.doubleValue() + (type.equals(SELL) ? 1 : -1) * 
					Rands.nextUniform(rand, bidRangeMin, bidRangeMax))).nonnegative().quantize(tickSize);
			
			sb.append(" position=").append(positionBalance).append(", for q=");
			sb.append(quantity).append(", value=");
			sb.append(fundamental.getValueAt(currentTime)).append(" + ");
			sb.append(privateValue.getValue(positionBalance, type));
			sb.append('=').append(val);
			log(INFO, sb.toString());
			
			scheduler.executeActivity(new SubmitNMSOrder(this, primaryMarket,
					type, price, quantity));
		} else {
			// if exceed max position, then don't submit a new bid
			sb.append("new order would exceed max position ");
			sb.append(privateValue.getMaxAbsPosition()).append("; no submission");
			log(INFO, sb.toString());
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
			submissionTime = trans.getBuyBid().getSubmitTime();
			type = OrderType.BUY;
		} else {
			submissionTime = trans.getSellBid().getSubmitTime();
			type = OrderType.SELL;
		}
		TimeStamp timeToExecution = trans.getExecTime().minus(submissionTime);

		int value = getTransactionValuation(type, trans.getQuantity(), 
				trans.getExecTime(), submissionTime).intValue();
		int cost = trans.getPrice().intValue() * trans.getQuantity();
		int transactionSurplus = (value - cost) * (type == OrderType.BUY ? 1 : -1) ;
		
		surplus.addValue(transactionSurplus, timeToExecution.getInTicks());
	}

	/**
	 * @return undiscounted surplus for player observation
	 */
	@Override
	public double getPayoff() {
		return surplus.getValueAtDiscount(DiscountFactor.NO_DISC);
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
		return new Price(
				fundamental.getValueAt(currentTime).intValue() * quantity
				+ privateValue.getValueFromQuantity(positionBalance, quantity, type).intValue()
				).nonnegative();
	}

	/**
	 * Returns same as getValuation except also sends info on private value,
	 * fundamental value to EventBus.
	 * 
	 * Note that this method has to subtract the transacted quantity from
	 * position balance (using the pre-transaction balance to determine the
	 * valuation).
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @param submissionTime
	 * @return
	 */
	protected Price getTransactionValuation(OrderType type, int quantity, 
			TimeStamp currentTime, TimeStamp submissionTime) {
		
		// Determine the pre-transaction balance
		int originalBalance = this.positionBalance + (type.equals(BUY) ? -1 : 1) * quantity;
		Price privateValue = this.privateValue.getValueFromQuantity(originalBalance, 
				quantity, type);
		Price fundamentalValue = fundamental.getValueAt(currentTime);
		
		return new Price(fundamentalValue.intValue() * quantity
				+ privateValue.intValue()).nonnegative();
	}
	
	/**
	 * @param type
	 * @param currentTime
	 * @param submissionTime
	 * @return
	 */
	protected Price getTransactionValuation(OrderType type, TimeStamp currentTime,
			TimeStamp submissionTime) {
		return getTransactionValuation(type, 1, currentTime, submissionTime);
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
