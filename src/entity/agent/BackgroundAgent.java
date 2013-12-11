package entity.agent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import iterators.ExpInterarrivals;
import static fourheap.Order.OrderType.*;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Consts.DiscountFactor;
import utils.Rands;
import activity.Activity;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableList;

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
	
	public BackgroundAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, Iterator<TimeStamp> reentry,
			PrivateValue pv, int tickSize, int bidRangeMin,
			int bidRangeMax) {
		super(arrivalTime, fundamental, sip, market, rand, reentry, tickSize);
	
		this.privateValue = checkNotNull(pv);
		this.surplus = DiscountedValue.create();
		this.bidRangeMin = bidRangeMin;
		this.bidRangeMax = bidRangeMax;
	}

	/**
	 * Shortcut constructor for exponential interarrivals (e.g. Poisson reentries)
	 */
	public BackgroundAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, double reentryRate, PrivateValue pv,
			int tickSize, int bidRangeMin, int bidRangeMax) {
		this(arrivalTime, fundamental, sip, market, rand, new ExpInterarrivals(reentryRate, rand),
				pv, tickSize, bidRangeMin, bidRangeMax);
		checkArgument(reentryRate >= 0, "Agent reentry rate must be positive!");
	}
	
	/**
	 * Submits a NMS-routed Zero-Intelligence limit order.
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * 
	 * @return
	 */
	public Iterable<? extends Activity> executeZIStrategy(OrderType type, int quantity, TimeStamp currentTime) {
		
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
			
			return ImmutableList.of(new SubmitNMSOrder(this, primaryMarket, type,
					price, quantity, TimeStamp.IMMEDIATE));
		} else {
			// if exceed max position, then don't submit a new bid
			sb.append("new order would exceed max position ");
			sb.append(privateValue.getMaxAbsPosition()).append("; no submission");
			log(INFO, sb.toString());
			
			return ImmutableList.of();
		}
	}
	
	@Override
	public void processTransaction(Transaction trans) {
		super.processTransaction(trans);
		TimeStamp submissionTime;
		OrderType type;
		
		if (trans.getBuyer().equals(trans.getSeller())) {
			// FIXME Handle appropriately... Maybe this is appropriate?
			return;
		} else if (trans.getBuyer().equals(this)) {
			submissionTime = trans.getBuyBid().getSubmitTime();
			type = OrderType.BUY;
		} else {
			submissionTime = trans.getSellBid().getSubmitTime();
			type = OrderType.SELL;
		}
		TimeStamp timeToExecution = trans.getExecTime().minus(submissionTime);

		int value = getValuation(type, trans.getQuantity(), trans.getExecTime()).intValue();
		int cost = trans.getPrice().intValue() * trans.getQuantity();
		int transactionSurplus = (value - cost) * (type == OrderType.BUY ? 1 : -1) ;
		
		surplus.addValue(transactionSurplus, timeToExecution.getInTicks());
	}

	@Override
	public double getPayoff() {
		return surplus.getValueAtDiscount(DiscountFactor.NO_DISC);
	}
	
	public double getDiscountedSurplus(DiscountFactor discount) {
		return surplus.getValueAtDiscount(discount);
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
	public Price getValuation(OrderType type, TimeStamp currentTime) {
		return getValuation(type, 1, currentTime);
	}
	
	/**
	 * Returns valuation = fundamental + private_value
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	public Price getValuation(OrderType type, int quantity, TimeStamp currentTime) {
		return new Price(
				fundamental.getValueAt(currentTime).intValue() * quantity
				+ privateValue.getValueFromQuantity(positionBalance, quantity, type).intValue()
				).nonnegative();
	}

	/**
	 * Returns the limit price for the agent (valuation for 1 unit).
	 * 
	 * @param type
	 * @param quantity
	 * @param currentTime
	 * @return
	 */
	public Price getLimitPrice(OrderType type, int quantity, TimeStamp currentTime) {
		return new Price(getValuation(type, quantity, currentTime).doubleValue() 
				/ quantity).nonnegative();
	}
}
