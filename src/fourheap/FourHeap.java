package fourheap;

import static fourheap.CompareUtils.max;
import static fourheap.CompareUtils.min;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class FourHeap<P extends Comparable<P>, T extends Comparable<T>> {

	protected final BinaryHeap<SplitOrder> sellUnmatched, sellMatched,
			buyUnmatched, buyMatched;
	// Left is unmatched, Right is matched
	protected final Map<Order<P, T>, Pair<SplitOrder, SplitOrder>> activeOrders;

	public FourHeap() {
		sellUnmatched = new BinaryHeap<SplitOrder>(new OrderComparator(true,
				true));
		sellMatched = new BinaryHeap<SplitOrder>(new OrderComparator(false,
				true));
		buyUnmatched = new BinaryHeap<SplitOrder>(new OrderComparator(false,
				false));
		buyMatched = new BinaryHeap<SplitOrder>(
				new OrderComparator(true, false));
		activeOrders = new HashMap<Order<P, T>, Pair<SplitOrder, SplitOrder>>();
	}

	public void insertOrder(Order<P, T> order) {
		if (order.quantity == 0)
			throw new IllegalArgumentException(
					"Orders must have nonzero quantity");

		int t = Integer.signum(order.quantity); // Sell or Buy
		BinaryHeap<SplitOrder> unmatchedHeap = t < 0 ? buyUnmatched
				: sellUnmatched;
		int unmatchedQuantity = order.quantity;
		while (unmatchedQuantity * t > 0 && !unmatchedHeap.isEmpty()
				&& unmatchedHeap.peek().price.compareTo(order.price) * t <= 0) {

			SplitOrder match = unmatchedHeap.poll();
			int quantityMatched = Math.min(unmatchedQuantity, -match.quantity);
			match(match.order, match.order.quantity - match.quantity
					- quantityMatched);
			unmatchedQuantity -= quantityMatched;
		}
		match(order, order.quantity - unmatchedQuantity);
	}

	public void withdrawOrder(Order<P, T> order) {
		withdrawOrder(order, order.quantity);
	}

	public void withdrawOrder(Order<P, T> order, int quantity) {
		// FIXME First withdraw from unmatched portion, then withdraw from matched portion
	}

	protected void match(Order<P, T> order, int totalMatched) {
		// Assign selling or buying
		BinaryHeap<SplitOrder> matchedHeap, unmatchedHeap;
		if (order.quantity < 0) {
			matchedHeap = sellMatched;
			unmatchedHeap = sellUnmatched;
		} else {
			matchedHeap = buyMatched;
			unmatchedHeap = buyUnmatched;
		}
		// Remove any existing split orders
		Pair<SplitOrder, SplitOrder> active = activeOrders.get(order);
		if (active != null) {
			if (active.left() != null) unmatchedHeap.remove(active.left());
			if (active.right() != null) matchedHeap.remove(active.right());
		}
		// Add new split orders
		SplitOrder unmatched, matched;
		if (totalMatched == 0) { // Completely Unmatched
			matched = null;
		} else {
			matched = new SplitOrder(order, totalMatched);
			matchedHeap.offer(matched);
		}
		if (totalMatched == order.quantity) { // Completely Matched
			unmatched = null;
		} else {
			unmatched = new SplitOrder(order, order.quantity - totalMatched);
			unmatchedHeap.offer(unmatched);
		}
		activeOrders.put(order, new Pair<SplitOrder, SplitOrder>(unmatched,
				matched));
	}

	/**
	 * Returns the Price quote as a pair of Price types. The left element is the buy price, the
	 * right element is the sell price.
	 * 
	 * @return Pair(Buy Price, Sell Price)
	 */
	public Pair<P, P> quote() {
		// Note to haskell lovers. Maybe Monad makes this much simpler
		P sin = sellMatched.peek() == null ? null : sellMatched.peek().price;
		P sout = sellUnmatched.peek() == null ? null
				: sellUnmatched.peek().price;
		P bin = buyMatched.peek() == null ? null : buyMatched.peek().price;
		P bout = buyUnmatched.peek() == null ? null : buyUnmatched.peek().price;
		return new Pair<P, P>(max(sin, bout), min(sout, bin));
	}

	@Override
	public String toString() {
		return "{Bo: " + buyUnmatched + ", So: " + sellUnmatched + ", Bi: "
				+ buyMatched + ", Si: " + sellMatched + "}";
	}

	protected class SplitOrder {
		protected final P price;
		protected final int quantity;
		protected final T time;
		protected final Order<P, T> order;

		SplitOrder(Order<P, T> backedOrder) {
			this(backedOrder, backedOrder.quantity);
		}

		SplitOrder(Order<P, T> backedOrder, int splitQuantity) {
			this.order = backedOrder;
			this.price = backedOrder.price;
			this.time = backedOrder.submitTime;
			this.quantity = splitQuantity;
		}

		@Override
		public String toString() {
			return "(" + price + ", " + quantity + ", " + time + ")";
		}
	}

	protected class OrderComparator implements Comparator<SplitOrder> {

		Comparator<P> priceComp;
		Comparator<T> timeComp;
		Comparator<Integer> quantComp;

		OrderComparator(boolean minPrice, boolean sell) {
			priceComp = minPrice ? CompareUtils.<P> naturalOrder()
					: Collections.<P> reverseOrder();
			quantComp = sell ? CompareUtils.<Integer> naturalOrder()
					: Collections.<Integer> reverseOrder();
			timeComp = CompareUtils.<T> naturalOrder();
		}

		@Override
		public int compare(SplitOrder o1, SplitOrder o2) {
			return 4
					* Integer.signum(priceComp.compare(o1.price, o2.price))
					+ Integer.signum(quantComp.compare(o1.quantity, o2.quantity))
					+ 2 * Integer.signum(o1.time.compareTo(o2.time));
		}

	}

}
