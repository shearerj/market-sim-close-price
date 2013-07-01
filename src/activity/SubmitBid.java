package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

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

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.ag.executeSubmitBid(this.mkt, this.price, this.quantity, time);
	}

	public String toString() {
		return new String(getName() + "::" + ag + "," + mkt + "+(" + price + ", " +
				quantity + ")");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubmitBid other = (SubmitBid) obj;
		return new EqualsBuilder().
				append(ag.getID(), other.ag.getID()).
				append(mkt.getID(), other.mkt.getID()).
				append(price, other.price).
				append(quantity, other.quantity).
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
