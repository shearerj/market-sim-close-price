package activity;

import entity.*;
import event.*;
import activity.market.*;

/**
 * Class for Activity of adding a bid to a market.
 * 
 * @author ewah
 */
public class AddBid extends Activity {

	private Market mkt;
	private Bid bid;
	
	public AddBid(Market mkt, Bid b, TimeStamp t) {
		this.mkt = mkt;
		this.bid = b;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.mkt.addBid(this.bid);
	}
}
