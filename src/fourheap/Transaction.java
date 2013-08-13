package fourheap;

import java.io.Serializable;

public class Transaction<P extends Comparable<P>, T extends Comparable<T>> implements Serializable {

	private static final long serialVersionUID = -6073835626927361670L;
	
	protected final Order<P, T> buy, sell;
	protected final int quantity;
	
	public Transaction(Order<P, T> buy, Order<P, T> sell, int quantity) {
		this.buy = buy;
		this.sell = sell;
		this.quantity = quantity;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + buy.hashCode();
		result = prime * result + quantity;
		result = prime * result + sell.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) return false;
		Transaction<?, ?> other = (Transaction<?, ?>) obj;
		return buy.equals(other.buy) && sell.equals(other.sell)
				&& quantity == other.quantity;
	}
	
	@Override
	public String toString() {
		return "<buy=" + buy + ", sell=" + sell + ", quantity="
				+ quantity + ">";
	}

}
