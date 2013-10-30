package fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

/**
 * An order meant for use in a fourheap
 * 
 * @author ebrink
 * 
 * @param <P>
 *            Price
 * @param <T>
 *            Time
 */
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
	
	/**
	 * Factory constructor
	 */
	public static <P extends Comparable<? super P>, T extends Comparable<? super T>> Order<P, T> create(
			P price, int initialQuantity, T submitTime) {
		return new Order<P, T>(price, initialQuantity, submitTime);
	}

	/**
	 * Get the Price
	 * 
	 * @return The price
	 */
	public P getPrice() {
		return price;
	}

	/**
	 * Get the quantity. Negative for sell orders
	 * 
	 * @return The quantity
	 */
	public int getQuantity() {
		return unmatchedQuantity + matchedQuantity;
	}

	/**
	 * Get the submission Time of the order
	 * 
	 * @return The submission Time
	 */
	public T getSubmitTime() {
		return submitTime;
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	@Override
	/**
	 * All Orders are unique
	 */
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "<" + submitTime + "| " + (getQuantity() > 0 ? "BUY " : "SELL ") + Math.abs(getQuantity()) + " @ " + price + ">";
	}
	
}
