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
	// Left is unmatched, Right is matched Should have a split order of 0 if nothing exists in
	// either unmatched or matched. if a split order has quantity 0, it shouldn't be in its
	// respective heap.
	protected final Map<Order<P, T>, Pair<SplitOrder, SplitOrder>> activeOrders;

	public FourHeap() {
		sellUnmatched = new BinaryHeap<SplitOrder>(new OrderComparator(true, true));
		sellMatched = new BinaryHeap<SplitOrder>(new OrderComparator(false, true));
		buyUnmatched = new BinaryHeap<SplitOrder>(new OrderComparator(false, false));
		buyMatched = new BinaryHeap<SplitOrder>(new OrderComparator(true, false));
		activeOrders = new HashMap<Order<P, T>, Pair<SplitOrder, SplitOrder>>();
	}

	public void insertOrder(Order<P, T> order) {
		if (order.quantity == 0)
			throw new IllegalArgumentException("Orders must have nonzero quantity");
		
		int t = Integer.signum(order.quantity); // Sell or Buy
		BinaryHeap<SplitOrder> orderMatchedHeap, orderUnmatchedHeap, matchedHeap, unmatchedHeap;
		unmatchedHeap = t < 0 ? buyUnmatched : sellUnmatched;
		matchedHeap = t < 0 ? buyMatched : sellMatched;
		orderUnmatchedHeap = t > 0 ? buyUnmatched : sellUnmatched;
		orderMatchedHeap = t > 0 ? buyMatched : sellMatched;
		
		int unmatchedQuantity = order.quantity;
		while (unmatchedQuantity * t > 0 && !unmatchedHeap.isEmpty()
				&& unmatchedHeap.peek().price.compareTo(order.price) * t <= 0) {
			
			SplitOrder match = unmatchedHeap.poll();
			int quantityMatched = t*Math.min(t*unmatchedQuantity, -t*match.quantity);
			match(match.order, -quantityMatched, unmatchedHeap, matchedHeap);
			unmatchedQuantity -= quantityMatched;
		}
		activeOrders.put(order, new Pair<SplitOrder, SplitOrder>(new SplitOrder(order, order.quantity), new SplitOrder(order, 0)));
		match(order, order.quantity - unmatchedQuantity, orderUnmatchedHeap, orderMatchedHeap);
	}

//	public void withdrawOrder(Order<P, T> order) {
//		withdrawOrder(order, order.quantity);
//	}

	// FIXME First withdraw from unmatched portion, then withdraw from matched portion
	public void withdrawOrder(Order<P, T> order, int quantity) {
		int t = Integer.signum(order.quantity); // Sell or Buy
		BinaryHeap<SplitOrder> orderMatchedHeap, orderUnmatchedHeap, matchedHeap, unmatchedHeap;
		unmatchedHeap = t < 0 ? buyUnmatched : sellUnmatched;
		matchedHeap = t < 0 ? buyMatched : sellMatched;
		orderUnmatchedHeap = t > 0 ? buyUnmatched : sellUnmatched;
		orderMatchedHeap = t > 0 ? buyMatched : sellMatched;
		
		if (t*quantity > t*order.quantity || t*quantity < 0)
			throw new IllegalArgumentException("Can't withdraw more than in order");
		
		Pair<SplitOrder, SplitOrder> active = activeOrders.get(order);
		if (active == null) return; // Order not in fourheap. XXX Throw exception instead?
		order.withdraw(quantity);
		if (order.quantity == 0) activeOrders.remove(order);
		
		// Withdraw unmatched portion
		SplitOrder unmatched = active.left();
		orderUnmatchedHeap.remove(unmatched);
		int quantityMatched = t*Math.min(t*quantity, -t*unmatched.quantity);
		unmatched.quantity -= quantityMatched;
		quantity -= quantityMatched;
		if (unmatched.quantity != 0) orderUnmatchedHeap.offer(unmatched);
		
		SplitOrder matched = active.right();
		orderMatchedHeap.remove(matched);
		while (quantity > 0) {
			SplitOrder oMatched = matchedHeap.poll();
			quantityMatched = t*Math.min(t*quantity, -t*oMatched.quantity);
			Order<P, T> o = oMatched.order;
			SplitOrder oUnmatched = activeOrders.get(o).left();
			unmatchedHeap.remove(oUnmatched);
			oMatched.quantity += quantityMatched;
			oUnmatched.quantity -= quantityMatched;
			if (oMatched.quantity != 0) matchedHeap.offer(oMatched);
			if (oUnmatched.quantity != 0) unmatchedHeap.offer(oUnmatched);
			matched.quantity -= quantityMatched;
			quantity -= quantityMatched;
		}
		if (matched.quantity != 0) orderMatchedHeap.offer(matched);
	}

	protected void match(Order<P, T> order, int numMatched,
			BinaryHeap<SplitOrder> unmatchedHeap,
			BinaryHeap<SplitOrder> matchedHeap) {
		// Remove any existing split orders
		Pair<SplitOrder, SplitOrder> active = activeOrders.get(order);
		SplitOrder unmatched = active.left(), matched = active.right();
		
		unmatchedHeap.remove(unmatched);
		matchedHeap.remove(matched);
		
		// Add new split orders
		matched.quantity += numMatched;
		unmatched.quantity -= numMatched;
		
		if (matched.quantity != 0) matchedHeap.offer(matched);
		if (unmatched.quantity != 0) unmatchedHeap.offer(unmatched);
	}

	/**
	 * Returns the Price quote as a pair of Price types. The left element is the buy price, the
	 * right element is the sell price.
	 * 
	 * @return Pair(Buy Price, Sell Price)
	 */
	public Pair<P, P> quote() {
		P sin = sellMatched.peek() == null ? null : sellMatched.peek().price;
		P sout = sellUnmatched.peek() == null ? null : sellUnmatched.peek().price;
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
		protected int quantity;
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
