package entity.agent;

import static logger.Logger.log;
import static logger.Logger.Level.INFO;

import java.util.Collection;

import com.google.common.collect.Lists;

import systemmanager.Keys;
import utils.Rands;
import activity.Activity;
import activity.SubmitNMSOrder;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * ZIRAGENT
 * 
 * A zero-intelligence agent with re-submission (ZIR).
 * 
 * The ZIR agent is primarily associated with a single market. It wakes up periodically to submit a
 * new bid (if its previous bid has transacted) or it does nothing.
 * 
 * This agent bases its private value on a stochastic process, the parameters of which are specified
 * at the beginning of the simulation by the spec file. The agent's private valuation is determined
 * by value of the random process at the time it enters, with some randomization added by using an
 * individual variance parameter. The private value is used to calculate the agent's surplus (and
 * thus the market's allocative efficiency).
 * 
 * This agent submits a single limit order at a time. It will modify its private value if its bid
 * has transacted by the time it wakes up.
 * 
 * NOTE: Each limit order price is uniformly distributed over a range that is twice the size of
 * bidRange in either a positive or negative direction from the agent's private value.
 * 
 * @author ewah
 */
public class ZIRAgent extends ReentryAgent {

	private static final long serialVersionUID = -1155740218390579581L;
	
	protected int bidRange; // range for limit order
	protected int maxAbsPosition; // max quantity for position

	public ZIRAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, Market market,
			Rands rand, int bidRange, int maxAbsPosition,
			double reentryRate, double pvVar, int tickSize) {
		super(arrivalTime, fundamental, sip, market, new PrivateValue(maxAbsPosition,
				pvVar, rand), rand, reentryRate, tickSize);
		this.bidRange = bidRange;
		this.maxAbsPosition = maxAbsPosition;
	}

	public ZIRAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, Market market,
			Rands rand, EntityProperties props) {
		this(arrivalTime, fundamental, sip, market, rand, props.getAsInt(Keys.BID_RANGE,
				5000), props.getAsInt(Keys.MAX_QUANTITY, 10),
				props.getAsDouble(Keys.REENTRY_RATE, 0.005), props.getAsDouble(
						Keys.PRIVATE_VALUE_VAR, 100000000), props.getAsInt(
						Keys.TICK_SIZE, 1));
	}

	@Override
	public Collection<Activity> agentStrategy(TimeStamp currentTime) {
		Collection<Activity> acts = Lists.newArrayList(super.agentStrategy(currentTime));

		StringBuilder sb = new StringBuilder().append(this).append(" ");
		sb.append(getName()).append(':');
		if (!currentTime.equals(arrivalTime)) {
			sb.append(" wake up.");
		}
		if (!activeOrders.isEmpty()) {
			sb.append(" last order has not transacted, go back to sleep");
			log(INFO, sb.toString());
			return acts;
		}

		// 0.50% chance of being either long or short
		int quantity = rand.nextBoolean() ? 1 : -1;

		int newPosition = quantity + positionBalance;
		// check that will not exceed max absolute position
		if (newPosition <= maxAbsPosition && newPosition >= -maxAbsPosition) {
			Price val = fundamental.getValueAt(currentTime).plus(
					privateValue.getValueFromQuantity(positionBalance, quantity)).nonnegative();
			Price price = new Price(
					(int) (val.getInTicks() - Math.signum(quantity)
							* rand.nextDouble() * 2 * bidRange)).nonnegative();

			sb.append(" position=").append(positionBalance).append(", for q=");
			sb.append(quantity).append(", value=");
			sb.append(fundamental.getValueAt(currentTime)).append(" + ");
			sb.append(privateValue.getValueFromQuantity(positionBalance,
					quantity));
			sb.append('=').append(val);
			log(INFO, sb.toString());

			acts.add(new SubmitNMSOrder(this, price, quantity,
					primaryMarket, TimeStamp.IMMEDIATE));
		} else {
			// if exceed max position, then don't submit a new bid
			// TODO - stay the same for now (position balance)
			sb.append("new order would exceed max position ");
			sb.append(maxAbsPosition).append("; no submission");
			log(INFO, sb.toString());
		}
		return acts;
	}

}
