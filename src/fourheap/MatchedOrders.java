package fourheap;

import java.io.Serializable;

public class MatchedOrders<BS, P extends Comparable<? super P>, T extends Comparable<? super T>> implements Serializable {

	private static final long serialVersionUID = -6073835626927361670L;
	
	protected final Order<BS, P, T> buy;
	protected final Order<BS, P, T> sell;
	protected final int quantity;
	
	protected MatchedOrders(Order<BS, P, T> buy, Order<BS, P, T> sell, int quantity) {
		this.buy = buy;
		this.sell = sell;
		this.quantity = quantity;
	}

	public static <BS, P extends Comparable<? super P>, T extends Comparable<? super T>> MatchedOrders<BS, P, T> create(
			Order<BS, P, T> buy, Order<BS, P, T> sell, int quantity) {
		return new MatchedOrders<BS, P, T>(buy, sell, quantity);
	}

	public Order<BS, P, T> getBuy() {
		return buy;
	}

	public Order<BS, P, T> getSell() {
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
