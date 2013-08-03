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

	// FIXME insert can displace an already matched bid
	public void insertOrder(Order<P, T> order) {
		if (order.quantity == 0)
			throw new IllegalArgumentException("Orders must have nonzero quantity");
		
		int t = Integer.signum(order.quantity); // Sell or Buy
		BinaryHeap<SplitOrder> unmatchedHeap = t < 0 ? buyUnmatched : sellUnmatched;
		
		int unmatchedQuantity = order.quantity;
		while (unmatchedQuantity * t > 0 && !unmatchedHeap.isEmpty()
				&& unmatchedHeap.peek().price.compareTo(order.price) * t <= 0) {
			
			SplitOrder match = unmatchedHeap.poll();
			int quantityMatched = t*Math.min(t*unmatchedQuantity, -t*match.quantity);
			moveOrders(match.order, -quantityMatched);
			unmatchedQuantity -= quantityMatched;
		}
		activeOrders.put(order, new Pair<SplitOrder, SplitOrder>(new SplitOrder(order, order.quantity), new SplitOrder(order, 0)));
		moveOrders(order, order.quantity - unmatchedQuantity);
	}

	public void withdrawOrder(Order<P, T> order) {
		withdrawOrder(order, order.quantity);
	}

	public void withdrawOrder(Order<P, T> order, int quantity) {
		int t = Integer.signum(order.quantity); // Sell or Buy
		if (t*quantity > t*order.quantity || t*quantity < 0)
			throw new IllegalArgumentException("Can't withdraw more than in order");
		
		BinaryHeap<SplitOrder> matchedHeap = t < 0 ? buyMatched : sellMatched;
		Pair<SplitOrder, SplitOrder> active = activeOrders.get(order);
		if (active == null) return; // Order not in fourheap
		
		// Modify order. This is done early for proper totalQuantity comparison on the fourheaps
		// when things are removed.
		order.withdraw(quantity);
		if (order.quantity == 0) activeOrders.remove(order);
		
		// Modify this order. First removes the quantity necessary to remove overall, then moves
		// appropriate amount from matchedHeap 
		SplitOrder unmatched = active.left();
		int qremove = t*Math.min(t*quantity, t*unmatched.quantity);
		unmatched.quantity -= quantity;
		quantity -= qremove;
		moveOrders(order, -qremove);
		
		// Remove any amount of matched orders
		while (quantity > 0) {
			SplitOrder oMatched = matchedHeap.poll();
			int quantityMatched = t*Math.min(t*quantity, -t*oMatched.quantity);
			moveOrders(oMatched.order, quantityMatched);
			quantity -= quantityMatched;
		}
		
	}

	/**
	 * @param order order to modify
	 * @param quantity to move from unmatched to matched
	 */
	protected void moveOrders(Order<P, T> order, int quantity) {
		BinaryHeap<SplitOrder> unmatchedHeap, matchedHeap;
		unmatchedHeap = order.totalQuantity < 0 ? sellUnmatched : buyUnmatched;
		matchedHeap = order.totalQuantity < 0 ? sellMatched : buyMatched;
		
		// Remove any existing split orders
		Pair<SplitOrder, SplitOrder> active = activeOrders.get(order);
		SplitOrder unmatched = active.left(), matched = active.right();
		
		unmatchedHeap.remove(unmatched);
		matchedHeap.remove(matched);
		matched.quantity += quantity;
		unmatched.quantity -= quantity;
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
