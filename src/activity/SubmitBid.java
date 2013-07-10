package activity;

import java.util.Collection;

import market.Price;

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
	private Price price;
	private int quantity;

	public SubmitBid(Agent ag, Market mkt, Price p, int q, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.mkt = mkt;
		this.price = p;
		this.quantity = q;
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return this.ag.executeSubmitBid(mkt, price, quantity, currentTime);
	}

	public String toString() {
		return new String(getName() + "::" + ag + "," + mkt + "+(" + price + ", " +
				quantity + ")");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass())
			return false;
		SubmitBid other = (SubmitBid) obj;
		return new EqualsBuilder().
				append(ag, other.ag).
				append(mkt, other.mkt).
				append(price, other.price).
				append(quantity, other.quantity).
				append(time, other.time).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).
				append(ag).
				append(mkt).
				append(price).
				append(quantity).
				append(time).
				toHashCode();
	}
}
