package fourheap;

public class Order<P extends Comparable<P>, T extends Comparable<T>> {

	protected final P price;
	protected int quantity; // Negative to sell
	protected final T submitTime;

	public Order(P price, int initialQuantity, T submitTime) {
		this.price = price;
		this.quantity = initialQuantity;
		this.submitTime = submitTime;
	}

	void removeQuantity(int quantityToRemove) {
		// TODO make sure you can only remove quantity, and it has to be less than or equal to the
		// amount remaining
		quantity -= quantityToRemove;
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
