package activity;

import java.util.Collection;

import entity.*;
import event.*;

/**
 * Class for Activity of submitting a National Market System (NMS) multiple-point bid
 * to a market. Note that the SMAgent can see the main market's best quote with zero delay.
 * 
 * @author ewah
 */
public class SubmitNMSMultipleBid extends Activity {

	private SMAgent ag;
	private int[] price;
	private int[] quantity;
	
	public SubmitNMSMultipleBid(SMAgent ag, int[] p, int[] q, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.price = p;
		this.quantity = q;
	}
	
	public SubmitNMSMultipleBid deepCopy() {
		return new SubmitNMSMultipleBid(this.ag, this.price, this.quantity, this.time);
	}
	
	public Collection<Activity> execute(TimeStamp time) {
		return this.ag.executeSubmitNMSMultipleBid(this.price, this.quantity, time);
	}
	
	public String toString() {
		return new String("SubmitNMSMultipleBid::" + this.ag + "," + 
				"+(" + this.price + "," + this.quantity + ")");
	}
}
