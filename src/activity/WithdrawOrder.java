package activity;

import java.util.Collection;


import org.apache.commons.lang3.builder.HashCodeBuilder;

import entity.market.Order;
import event.TimeStamp;

/**
 * Class for Activity of withdrawing an agent's bid in a given market. Used when bids expire as
 * well.
 * 
 * @author ewah
 */
public class WithdrawOrder extends Activity {

	protected final Order order;
	protected final int quantity;
	
	public WithdrawOrder(Order order, TimeStamp scheduledTime) {
		this(order, order.getQuantity(), scheduledTime);
	}

	public WithdrawOrder(Order order, int quantity, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.order = order;
		this.quantity = quantity;
	}

	public Collection<? extends Activity> execute(TimeStamp time) {
		return order.getMarket().withdrawOrder(order, quantity, time);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(order).append(super.hashCode()).toHashCode();
	}
	
	@Override
	public String toString() {
		return new String(getName() + " :: " + order);
	}
	
}
