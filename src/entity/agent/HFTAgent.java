package entity.agent;

import java.util.Collection;
import java.util.Random;

import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * Abstract class for high-frequency traders.
 * 
 * @author ewah
 */
public abstract class HFTAgent extends MMAgent {

	private static final long serialVersionUID = -1483633963238206201L;

	public HFTAgent(TimeStamp arrivalTime, Collection<Market> markets,
			FundamentalValue fundamental, SIP sip, Random rand, int tickSize) {
		super(arrivalTime, markets, fundamental, sip, new PrivateValue(), rand,
				tickSize);
	}

}
