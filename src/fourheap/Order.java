package fourheap;

import static java.lang.Math.abs;
import static java.lang.Integer.signum;

import java.io.Serializable;

public class Order<P extends Comparable<P>, T extends Comparable<T>> implements Serializable {

	private static final long serialVersionUID = -3460176014871040729L;
	
	protected final P price;
	protected int totalQuantity; // Negative to sell
	protected int quantity;
	protected final T submitTime;

	public Order(P price, int initialQuantity, T submitTime) {
		this.price = price;
		this.totalQuantity = initialQuantity;
		this.quantity = initialQuantity;
		this.submitTime = submitTime;
	}
	
	public Order(P price, T submitTime) {
		this(price, 1, submitTime);
	}

	Transaction<P, T> transact(Order<P, T> other, int buyQuantity) {
		Order<P, T> buy = this.totalQuantity > 0 ? this : other;
		Order<P, T> sell = this.totalQuantity > 0 ? other : this;
		
		if (sell.price.compareTo(buy.price) > 0)
			throw new IllegalArgumentException("Invalid Price");
		buy.quantity -= buyQuantity;
		sell.quantity += buyQuantity;
		return new Transaction<P, T>(buy, sell, buyQuantity);
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
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "(" + price + ", " + quantity + ", " + submitTime + ")";
	}
	
}
