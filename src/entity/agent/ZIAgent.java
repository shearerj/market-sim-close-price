package entity.agent;

import java.util.Random;

import com.google.common.collect.Iterators;

import static fourheap.Order.OrderType.*;

import systemmanager.Keys;
import systemmanager.Scheduler;
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

	public ZIAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			double pvVar, int tickSize, int bidRangeMin, int bidRangeMax) {
		super(scheduler, arrivalTime, fundamental, sip, market, rand, Iterators
				.<TimeStamp> emptyIterator(), new PrivateValue(1, pvVar, rand),
				tickSize, bidRangeMin, bidRangeMax);
	}

	public ZIAgent(Scheduler scheduler, TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip,
			Market market, Random rand, EntityProperties props) {
		this(scheduler, arrivalTime, fundamental, sip, market, rand,
				props.getAsDouble(Keys.PRIVATE_VALUE_VAR, 100000000), 
				props.getAsInt(Keys.TICK_SIZE, 1),
				props.getAsInt(Keys.BID_RANGE_MIN, 0),
				props.getAsInt(Keys.BID_RANGE_MAX, 5000));
	}
	
	/**
	 * Constructor for testing purposes (ZIAgentTest)
	 * 
	 * TODO This shouldn't exist. It should be in the test classs or somewhere
	 * else.
	 */
	ZIAgent(Scheduler scheduler, TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			PrivateValue pv, int tickSize, int bidRangeMin, int bidRangeMax) {
		super(scheduler, arrivalTime, fundamental, sip, market, rand, Iterators
				.<TimeStamp> emptyIterator(), pv, tickSize, bidRangeMin,
				bidRangeMax);
	}

	@Override
	public void agentStrategy(TimeStamp currentTime) {
		// 50% chance of being either long or short
		this.executeZIStrategy(rand.nextBoolean() ? BUY : SELL, 1, currentTime);
	}
	
}
