package activity;

import entity.*;
import event.*;

/**
 * Class for Activity of submitting a bid (price + quantity) to a market.
 * 
 * @author ewah
 */
public class SubmitBid extends Activity {
	
	private Agent ag;
	private Market mkt;
	private double price;
	private int quantity;
	
	public SubmitBid(Agent ag, Market mkt, double p, int q, TimeStamp t) {
		this.ag = ag;
		this.mkt = mkt;
		this.price = p;
		this.quantity = q;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.submitBid(this.mkt, this.price, this.quantity, this.time);
	}	
}