package fourheap;

import static fourheap.CompareUtils.max;
import static fourheap.CompareUtils.min;
import static java.lang.Integer.signum;
import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FourHeap<P extends Comparable<P>, T extends Comparable<T>> {

	protected final BinaryHeap<SplitOrder> sellUnmatched, sellMatched,
			buyUnmatched, buyMatched;
	// Left is unmatched, Right is matched Should have a split order of 0 if nothing exists in
	// either unmatched or matched. if a split order has quantity 0, it shouldn't be in its
	// respective heap.
	protected final Map<Order<P, T>, Pair<SplitOrder, SplitOrder>> activeOrders;
	protected int size;

	public FourHeap() {
		sellUnmatched = new BinaryHeap<SplitOrder>(new OrderComparator(true, true));
		sellMatched = new BinaryHeap<SplitOrder>(new OrderComparator(false, true));
		buyUnmatched = new BinaryHeap<SplitOrder>(new OrderComparator(false, false));
		buyMatched = new BinaryHeap<SplitOrder>(new OrderComparator(true, false));
		activeOrders = new HashMap<Order<P, T>, Pair<SplitOrder, SplitOrder>>();
		size = 0;
	}

	// FIXME insert can displace an already matched bid
	public void insertOrder(Order<P, T> order) {
		if (order.quantity == 0)
			throw new IllegalArgumentException(
					"Orders must have nonzero quantity");

		size += abs(order.quantity);
		int t = Integer.signum(order.quantity); // Sell or Buy
		BinaryHeap<SplitOrder> unmatchedHeap = t < 0 ? buyUnmatched : sellUnmatched;
		BinaryHeap<SplitOrder> matchedHeap = t > 0 ? buyMatched : sellMatched;
		int unmatchedQuantity = order.quantity;

		// First match with unmatched orders
		while (unmatchedQuantity * t > 0 && !unmatchedHeap.isEmpty()
				&& unmatchedHeap.peek().price.compareTo(order.price) * t <= 0) {

			SplitOrder match = unmatchedHeap.poll();
			int quantityMatched = t
					* Math.min(t * unmatchedQuantity, -t * match.quantity);
			moveOrders(match.order, -quantityMatched, t > 0);
			unmatchedQuantity -= quantityMatched;
		}

		// Next displace inferior matched orders
		SplitOrder test = new SplitOrder(order); // test comparison
		while (unmatchedQuantity * t > 0 && !matchedHeap.isEmpty()
				&& matchedHeap.comp.compare(test, matchedHeap.peek()) > 0) {

			SplitOrder match = matchedHeap.poll();
			int quantityMatched = t
					* Math.min(t * unmatchedQuantity, t * match.quantity);
			moveOrders(match.order, -quantityMatched, t < 0);
			unmatchedQuantity -= quantityMatched;
		}

		// Put the remainder in the unmatched heap
		activeOrders.put(order,
				new Pair<SplitOrder, SplitOrder>(new SplitOrder(order,
						order.quantity), new SplitOrder(order, 0)));
		moveOrders(order, order.quantity - unmatchedQuantity, t < 0);
	}

	public void withdrawOrder(Order<P, T> order) {
		withdrawOrder(order, order.quantity);
	}

	public void withdrawOrder(Order<P, T> order, int quantity) {
		int t = Integer.signum(order.quantity); // Sell or Buy
		if (t * quantity > t * order.quantity || t * quantity < 0)
			throw new IllegalArgumentException(
					"Can't withdraw more than in order");

		size -= abs(quantity);
		BinaryHeap<SplitOrder> matchedHeap = t < 0 ? buyMatched : sellMatched;
		Pair<SplitOrder, SplitOrder> active = activeOrders.get(order);
		if (active == null) return; // Order not in fourheap

		// Modify order. This is done early for proper totalQuantity comparison on the fourheaps
		// when things are removed.
		order.withdraw(quantity);

		// Modify this order. First removes the quantity necessary to remove overall, then moves
		// appropriate amount from matchedHeap
		SplitOrder unmatched = active.left();
		int qremove = t * Math.min(t * quantity, t * unmatched.quantity);
		unmatched.quantity -= quantity;
		quantity -= qremove;
		moveOrders(order, -quantity, t < 0);

		// Remove any amount of matched orders
		while (t * quantity > 0) {
			SplitOrder oMatched = matchedHeap.poll();
			int quantityMatched = t
					* Math.min(t * quantity, -t * oMatched.quantity);
			moveOrders(oMatched.order, quantityMatched, t > 0);
			quantity -= quantityMatched;
		}

		if (order.quantity == 0) activeOrders.remove(order);
	}

	public List<Transaction<P, T>> clear() {
		List<SplitOrder> buys = new ArrayList<SplitOrder>(buyMatched);
		List<SplitOrder> sells = new ArrayList<SplitOrder>(sellMatched);
		buyMatched.clear();
		sellMatched.clear();

		Collections.sort(buys, buyUnmatched.comp);
		Collections.sort(sells, sellUnmatched.comp);
		List<Order<P, T>> buyOrders = new ArrayList<Order<P, T>>(buys.size());
		for (SplitOrder s : buys) buyOrders.add(s.order);
		List<Order<P, T>> sellOrders = new ArrayList<Order<P, T>>(sells.size());
		for (SplitOrder s : sells) sellOrders.add(s.order);

		SplitOrder buy = null, sell = null;
		Iterator<SplitOrder> buyIt = buys.iterator();
		Iterator<SplitOrder> sellIt = sells.iterator();
		
		List<Transaction<P, T>> transactions = new ArrayList<Transaction<P, T>>();
		while (buyIt.hasNext() || sellIt.hasNext()) {
			if (buy == null || buy.quantity == 0) buy = buyIt.next();
			if (sell == null || sell.quantity == 0) sell = sellIt.next();
			int quantity = Math.min(buy.quantity, -sell.quantity);
			transactions.add(buy.order.transact(sell.order, quantity));
			
			size -= 2*quantity;
			buy.quantity -= quantity;
			sell.quantity += quantity;
			if (buy.order.quantity == 0) activeOrders.remove(buy.order);
			if (sell.order.quantity == 0) activeOrders.remove(sell.order);
		}
		return transactions;
	}

	public boolean contains(Order<P, T> order) {
		return activeOrders.containsKey(order);
	}

	public P bidQuote() {
		P sin = sellMatched.peek() == null ? null : sellMatched.peek().price;
		P bout = buyUnmatched.peek() == null ? null : buyUnmatched.peek().price;
		return max(sin, bout);
	}
	
	public P askQuote() {
		P sout = sellUnmatched.peek() == null ? null : sellUnmatched.peek().price;
		P bin = buyMatched.peek() == null ? null : buyMatched.peek().price;
		return min(sout, bin);
	}
	
	/**
	 * @return total quantity of every order in the Fourheap
	 */
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return buyUnmatched.isEmpty() && sellUnmatched.isEmpty()
				&& buyMatched.isEmpty() && sellMatched.isEmpty();
	}

	/**
	 * @param order order to modify
	 * @param quantity quantity to move from unmatched to matched
	 * @param sell if this is a sell order (in case quantity is 0)
	 */
	protected void moveOrders(Order<P, T> order, int quantity, boolean sell) {
		// Pass in sell instead of order.totalQuantity in case order.quantity == 0 and can't be used
		// to determine buying or selling.
		BinaryHeap<SplitOrder> unmatchedHeap, matchedHeap;
		unmatchedHeap = sell ? sellUnmatched : buyUnmatched;
		matchedHeap = sell ? sellMatched : buyMatched;
	
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

	@Override
	public String toString() {
		return "<Bo: " + buyUnmatched + ", So: " + sellUnmatched + ", Bi: "
				+ buyMatched + ", Si: " + sellMatched + ">";
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

	/**
	 * Compares price, then time, then quantity
	 */
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
					* signum(priceComp.compare(o1.price, o2.price))
					+ 2
					* signum(o1.time.compareTo(o2.time))
					+ signum(quantComp.compare(o1.order.totalQuantity,
							o2.order.totalQuantity));
		}
	}

}
