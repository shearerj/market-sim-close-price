package fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.abs;
import static java.lang.Integer.signum;

import java.io.Serializable;

public class Order<P extends Comparable<? super P>, T extends Comparable<? super T>> implements Serializable {

	private static final long serialVersionUID = -3460176014871040729L;
	
	protected final P price;
	protected int totalQuantity; // Negative to sell
	protected int quantity;
	protected final T submitTime;

	protected Order(P price, int initialQuantity, T submitTime) {
		checkArgument(initialQuantity != 0, "Initial Quantity can't be zero");
		this.price = checkNotNull(price, "Price");
		this.totalQuantity = initialQuantity;
		this.quantity = initialQuantity;
		this.submitTime = checkNotNull(submitTime, "Submit Time");
	}
	
	public static <P extends Comparable<? super P>, T extends Comparable<? super T>> Order<P, T> create(
			P price, int initialQuantity, T submitTime) {
		return new Order<P, T>(price, initialQuantity, submitTime);
	}

	Transaction<P, T> transact(Order<P, T> other, int buyQuantity) {
		Order<P, T> buy = this.totalQuantity > 0 ? this : other;
		Order<P, T> sell = this.totalQuantity > 0 ? other : this;
		
		checkArgument(sell.price.compareTo(buy.price) <= 0, "Invalid Price");
		buy.quantity -= buyQuantity;
		sell.quantity += buyQuantity;
		return Transaction.create(buy, sell, buyQuantity);
	}
	
	void withdraw(int quantity) {
		if (abs(quantity) > abs(this.quantity) || signum(quantity) != signum(this.quantity))
			throw new IllegalArgumentException("Can't withdraw more than are in order");
		this.totalQuantity -= quantity;
		this.quantity -= quantity;
	}

	public P getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
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
		return "(" + price + ", " + quantity + ", " + submitTime + ")";
	}
	
}
