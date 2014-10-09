package entity.agent;

import java.util.Random;

import systemmanager.Keys;
import systemmanager.Scheduler;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;

public class MockMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 1L;

	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			int numRungs, int rungSize) {
		this(scheduler, fundamental, sip, market, numRungs, rungSize, false, false, true, false);
	}
	
	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Market market, int numRungs, int rungSize, boolean truncateLadder, 
			boolean tickImprovement, boolean tickInside, boolean fix) {
		super(scheduler, fundamental, sip, market, new Random(), 0, 1, numRungs, 
				rungSize, truncateLadder, tickImprovement, tickInside, 0, 0, fix);
	}
	
	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Market market, int numRungs, int rungSize, boolean truncateLadder, 
			boolean tickImprovement, boolean tickInside, int initLadderMean,
			int initLadderRange, boolean fix) {
		super(scheduler, fundamental, sip, market, new Random(), 0, 1, numRungs, 
				rungSize, truncateLadder, tickImprovement, tickInside, initLadderMean,
				initLadderRange, fix);
	}

	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			EntityProperties props) {
		this(scheduler, fundamental, sip, market,
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000),
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true), 
				props.getAsBoolean(Keys.TICK_IMPROVEMENT, false),
				props.getAsBoolean(Keys.TICK_OUTSIDE, true),
				props.getAsInt(Keys.INITIAL_LADDER_MEAN, 0),
				props.getAsInt(Keys.INITIAL_LADDER_RANGE, 0),
				props.getAsBoolean(Keys.FIX, false));
	}
}
