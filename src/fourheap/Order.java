package fourheap;

public class Order<P extends Comparable<P>, T extends Comparable<T>> {

	protected final P price;
	protected int totalQuantity;
	protected int quantity; // Negative to sell
	protected final T submitTime;

	public Order(P price, int initialQuantity, T submitTime) {
		this.price = price;
		this.totalQuantity = initialQuantity;
		this.quantity = initialQuantity;
		this.submitTime = submitTime;
	}

	void transact(Order<P, T> other, int quantity) {
		// FIXME Transact, reduce quantity. Should this take a quantity, or just calculate it. Will
		// we ever not want to transact the full amount?
	}
	
	void withdraw(int quantity) {
		// FIXME withdraw quantity from quantity
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
