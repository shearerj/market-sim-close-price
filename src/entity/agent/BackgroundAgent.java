package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static logger.Logger.log;
import static logger.Logger.Level.INFO;
import iterators.ExpInterarrivals;

import java.util.Iterator;
import java.util.Random;

import systemmanager.Consts.OrderType;
import utils.Rands;
import activity.Activity;
import activity.AgentStrategy;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * Abstract class for background traders.
 * 
 * All background traders can submit ZI-strategy limit orders.
 * 
 * @author ewah
 */
public abstract class BackgroundAgent extends SMAgent {
	
	private static final long serialVersionUID = 7742389103679854398L;
	
	protected final PrivateValue privateValue;
	protected final DiscountedValue surplus;
	protected Iterator<TimeStamp> reentry; // re-entry times
	
	protected int bidRangeMax; 		// range for limit order
	protected int bidRangeMin;
	
	public BackgroundAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, Iterator<TimeStamp> reentry,
			PrivateValue pv, int tickSize, int bidRangeMin,
			int bidRangeMax) {
		super(arrivalTime, fundamental, sip, market, rand, tickSize);
	
		this.privateValue = checkNotNull(pv);
		this.surplus = DiscountedValue.create();
		this.reentry = checkNotNull(reentry);
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
	}
	
	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		TimeStamp nextStrategy = currentTime.plus(reentry.next());
		return ImmutableList.of(new AgentStrategy(this, nextStrategy));
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
		
		int newPosition = (type.equals(OrderType.BUY) ? 1 : -1) * quantity + positionBalance;
		if (newPosition <= privateValue.getMaxAbsPosition() &&
				newPosition >= -privateValue.getMaxAbsPosition()) {
			
			Price val = getValuation(type, currentTime);
			Price price = new Price((val.doubleValue() + (type.equals(OrderType.SELL) ? 1 : -1) * 
					Rands.nextUniform(rand, bidRangeMin, bidRangeMax))).nonnegative().quantize(tickSize);
			
			sb.append(" position=").append(positionBalance).append(", for q=");
			sb.append(quantity).append(", value=");
			sb.append(fundamental.getValueAt(currentTime)).append(" + ");
			sb.append(privateValue.getValue(positionBalance, type));
			sb.append('=').append(val);
			log(INFO, sb.toString());
			
			return ImmutableList.of(new SubmitNMSOrder(this, type, price,
					quantity, primaryMarket, TimeStamp.IMMEDIATE));
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
	// TODO Returns undiscounted surplus. To get otherwise would require
	// modifying this, or allowing discounted profit for all agents...
	public double getPayoff() {
		return surplus.getValueAtDiscount(0);
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
	 * Returns the limit price (i.e. valuation) for the agent.
	 * 
	 * valuation = fundamental + private_value
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
}
