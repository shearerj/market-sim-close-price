package entity.agent;

import java.util.Collection;
import java.util.Random;

import com.google.common.collect.Lists;

import systemmanager.Keys;
import activity.Activity;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * ZIPAGENT
 * 
 * Zero-Intelligence Plus Agent.
 *
 * Based on Cliff & Bruten, "Zero is Not Enough: On the lower limit of agent
 * intelligence for continuous double auction markets," HP Laboratories technical report,
 * HPL-97-141, 1997.
 * 
 * @author ewah
 */
public class ZIPAgent extends BackgroundAgent {

	private static final long serialVersionUID = 8138883791556301413L;
	
	protected final double c_R, c_A, beta, betaVar, gamma;

	public ZIPAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, 
			Market market, Random rand, double reentryRate, double pvVar, 
			int tickSize, int maxAbsPosition, int bidRangeMin, int bidRangeMax, 
			double c_R, double c_A, double beta, double betaVar, double gamma) {
		super(arrivalTime, fundamental, sip, market, rand, reentryRate,
				new PrivateValue(maxAbsPosition, pvVar, rand), tickSize,
				bidRangeMin, bidRangeMax);
		this.c_R = c_R;
		this.c_A = c_A;
		this.beta = beta;
		this.betaVar = betaVar;
		this.gamma = gamma;
	}
	
	public ZIPAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, Market market,
			Random rand, EntityProperties props) {
		this(arrivalTime, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0.005), 
				props.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000),
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.MAX_QUANTITY, 10),
				props.getAsInt(Keys.BID_RANGE_MIN, 0),
				props.getAsInt(Keys.BID_RANGE_MAX, 5000), 
				props.getAsDouble(Keys.CR, .05),
				props.getAsDouble(Keys.CA, .05), 
				props.getAsDouble(Keys.BETA, .03), 
				props.getAsDouble(Keys.BETA_VAR, .005), 
				props.getAsDouble(Keys.BETA, .5));
	}
	
	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		Collection<? extends Activity> acts = Lists.newArrayList(super.agentStrategy(currentTime));
		
		// 0.50% chance of being either long or short
		int quantity = rand.nextBoolean() ? 1 : -1;
		@SuppressWarnings("unused")
		int val = Math.max(
				0,
				fundamental.getValueAt(currentTime).plus(
						privateValue.getValueFromQuantity(positionBalance,
								quantity)).getInTicks());

		// Insert events for the agent to sleep, then wake up again at timestamp
		// tsNew
		return acts;
	}
}
