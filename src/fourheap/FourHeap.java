package fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.primitives.Ints;

public class FourHeap<P extends Comparable<? super P>, T extends Comparable<? super T>> implements Serializable {

	private static final long serialVersionUID = -7322375558427133915L;
	protected final Ordering<P> pord = Ordering.natural();
	
	protected final BinaryHeap<SplitOrder> sellUnmatched, sellMatched,
			buyUnmatched, buyMatched;
	// Left is unmatched, Right is matched Should have a split order of 0 if nothing exists in
	// either unmatched or matched. if a split order has quantity 0, it shouldn't be in its
	// respective heap.
	protected final Map<Order<P, T>, OrderRecord> activeOrders;
	protected int size;

	protected FourHeap() {
		sellUnmatched = BinaryHeap.create(priceComp.compound(timeComp).compound(quantComp));
		sellMatched = BinaryHeap.create(priceComp.reverse().compound(timeComp).compound(quantComp));
		buyUnmatched = BinaryHeap.create(priceComp.reverse().compound(timeComp).compound(quantComp.reverse()));
		buyMatched = BinaryHeap.create(priceComp.compound(timeComp).compound(quantComp.reverse()));
		activeOrders = Maps.newHashMap();
		size = 0;
	}
	
	public static <P extends Comparable<? super P>, T extends Comparable<? super T>> FourHeap<P, T> create() {
		return new FourHeap<P, T>();
	}

	public void insertOrder(Order<P, T> order) {
		checkArgument(order.quantity != 0, "Orders must have nonzero quantity");

		size += abs(order.quantity);
		int t = Integer.signum(order.quantity); // Sell or Buy
		BinaryHeap<SplitOrder> unmatchedHeap = t < 0 ? buyUnmatched : sellUnmatched;
		BinaryHeap<SplitOrder> matchedHeap = t > 0 ? buyMatched : sellMatched;
		int unmatchedQuantity = order.quantity;

		// First match with unmatched orders
		while (abs(unmatchedQuantity) > 0
				&& !unmatchedHeap.isEmpty()
				&& unmatchedHeap.peek().price.compareTo(order.price) * t <= 0
				&& (matchedHeap.isEmpty() || unmatchedHeap.peek().price.compareTo(matchedHeap.peek().price)
						* t <= 0)) {

			SplitOrder match = unmatchedHeap.poll();
			int quantityMatched = t
					* Math.min(abs(unmatchedQuantity), abs(match.quantity));
			moveOrders(match.order, -quantityMatched, t > 0);
			unmatchedQuantity -= quantityMatched;
		}

		// Next displace inferior matched orders
		SplitOrder test = new SplitOrder(order); // test comparison
		while (abs(unmatchedQuantity) > 0 && !matchedHeap.isEmpty()
				&& matchedHeap.ordering.compare(test, matchedHeap.peek()) > 0) {

			SplitOrder match = matchedHeap.poll();
			int quantityMatched = t
					* Math.min(abs(unmatchedQuantity), abs(match.quantity));
			moveOrders(match.order, -quantityMatched, t < 0);
			unmatchedQuantity -= quantityMatched;
		}

		// Put the remainder in the unmatched heap
		activeOrders.put(order, new OrderRecord(order));
		moveOrders(order, order.quantity - unmatchedQuantity, t < 0);
	}

	public void withdrawOrder(Order<P, T> order) {
		withdrawOrder(order, order.quantity);
	}

	public void withdrawOrder(Order<P, T> order, int quantity) {
		int t = Integer.signum(order.quantity); // Sell or Buy
		checkArgument(abs(quantity) <= abs(order.quantity) && t * quantity > 0,
				"Can't withdraw more than in order");

		size -= abs(quantity);
		BinaryHeap<SplitOrder> matchedHeap = t < 0 ? buyMatched : sellMatched;
		OrderRecord active = activeOrders.get(order);
		if (active == null) return; // Order not in fourheap

		// Modify order. This is done early for proper totalQuantity comparison on the fourheaps
		// when things are removed.
		order.withdraw(quantity);

		// Modify this order. First removes the quantity necessary to remove overall, then moves
		// appropriate amount from matchedHeap
		SplitOrder unmatched = active.unmatched;
		int qremove = t * Math.min(abs(quantity), abs(unmatched.quantity));
		unmatched.quantity -= quantity;
		quantity -= qremove;
		moveOrders(order, -quantity, t < 0);

		// Remove any amount of matched orders
		while (abs(quantity) > 0) {
			SplitOrder oMatched = matchedHeap.poll();
			int quantityMatched = t
					* Math.min(abs(quantity), abs(oMatched.quantity));
			moveOrders(oMatched.order, quantityMatched, t > 0);
			quantity -= quantityMatched;
		}

		if (order.quantity == 0) activeOrders.remove(order);
	}

