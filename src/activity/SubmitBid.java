package activity;

import java.util.Collection;

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
	private int price;
	private int quantity;
	
	public SubmitBid(Agent ag, Market mkt, int p, int q, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.mkt = mkt;
		this.price = p;
		this.quantity = q;
	}
	
	public SubmitBid deepCopy() {
		return new SubmitBid(this.ag, this.mkt, this.price, this.quantity, this.time);
	}
	
	public Collection<Activity> execute() {
		return this.ag.executeSubmitBid(this.mkt, this.price, this.quantity, this.time);
	}
	
	public String toString() {
		return new String("SubmitBid::" + this.ag + "," + this.mkt + "+(" + this.price + ", " +
				this.quantity + ")");
	}
}
