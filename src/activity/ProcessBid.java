package activity;

import market.*;
import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of processing a market's bid.
 * 
 * @author ewah
 */
public class ProcessBid extends Activity {

	private Market mkt;
	private Bid bid;
	
	public ProcessBid(Market mkt, Bid b, TimeStamp t) {
		this.mkt = mkt;
		this.bid = b;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
//		return this.mkt.processBid(this.bid);
		return null;
	}
}
