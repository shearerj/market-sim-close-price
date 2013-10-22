package activity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
		this(order, Math.abs(order.getQuantity()), scheduledTime);
	}

	public WithdrawOrder(Order order, int quantity, TimeStamp scheduledTime) {
		super(scheduledTime);
		checkArgument(quantity > 0, "Quantiy must be positive");
		this.order = checkNotNull(order, "Order");
		this.quantity = quantity;
	}

	@Override
	public Iterable<? extends Activity> execute(TimeStamp time) {
		return order.getMarket().withdrawOrder(order, quantity, time);
	}
	
	@Override
	public String toString() {
		return super.toString() + order;
	}
	
}
