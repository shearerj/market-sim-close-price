package entity.agent;

import java.util.Random;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;

public class MockMarketMaker extends MarketMaker {

	private static final long serialVersionUID = 1L;

	public MockMarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			int numRungs, int rungSize) {
		this(fundamental, sip, market, false, numRungs, rungSize, false);
	}
	
	public MockMarketMaker(FundamentalValue fundamental, SIP sip,
			Market market, boolean noOp, int numRungs, int rungSize, 
			boolean truncateLadder) {
		super(fundamental, sip, market, new Random(), 0, 1, noOp, 
				numRungs, rungSize, truncateLadder);
	}

	@Override
	public String toString() {
		return "MockMarketMaker " + super.toString();
	}
}
