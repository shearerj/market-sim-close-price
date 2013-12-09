package entity.agent;

import java.util.Iterator;
import java.util.Random;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MockReentryAgent extends ReentryAgent {

	private static final long serialVersionUID = 1L;

	public MockReentryAgent(TimeStamp arrivalTime,
			FundamentalValue fundamental, SIP sip, Market market, Random rand,
			Iterator<TimeStamp> reentry, int tickSize) {
		super(arrivalTime, fundamental, sip, market, rand, reentry, tickSize);
		// TODO Auto-generated constructor stub
	}

}
