package activity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
		checkArgument(quantity != 0, "Quantiy can't be 0");
		this.order = checkNotNull(order, "Order");
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
