package activity;

import entity.*;
import event.*;

import java.util.ArrayList;

/**
 * Class for Activity of submitting a National Market System (NMS) bid (price +
 * quantity) to a market. Will submit the bid to the best market of those available.
 * 
 * @author ewah
 */
public class SubmitNMSBid extends Activity {
	
	private SMAgent ag;
	private int price;
	private int quantity;
	private long duration;
	
	public SubmitNMSBid(SMAgent ag, int p, int q, long d, TimeStamp t) {
		this.ag = ag;
		this.price = p;
		this.quantity = q;
		this.duration = d;
		this.time = t;
	}
	
	public SubmitNMSBid deepCopy() {
		return new SubmitNMSBid(this.ag, this.price, this.quantity, this.duration, this.time);
	}
	
	public ActivityHashMap execute() {
		return this.ag.executeSubmitNMSBid(this.price, this.quantity, this.duration, this.time);
	}
	
	public String toString() {
		return new String("SubmitNMSBid::" + this.ag + "," + ag.getMarketSubmittedBid() + 
				"+(" + this.price + ", " + this.quantity + "), duration=" + duration);
	}
}