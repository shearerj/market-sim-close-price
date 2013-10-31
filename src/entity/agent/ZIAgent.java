package entity.agent;

import java.util.Random;

import systemmanager.Keys;
import activity.Activity;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * ZIAGENT
 * 
 * A zero-intelligence (ZI) agent.
 * 
 * This agent bases its private value on a stochastic process, the parameters of which are specified
 * at the beginning of the simulation by the spec file. The agent's private valuation is determined
 * by value of the random process at the time it enters, with some randomization added by using an
 * individual variance parameter. The private value is used to calculate the agent's surplus (and
 * thus the market's allocative efficiency).
 * 
 * This agent submits only ONE limit order during its lifetime.
 * 
 * NOTE: The limit order price is uniformly distributed over a range that is twice the size of
 * bidRange in either a positive or negative direction from the agent's private value.
 * 
 * @author ewah
 */
public class ZIAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1148707664467962927L;

	public ZIAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, double reentryRate, double pvVar,
			int tickSize, int bidRangeMin, int bidRangeMax) {
		super(arrivalTime, fundamental, sip, market, rand, reentryRate, 
				new PrivateValue(1, pvVar, rand), tickSize, 
				bidRangeMin, bidRangeMax);
	}

	public ZIAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, EntityProperties props) {
		this(arrivalTime, fundamental, sip, market, rand,
				props.getAsDouble(Keys.REENTRY_RATE, 0),
				props.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000), 
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.BID_RANGE_MIN, 0),
				props.getAsInt(Keys.BID_RANGE_MAX, 5000));
	}
	
	/**
	 * Constructor for testing purposes (ZIAgentTest)
	 */
	public ZIAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, double reentryRate, PrivateValue pv,
			int tickSize, int bidRangeMin, int bidRangeMax){
		super(arrivalTime, fundamental, sip, market, rand, reentryRate,
				pv, tickSize, bidRangeMin, bidRangeMax);
	}

	@Override
	public Iterable<? extends Activity> agentStrategy(TimeStamp currentTime) {
		// 50% chance of being either long or short
		int quantity = rand.nextBoolean() ? 1 : -1;
		return this.executeZIStrategy(quantity, currentTime);
	}

	@Override
	public String toString() {
		return "ZI " + super.toString();
	}
	
}
