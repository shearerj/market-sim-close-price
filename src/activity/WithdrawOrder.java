package activity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import entity.market.Order;
import event.TimeStamp;

/**
 * Class for Activity of withdrawing an agent's order in a given market. 
 * Needs to be called with the order that is intended to be removed. Note that
 * this is only necessary for <b>scheduled</b> order withdrawals. Immediate
 * order withdrawals need to call the Agent class' withdrawOrder method directly.
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
		checkArgument(quantity >= 0, "Quantity must be nonnegative");
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
