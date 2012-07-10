package activity;

import market.*;
import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of withdrawing a Bid from a market. Could be due to
 * being replaced or because of expiration.
 * 
 * @author ewah
 */
public class WithdrawBid extends Activity {

	private Bid bid;
	private Agent ag;
	private Market mkt;
	
	public WithdrawBid(Agent ag, Bid b, Market mkt, TimeStamp t) {
		this.ag = ag;
		this.bid = b;
		this.mkt = mkt;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.withdrawBid(this.bid, this.mkt, this.time);
	}
}
