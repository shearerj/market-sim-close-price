package activity;

import entity.*;
import event.*;

import java.util.ArrayList;

/**
 * Class for Activity of submitting a multiple-offer bid to a given market.
 * 
 * @author ewah
 */
public class SubmitMultipleBid extends Activity {

	private Agent ag;
	private Market mkt;
	private ArrayList<Integer> price;
	private ArrayList<Integer> quantity;
	
	public SubmitMultipleBid(Agent ag, Market mkt, ArrayList<Integer> p, ArrayList<Integer> q, TimeStamp t) {
		this.ag = ag;
		this.mkt = mkt;
		this.price = p;
		this.quantity = q;
		this.time = t;
	}
	
	public SubmitMultipleBid deepCopy() {
		return new SubmitMultipleBid(this.ag, this.mkt, this.price, this.quantity, this.time);
	}
	
	public ActivityHashMap execute() {
		return this.ag.executeSubmitMultipleBid(this.mkt, this.price, this.quantity, this.time);
	}
	
	public String toString() {
		return new String("SubmitMultipleBid::" + this.ag + "," + this.mkt + "+(" + 
							this.price + "," + this.quantity + ")");
	}
}
