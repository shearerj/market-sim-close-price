package activity;

import java.util.Collection;

import market.Price;

import org.apache.commons.lang3.builder.*;

import entity.*;
import event.*;

/**
 * Class for Activity of submitting a National Market System (NMS) bid (price +
 * quantity) to a market. Will submit the bid to the best market of those
 * available.
 * 
 * @author ewah
 */
public class SubmitNMSBid extends Activity {

	protected final SMAgent ag;
	protected final Price price;
	protected final int quantity;
	protected final TimeStamp duration;

	public SubmitNMSBid(SMAgent ag, Price price, int quantity,
			TimeStamp duration, TimeStamp ts) {
		super(ts);
		this.ag = ag;
		this.price = price;
		this.quantity = quantity;
		this.duration = duration;
	}

	public SubmitNMSBid deepCopy() {
		return new SubmitNMSBid(this.ag, this.price, this.quantity,
				this.duration, this.time);
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return this.ag.executeSubmitNMSBid(this.price, this.quantity,
				this.duration, time);
	}

	public String toString() {
		return new String(getName() + "::" + ag + "," + "+(" + price + ", "
				+ quantity + "), duration=" + duration);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null | getClass() != obj.getClass())
			return false;
		SubmitNMSBid other = (SubmitNMSBid) obj;
		return new EqualsBuilder().append(ag.getID(), other.ag.getID()).append(
				price, other.price).append(quantity, other.quantity).append(
				duration.longValue(), other.duration.longValue()).append(
				time.longValue(), other.time.longValue()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(ag.getID()).append(price).append(
				quantity).append(time.longValue()).toHashCode();
	}

	public SMAgent getAg() {
		// TODO Auto-generated method stub
		return ag;
	}
	
	public Price getPrice() {
		return price;
	}
	
	public int getQuantity() {
		return quantity;
	}
}
