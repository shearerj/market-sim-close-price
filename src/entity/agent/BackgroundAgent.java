package entity.agent;

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
	
	protected Iterator<TimeStamp> reentry; // re-entry times
	
	protected int bidRangeMax; 		// range for limit order
	protected int bidRangeMin;
	
	public BackgroundAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, Iterator<TimeStamp> reentry,
			PrivateValue pv, int tickSize, int bidRangeMin,
			int bidRangeMax) {
		super(arrivalTime, fundamental, sip, market, rand, pv, tickSize);
	
		this.reentry = reentry;
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
			sb.append(privateValue.getValueFromQuantity(positionBalance, type));
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
	
	/**
	 * Returns the limit price (i.e. valuation) for the agent.
	 * 
	 * valuation = fundamental + private_value
	 * 
	 * @param type
	 * @param currentTime
	 * @return
	 */
	public Price getValuation(OrderType type, TimeStamp currentTime) {
		return new Price(
				fundamental.getValueAt(currentTime).intValue()
						+ privateValue.getValueFromQuantity(positionBalance,
								type).intValue()).nonnegative();
	}
}
