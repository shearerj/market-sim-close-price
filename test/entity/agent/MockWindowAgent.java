package entity.agent;

import java.util.Random;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

public class MockWindowAgent extends WindowAgent {

	private static final long serialVersionUID = 1L;

	public MockWindowAgent(FundamentalValue fundamental, SIP sip, Market market,
			int windowLength) {
		this(fundamental, sip, market, new PrivateValue(), 0, 0, windowLength);
	}
	
	public MockWindowAgent(FundamentalValue fundamental, SIP sip, Market market, 
			PrivateValue pv, int bidRangeMin, int bidRangeMax,
			int windowLength) {
		super(TimeStamp.ZERO, fundamental, sip, market, new Random(), 0, 
				pv, 1, bidRangeMin, bidRangeMax, windowLength);
	}
	
}
