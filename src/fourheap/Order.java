package fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

public class Order<P extends Comparable<? super P>, T extends Comparable<? super T>> implements Serializable {

	private static final long serialVersionUID = -3460176014871040729L;
	
	protected final P price;
	protected int unmatchedQuantity, matchedQuantity; // Negative for sell order
	protected final T submitTime;

	protected Order(P price, int initialQuantity, T submitTime) {
		checkArgument(initialQuantity != 0, "Initial Quantity can't be zero");
		this.price = checkNotNull(price, "Price");
		this.unmatchedQuantity = initialQuantity;
		this.matchedQuantity = 0;
		this.submitTime = checkNotNull(submitTime, "Submit Time");
	}
	
	public static <P extends Comparable<? super P>, T extends Comparable<? super T>> Order<P, T> create(
			P price, int initialQuantity, T submitTime) {
		return new Order<P, T>(price, initialQuantity, submitTime);
	}

	/**
	 * 
	 * @param other The other order to match
	 * @param quantity The number of orders that transact. Always positive
	 * @return
	 */
	MatchedOrders<P, T> match(Order<P, T> other, int quantity) {
		Order<P, T> buy  = (matchedQuantity + unmatchedQuantity) > 0 ? this  : other;
		Order<P, T> sell = (matchedQuantity + unmatchedQuantity) > 0 ? other : this;
		
		checkArgument(sell.price.compareTo(buy.price) <= 0, "Invalid Price");
		checkArgument(buy.matchedQuantity >= quantity, "Tried to transact with more than were matched");
		checkArgument(sell.matchedQuantity <= -quantity, "Tried to transact with more than were matched");
		
		buy.matchedQuantity -= quantity;
		sell.matchedQuantity += quantity;
		return MatchedOrders.create(buy, sell, quantity);
	}

	public P getPrice() {
		return price;
	}

	public int getQuantity() {
		return unmatchedQuantity + matchedQuantity;
	}

	public T getSubmitTime() {
		return submitTime;
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "(" + price + ", " + getQuantity() + ", " + submitTime + ")";
	}
	
}
