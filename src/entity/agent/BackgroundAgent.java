package entity.agent;

import data.FundamentalValue;
import utils.Rands;
import entity.infoproc.SIP;
import entity.market.Market;
import event.TimeStamp;

/**
 * Abstract class for background traders. Makes it easier to test when 
 * an agent is in the background role.
 * 
 * @author ewah
 */
public abstract class BackgroundAgent extends SMAgent {
	
	private static final long serialVersionUID = 7742389103679854398L;

	public BackgroundAgent(TimeStamp arrivalTime, FundamentalValue fundamental, SIP sip, 
			Market market, PrivateValue pv, Rands rand, int tickSize) {
		super(arrivalTime, fundamental, sip, market, pv, rand, tickSize);
	}
	
}
