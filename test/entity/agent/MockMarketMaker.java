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
		this(scheduler, fundamental, sip, market, false, numRungs, rungSize, false);
	}
	
	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip,
			Market market, boolean noOp, int numRungs, int rungSize, 
			boolean truncateLadder) {
		super(scheduler, fundamental, sip, market, new Random(), 0, 1, noOp, 
				numRungs, rungSize, truncateLadder, false);
	}
	
	public MockMarketMaker(Scheduler scheduler, FundamentalValue fundamental, SIP sip, Market market,
			EntityProperties props) {
		this(scheduler, fundamental, sip, market, 
				props.getAsBoolean(Keys.NO_OP, false),
				props.getAsInt(Keys.NUM_RUNGS, 10),
				props.getAsInt(Keys.RUNG_SIZE, 1000), 
				props.getAsBoolean(Keys.TRUNCATE_LADDER, true));
	}

}
