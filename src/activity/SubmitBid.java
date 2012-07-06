package activity;

import entity.*;
import event.*;
import activity.market.*;

/**
 * Class for Activity of submitting a Bid to a market.
 * 
 * @author ewah
 */
public class SubmitBid extends Activity {
	
	private Bid bid;
	private Agent ag;
	private Market mkt;
	
	public SubmitBid(Agent ag, Bid b, Market mkt, TimeStamp t) {
		this.ag = ag;
		this.bid = b;
		this.mkt = mkt;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.submitBid(this.bid, this.mkt);
	}
	
}