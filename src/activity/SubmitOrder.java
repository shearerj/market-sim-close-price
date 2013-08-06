package activity;

import java.util.Collection;


import org.apache.commons.lang3.builder.*;

import entity.agent.*;
import entity.market.Market;
import entity.market.Price;
import event.*;

/**
 * Class for Activity of submitting a bid (price + quantity) to a market.
 * 
 * @author ewah
 */
public class SubmitOrder extends Activity {

	// TODO Change this to a Bid Object. Will take a bid more thought.
	private Agent agent;
	private Market market;
	private Price price;
	private int quantity;

	public SubmitOrder(Agent agent, Market market, Price price, int quantity,
			TimeStamp scheduledTime) {
		super(scheduledTime);
		this.agent = agent;
		this.market = market;
		this.price = price;
		this.quantity = quantity;
	}

	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return market.submitOrder(agent, price, quantity, currentTime);
	}

	public String toString() {
		return new String(getName() + "::" + agent + "," + market + "+("
				+ price + ", " + quantity + ")");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) return false;
		SubmitOrder other = (SubmitOrder) obj;
		return new EqualsBuilder().append(agent, other.agent).append(market,
				other.market).append(price, other.price).append(quantity,
				other.quantity).append(scheduledTime, other.scheduledTime).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(market).append(
				price).append(quantity).append(scheduledTime).toHashCode();
	}
}
