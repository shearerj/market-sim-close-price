package activity;

import entity.*;
import event.*;

import java.util.List;
import java.util.Collection;
import org.apache.commons.lang3.builder.*;

/**
 * Class for Activity of submitting a multiple-offer bid to a given market.
 * 
 * @author ewah
 */
public class SubmitMultipleBid extends Activity {

	private Agent ag;
	private Market mkt;
	private List<Integer> price;
	private List<Integer> quantity;

	public SubmitMultipleBid(Agent ag, Market mkt, List<Integer> p, List<Integer> q, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.mkt = mkt;
		this.price = p;
		this.quantity = q;
	}

	public SubmitMultipleBid deepCopy() {
		return new SubmitMultipleBid(this.ag, this.mkt, this.price, this.quantity, this.time);
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.ag.executeSubmitMultipleBid(this.mkt, this.price, this.quantity, time);
	}

	public String toString() {
		return new String(getName() + "::" + ag + "," + mkt + "+(" + 
				price + "," + quantity + ")");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubmitMultipleBid other = (SubmitMultipleBid) obj;
		return new EqualsBuilder().
				append(ag.getID(), other.ag.getID()).
				append(mkt.getID(), other.mkt.getID()).
				append(price.hashCode(), other.price.hashCode()).
				append(quantity.hashCode(), other.quantity.hashCode()).
				append(time.longValue(), other.time.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(ag.getID()).
				append(mkt.getID()).
				append(price).
				append(quantity).
				append(time.longValue()).
				toHashCode();
	}
}
