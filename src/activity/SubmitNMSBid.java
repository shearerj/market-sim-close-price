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
	
	public SubmitNMSBid(SMAgent ag, int p, int q, TimeStamp t) {
		this.ag = ag;
		this.price = p;
		this.quantity = q;
		this.time = t;
	}
	
	public SubmitNMSBid deepCopy() {
		return new SubmitNMSBid(this.ag, this.price, this.quantity, this.time);
	}
	
	public ActivityHashMap execute() {
		return this.ag.executeSubmitNMSBid(this.price, this.quantity, this.time);
	}
	
	public String toString() {
		return new String("SubmitNMSBid::" + this.ag + "," + ag.getMarketSubmittedBid() + 
				"+(" + this.price + ", " + this.quantity + ")");
	}
}
