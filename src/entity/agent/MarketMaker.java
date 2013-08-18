package entity.agent;

import java.util.Random;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * Abstract class for MarketMakers. Makes it easier to test when an agent is a market maker.
 * 
 * @author ewah
 */
// TODO Currently MarketMakers don't ever schedule a liquidate. Not sure exactly when this should
// happen.
public abstract class MarketMaker extends SMAgent {

	private static final long serialVersionUID = -782740037969385370L;

	public MarketMaker(FundamentalValue fundamental, SIP sip, Market market,
			Random rand, int tickSize) {
		super(TimeStamp.ZERO, fundamental, sip, market, new PrivateValue(),
				rand, tickSize);
	}

}
