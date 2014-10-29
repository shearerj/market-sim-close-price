package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;

import java.util.Random;

import logger.Log;
import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.market.Market;
import entity.sip.MarketInfo;
import event.TimeLine;
import fourheap.Order.OrderType;

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

	protected ZIRAgent(int id, Stats stats, TimeLine timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		super(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}

	public static ZIRAgent create(int id, Stats stats, TimeLine timeline, Log log, Random rand, MarketInfo sip, FundamentalValue fundamental,
			Market market, Props props) {
		return new ZIRAgent(id, stats, timeline, log, rand, sip, fundamental, market, props);
	}

	@Override
	protected void agentStrategy() {
		super.agentStrategy();

		// 0.50% chance of being either long or short
		OrderType type = rand.nextBoolean() ? BUY : SELL;
		log(INFO, "%s Submit %s order", this, type);
		executeZIStrategy(type, 1);
	}
	
}
