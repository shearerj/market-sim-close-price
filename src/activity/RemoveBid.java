package activity;

import market.*;
import entity.*;
import event.*;

/**
 * Activity to remove a bid from a market.
 * 
 * @author ewah
 */
public class RemoveBid extends Activity 
{
	private Bid bid;
	private Market mkt;
	
	public RemoveBid(Market mkt, Bid b, TimeStamp t) {
		this.mkt = mkt;
		this.bid = b;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
//		return this.mkt.removeBid(this.bid, this.time);
		return null;
	}
	
}
