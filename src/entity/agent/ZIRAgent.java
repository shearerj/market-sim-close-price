package entity.agent;

import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import systemmanager.Keys;
import activity.Activity;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * ZIRAGENT
 * 
 * A zero-intelligence agent with re-submission (ZIR).
 *
 * The ZIR agent is primarily associated with a single market. It wakes up
 * periodically to submit a new bid.
 * 
 * This agent bases its private value on a stochastic process, the parameters
 * of which are specified at the beginning of the simulation by the spec file.
 * The agent's private valuation is determined by value of the random process at
 * the time it enters, with some randomization added by using an individual 
 * variance parameter. The private value is used to calculate the agent's surplus 
 * (and thus the market's allocative efficiency).
 *
 * This agent submits a single limit order at a time. It will modify its private
 * value if its bid has transacted by the time it wakes up.
 * 
 * NOTE: Each limit order price is uniformly distributed over a range that is twice
 * the size of bidRange (min, max) in either a positive or negative direction from 
 * the agent's private value.
 *
 * @author ewah
 */
public class ZIRAgent extends BackgroundAgent {

	private static final long serialVersionUID = -1155740218390579581L;

	public ZIRAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, 
			Market market, Random rand, double reentryRate, double pvVar,
			int tickSize, int maxAbsPosition, int bidRangeMin, int bidRangeMax) {
		super(arrivalTime, fundamental, sip, market, rand, reentryRate, 
				new PrivateValue(maxAbsPosition, pvVar, rand), tickSize,
				bidRangeMin, bidRangeMax);
	}

	public ZIRAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, EntityProperties props) {
		this(arrivalTime, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.005),
				props.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.MAX_QUANTITY, 10),
				props.getAsInt(Keys.BID_RANGE_MIN, 0),
				props.getAsInt(Keys.BID_RANGE_MAX, 5000));
	}

	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		Builder<Activity> acts = ImmutableList.<Activity> builder().addAll(
				super.agentStrategy(currentTime));

		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(':');
		if (!currentTime.equals(arrivalTime)) {
			sb.append(" wake up.");
		}
//		if (!activeOrders.isEmpty()) {
//			sb.append(" last order has not transacted, go back to sleep");
//			log(INFO, sb.toString());
//			return acts.build();
//		}

		// 0.50% chance of being either long or short
		int quantity = rand.nextBoolean() ? 1 : -1;
		acts.addAll(executeZIStrategy(quantity, currentTime));
		
//		int newPosition = quantity + positionBalance;
//		// check that will not exceed max absolute position
//		if (newPosition <= maxAbsPosition && newPosition >= -maxAbsPosition) {
//			Price val = fundamental.getValueAt(currentTime).plus(
//					privateValue.getValueFromQuantity(positionBalance, quantity)).nonnegative();
//			Price price = new Price(
//					(int) (val.getInTicks() - Math.signum(quantity)
//							* rand.nextDouble() * 2 * bidRange)).nonnegative();
//
//			sb.append(" position=").append(positionBalance).append(", for q=");
//			sb.append(quantity).append(", value=");
//			sb.append(fundamental.getValueAt(currentTime)).append(" + ");
//			sb.append(privateValue.getValueFromQuantity(positionBalance,
//					quantity));
//			sb.append('=').append(val);
//			log(INFO, sb.toString());
//
//			acts.add(new SubmitNMSOrder(this, price, quantity,
//					primaryMarket, TimeStamp.IMMEDIATE));
//		} else {
//			// if exceed max position, then don't submit a new bid
//			// TODO - stay the same for now (position balance)
//			sb.append("new order would exceed max position ");
//			sb.append(maxAbsPosition).append("; no submission");
//			log(INFO, sb.toString());
//		}
		return acts.build();
	}

}
