package activity;

import java.util.Collection;


import org.apache.commons.lang3.builder.*;

import entity.agent.*;
import entity.market.Market;
import entity.market.Price;
import event.*;

/**
 * Class for Activity of submitting a National Market System (NMS) bid (price + quantity) to a
 * market. Will submit the bid to the best market of those available.
 * 
 * @author ewah
 */
public class SubmitNMSOrder extends Activity {

	protected final Agent agent;
	protected final Price price;
	protected final int quantity;
	protected final Market primaryMarket;
	protected final TimeStamp duration;

	public SubmitNMSOrder(Agent agent, Price price, int quantity,
			Market primaryMarket, TimeStamp scheduledTime, TimeStamp duration) {
		super(scheduledTime);
		this.agent = agent;
		this.price = price;
		this.quantity = quantity;
		this.primaryMarket = primaryMarket;
		this.duration = duration;
	}

	public SubmitNMSOrder(Agent agent, Price price, int quantity,
			Market primaryMarket, TimeStamp scheduledTime) {
		this(agent, price, quantity, primaryMarket, scheduledTime,
				TimeStamp.IMMEDIATE);
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return primaryMarket.submitNMSOrder(agent, price, quantity, currentTime);
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(price).append(
				quantity).append(super.hashCode()).toHashCode();
	}
	
	@Override
	public String toString() {
		return super.toString() + agent + "(" + price + ", " + quantity
				+ ") -> " + primaryMarket;
	}
	
}
