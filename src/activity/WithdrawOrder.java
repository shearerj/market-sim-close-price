package activity;

import java.util.Collection;

import entity.market.Order;
import event.TimeStamp;

/**
 * Class for Activity of withdrawing an agent's order in a given market. Used when bids expire as
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

	@Override
	public Collection<? extends Activity> execute(TimeStamp time) {
		return order.getMarket().withdrawOrder(order, quantity, time);
	}
	
	@Override
	public String toString() {
		return super.toString() + order;
	}
	
}
