package fourheap;

import java.io.Serializable;

public class Transaction<P extends Comparable<? super P>, T extends Comparable<? super T>> implements Serializable {

	private static final long serialVersionUID = -6073835626927361670L;
	
	protected final Order<P, T> buy, sell;
	protected final int quantity;
	
	protected Transaction(Order<P, T> buy, Order<P, T> sell, int quantity) {
		this.buy = buy;
		this.sell = sell;
		this.quantity = quantity;
	}

	public static <P extends Comparable<? super P>, T extends Comparable<? super T>> Transaction<P, T> create(
			Order<P, T> buy, Order<P, T> sell, int quantity) {
		return new Transaction<P, T>(buy, sell, quantity);
	}

	public Order<P, T> getBuy() {
		return buy;
	}

	public Order<P, T> getSell() {
		return sell;
	}

	public int getQuantity() {
		return quantity;
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
		return "<buy=" + buy + ", sell=" + sell + ", quantity="
				+ quantity + ">";
	}

}
