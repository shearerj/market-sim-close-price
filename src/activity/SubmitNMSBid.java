package activity;

import java.util.Collection;

import market.Price;

import org.apache.commons.lang3.builder.*;

import entity.*;
import event.*;

/**
 * Class for Activity of submitting a National Market System (NMS) bid (price + quantity) to a
 * market. Will submit the bid to the best market of those available.
 * 
 * @author ewah
 */
public class SubmitNMSBid extends Activity {

	protected final Agent agent;
	protected final Price price;
	protected final int quantity;
	protected final Market primaryMarket;
	protected final TimeStamp duration;

	public SubmitNMSBid(Agent agent, Price price, int quantity,
			Market primaryMarket, TimeStamp scheduledTime, TimeStamp duration) {
		super(scheduledTime);
		this.agent = agent;
		this.price = price;
		this.quantity = quantity;
		this.primaryMarket = primaryMarket;
		this.duration = duration;
	}

	public SubmitNMSBid(Agent agent, Price price, int quantity,
			Market primaryMarket, TimeStamp scheduledTime) {
		this(agent, price, quantity, primaryMarket, scheduledTime,
				TimeStamp.IMMEDIATE);
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return primaryMarket.submitNMSBid(agent, price, quantity, currentTime);
	}

	public String toString() {
		return new String(getName() + "::" + agent + "," + "+(" + price + ", "
				+ quantity + ")");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null | getClass() != obj.getClass()) return false;
		SubmitNMSBid other = (SubmitNMSBid) obj;
		return this.agent.equals(other.agent) && this.price.equals(other.price)
				&& this.quantity == other.quantity
				&& this.primaryMarket == other.primaryMarket;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(price).append(
				quantity).append(scheduledTime).toHashCode();
	}

}