	public List<Transaction<P, T>> clear() {
		List<SplitOrder> buys = buyUnmatched.ordering.sortedCopy(buyMatched);
		List<SplitOrder> sells = sellUnmatched.ordering.sortedCopy(sellMatched);
		buyMatched.clear();
		sellMatched.clear();

		SplitOrder buy = new SplitOrder(), sell = new SplitOrder();
		Iterator<SplitOrder> buyIt = buys.iterator();
		Iterator<SplitOrder> sellIt = sells.iterator();
		
		Builder<Transaction<P, T>> transactions = ImmutableList.builder();
		while (buyIt.hasNext() || sellIt.hasNext()) {
			if (buy.quantity == 0) buy = buyIt.next();
			if (sell.quantity == 0) sell = sellIt.next();
			int quantity = Math.min(buy.quantity, -sell.quantity);
			transactions.add(buy.order.transact(sell.order, quantity));
			
			size -= 2*quantity;
			buy.quantity -= quantity;
			sell.quantity += quantity;
			if (buy.order.quantity == 0) activeOrders.remove(buy.order);
			if (sell.order.quantity == 0) activeOrders.remove(sell.order);
		}
		return transactions.build();
	}

	public boolean contains(Order<P, T> order) {
		return activeOrders.containsKey(order);
	}

	public P bidQuote() {
		SplitOrder sin = sellMatched.peek(), bout = buyUnmatched.peek();
		
		if (sin == null && bout == null) return null;
		else if (sin == null) return bout.price;
		else if (bout == null) return sin.price;
		else return pord.max(sin.price, bout.price);
	}
	
	public P askQuote() {
		SplitOrder sout = sellUnmatched.peek(), bin = buyMatched.peek();
		
		if (bin == null && sout == null) return null;
		else if (bin == null) return sout.price;
		else if (sout == null) return bin.price;
		else return pord.min(bin.price, sout.price);
	}
	
	/**
	 * @return total quantity of every order in the Fourheap
	 */
	public int size() {
		return size;
	}
	
	public boolean isEmpty() {
		return size == 0;
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
		OrderRecord active = activeOrders.get(order);
		SplitOrder unmatched = active.unmatched, matched = active.matched;
	
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

	protected class SplitOrder implements Serializable {
		private static final long serialVersionUID = 8502206148661366958L;
		
		protected final P price;
		protected int quantity;
		protected final T time;
		protected final Order<P, T> order;
		
		protected SplitOrder() { // Only intended for use in clear
			this.price = null;
			this.quantity = 0;
			this.time = null;
			this.order = null;
		}

		protected SplitOrder(Order<P, T> backedOrder) {
			this(backedOrder, backedOrder.quantity);
		}

		protected SplitOrder(Order<P, T> backedOrder, int splitQuantity) {
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
	
	protected class OrderRecord implements Serializable {
		private static final long serialVersionUID = 342977618049223128L;
		
		protected final SplitOrder unmatched, matched;
		
		protected OrderRecord(Order<P, T> order) {
			unmatched = new SplitOrder(order);
			matched = new SplitOrder(order, 0);
		}
		
	}
	
	protected final Ordering<SplitOrder> priceComp = new Ordering<SplitOrder>() {
		public int compare(SplitOrder first, SplitOrder second) {
			return first.price.compareTo(second.price);
		}
	};
	protected final Ordering<SplitOrder> timeComp = new Ordering<SplitOrder>() {
		public int compare(SplitOrder first, SplitOrder second) {
			return first.time.compareTo(second.time);
		}
	};
	protected final Ordering<SplitOrder> quantComp = new Ordering<SplitOrder>() {
		public int compare(SplitOrder first, SplitOrder second) {
			return Ints.compare(first.quantity, second.quantity);
		}
	};

}
