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

	protected final Agent agent;
	protected final Market market;
	protected final Price price;
	protected final int quantity;

	public SubmitOrder(Agent agent, Market market, Price price, int quantity,
			TimeStamp scheduledTime) {
		super(scheduledTime);
		this.agent = agent;
		this.market = market;
		this.price = price;
		this.quantity = quantity;
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return market.submitOrder(agent, price, quantity, currentTime);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(market).append(
				price).append(quantity).append(super.hashCode()).toHashCode();
	}
	
	@Override
	public String toString() {
		return super.toString() + agent + "+(" + price + ", " + quantity
				+ ") -> " + market;
	}
	
}
