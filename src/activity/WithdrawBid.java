package activity;

import entity.*;
import event.TimeStamp;

/**
 * Class for Activity of withdrawing an agent's bid in a given market. Could be due to
 * being replaced or because of expiration.
 * 
 * @author ewah
 */
public class WithdrawBid extends Activity {

	private Agent ag;
	private Market mkt;
	
	public WithdrawBid(Agent ag, Market mkt, TimeStamp t) {
		this.ag = ag;
		this.mkt = mkt;
		this.time = t;
	}
	
	public WithdrawBid deepCopy() {
		return new WithdrawBid(this.ag, this.mkt, this.time);
	}
	
	public ActivityHashMap execute() {
		return this.ag.executeWithdrawBid(this.mkt, this.time);
	}
	
	public String toString() {
		return new String("WithdrawBid::" + this.ag + "," + this.mkt);
	}
}
