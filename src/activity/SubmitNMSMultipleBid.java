package activity;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.builder.*;

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
	private List<Integer> price;
	private List<Integer> quantity;

	public SubmitNMSMultipleBid(SMAgent ag, List<Integer> p, List<Integer> q, TimeStamp t) {
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
		return new String(getName() + "::" + ag + "," + "+(" + price + "," 
				+ quantity + ")");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubmitNMSMultipleBid other = (SubmitNMSMultipleBid) obj;
		return new EqualsBuilder().
				append(ag.getID(), other.ag.getID()).
				append(price.hashCode(), other.price.hashCode()).
				append(quantity.hashCode(), other.quantity.hashCode()).
				append(time.longValue(), other.time.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(ag.getID()).
				append(price).
				append(quantity).
				append(time.longValue()).
				toHashCode();
	}
}
