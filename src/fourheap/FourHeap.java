package fourheap;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * This class provides an efficient order matching mechanism while also
 * producing valid price quotes in constant time.
 * 
 * Produces undefined behavior (relative to the two order) if P and T for an
 * order are the same. To remove this behavior, ensure T (or P) always has a
 * strict ordering.
 * 
 * Note, remove is O(n).
 * 
 * @author ebrink
 * 
 * @param <P>
 *            Price class
 * @param <T>
 *            Time class
 */
/*
 * TODO Remove is slow, and changing to SortedSets will provide faster remove
 * for large heaps, but probably slow everything else down by a constant.
 * 
 * TODO There's a lot of almost duplicate code about modifying matched and
 * unmatched quantities, and then inserting and removing from heaps. There's got
 * to be a way to generalize it.
 * 
 */
public class FourHeap <P extends Comparable<? super P>, T extends Comparable<? super T>> implements Serializable {

	private static final long serialVersionUID = -7322375558427133915L;
	protected final Ordering<P> pord = Ordering.natural();
		
	protected final PriorityQueue<Order<P, T>> sellUnmatched, sellMatched, buyUnmatched, buyMatched;
	protected int size;

	protected FourHeap() {
		Ordering<Order<P, T>> priceComp = new PriceOrdering(), timeComp = new TimeOrdering();
		
		// Sout: unmatched sells, min first
		this.sellUnmatched = new PriorityQueue<Order<P, T>>(1, priceComp.compound(timeComp));
		// Sin: matched sells, max first
		this.sellMatched   = new PriorityQueue<Order<P, T>>(1, priceComp.reverse().compound(timeComp));
		// Bout: unmatched buys, max first
		this.buyUnmatched  = new PriorityQueue<Order<P, T>>(1, priceComp.reverse().compound(timeComp));
		// Bin: matched buys, min first
		this.buyMatched    = new PriorityQueue<Order<P, T>>(1, priceComp.compound(timeComp));
		this.size = 0;
	}
	
	/**
	 * Factory Method
	 */
	public static <P extends Comparable<? super P>, T extends Comparable<? super T>> FourHeap<P, T> create() {
		return new FourHeap<P, T>();
	}

	/**
	 * Inserts an order into the fourheap. Order should not be any other
	 * fourheaps, or already in this one.
	 * 
	 * Complexity: O(log n)
	 * 
	 * @param order
	 */
	public void insertOrder(Order<P, T> order) {
		checkArgument(order.unmatchedQuantity > 0, "Orders must have positive quantity");

		size += order.unmatchedQuantity;;
		int t;
		PriorityQueue<Order<P, T>> matchUnmatchedHeap, matchMatchedHeap, orderUnmatchedHeap, orderMatchedHeap;
		if (order.type == Order.OrderType.BUY) { // buy order
			orderUnmatchedHeap = buyUnmatched;
			orderMatchedHeap = buyMatched;
			matchUnmatchedHeap = sellUnmatched;
			matchMatchedHeap = sellMatched;
			t = 1;
		} else { // sell order
			orderUnmatchedHeap = sellUnmatched;
			orderMatchedHeap = sellMatched;
			matchUnmatchedHeap = buyUnmatched;
			matchMatchedHeap = buyMatched;
			t = -1;
		}

		// First match with unmatched orders
		while (order.unmatchedQuantity > 0 // Quantity left to match
				&& !matchUnmatchedHeap.isEmpty() // Orders to match with
				&& matchUnmatchedHeap.peek().price.compareTo(order.price) * t <= 0 // Can match with other order
				&& (orderMatchedHeap.isEmpty() || // Make sure it shouldn't kick out an order instead 
						matchUnmatchedHeap.peek().price.compareTo(orderMatchedHeap.peek().price) * t <= 0)) {

			Order<P, T> match = matchUnmatchedHeap.peek();
			if (match.matchedQuantity == 0) matchMatchedHeap.offer(match); // Will have nonzero matched after this
			
			int quantityMatched = Math.min(order.unmatchedQuantity, match.unmatchedQuantity);	//FIXME - will it still work if take out t here?
			order.unmatchedQuantity -= quantityMatched;
			order.matchedQuantity += quantityMatched;
			match.unmatchedQuantity -= quantityMatched;
			match.matchedQuantity += quantityMatched;
			
			if (match.unmatchedQuantity == 0) matchUnmatchedHeap.poll(); // lost all unmatched, needed to be removed
		}

		// Next displace inferior matched orders
		while (order.unmatchedQuantity > 0 // Quantity left to match
				&& !orderMatchedHeap.isEmpty() // Orders to displace
				&& orderMatchedHeap.comparator().compare(order, orderMatchedHeap.peek()) > 0) { // Should displace order

			Order<P, T> match = orderMatchedHeap.peek();
			if (match.unmatchedQuantity == 0) orderUnmatchedHeap.offer(match);
			
			int quantityMatched = Math.min(order.unmatchedQuantity, match.matchedQuantity);	// XXX take out t here?
			order.unmatchedQuantity -= quantityMatched;
			order.matchedQuantity += quantityMatched;
			match.unmatchedQuantity += quantityMatched;
			match.matchedQuantity -= quantityMatched;
			
			if (match.matchedQuantity == 0) orderMatchedHeap.poll();
		}

		// Put order in necessary heaps
		if (order.unmatchedQuantity != 0) orderUnmatchedHeap.offer(order);
		if (order.matchedQuantity != 0) orderMatchedHeap.offer(order);
	}

	/**
	 * Withdraws a specific order. It must be in the fourheap.
	 * 
	 * Complexity: O(n)
	 * 
	 * @param order
	 *            The order to withdraw
	 */
	public void withdrawOrder(Order<P, T> order) {
		withdrawOrder(order, order.getQuantity());
	}

	/**
	 * Withdraws a specific quantity from the fourheap. Behavior is undefined if
	 * the order isn't already in the fourheap.
	 * 
	 * Complexity: O(n)
	 * 
	 * @param order
	 *            The order to withdraw
	 * @param quantity
	 *            The quantity to withdraw from order. Must be positive, even
	 *            for sell orders.
	 */
	public void withdrawOrder(Order<P, T> order, int quantity) {
		checkArgument(quantity > 0, "Quantity must be positive");
		checkArgument(quantity <= order.getQuantity(), "Can't withdraw more than in order");

		size -= quantity;
		int t;
		PriorityQueue<Order<P, T>> matchUnmatchedHeap, matchMatchedHeap, orderUnmatchedHeap, orderMatchedHeap;
		if (order.type == Order.OrderType.BUY) { // buy order
			orderUnmatchedHeap = buyUnmatched;
			orderMatchedHeap = buyMatched;
			matchUnmatchedHeap = sellUnmatched;
			matchMatchedHeap = sellMatched;
			t = 1;
		} else { // sell order
			orderUnmatchedHeap = sellUnmatched;
			orderMatchedHeap = sellMatched;
			matchUnmatchedHeap = buyUnmatched;
			matchMatchedHeap = buyMatched;
			t = -1;
		}

		// First remove any unmatched orders (easy)
		if (order.unmatchedQuantity != 0) {
			int qremove = Math.min(quantity, order.unmatchedQuantity);
			order.unmatchedQuantity -= qremove;
			quantity -= qremove;
			if (order.unmatchedQuantity == 0) orderUnmatchedHeap.remove(order);
		}
		
		// Replace withdrawn quantity with viable orders from orderUnmatchedHeap
		while (quantity > 0 // More to remove
				&& !orderUnmatchedHeap.isEmpty() // Orders to replace
				&& orderUnmatchedHeap.peek().price.compareTo(matchMatchedHeap.peek().price) * t >= 0) { // Valid to match
			Order<P, T> match = orderUnmatchedHeap.peek();
			if (match.matchedQuantity == 0) orderMatchedHeap.offer(match);
			
			int quantityMatched = Math.min(quantity, match.unmatchedQuantity); // XXX take out t
			order.matchedQuantity -= quantityMatched;
			match.matchedQuantity += quantityMatched;
			match.unmatchedQuantity -= quantityMatched;
			quantity -= quantityMatched;  // XXX take out t
			
			if (match.unmatchedQuantity == 0) orderUnmatchedHeap.poll();
		}

		// Remove any amount of matched orders
		while (quantity > 0) {
			Order<P, T> match = matchMatchedHeap.peek();
			if (match.unmatchedQuantity == 0) matchUnmatchedHeap.offer(match);
			
			int quantityMatched = Math.min(quantity, match.matchedQuantity);	 // XXX take out t
			order.matchedQuantity -= quantityMatched;
			match.matchedQuantity -= quantityMatched;
			match.unmatchedQuantity += quantityMatched;
			quantity -= quantityMatched; // XXX take out t
			
			if (match.matchedQuantity == 0) matchMatchedHeap.poll();
		}

		if (order.matchedQuantity == 0) orderMatchedHeap.remove(order);
	}

	/**
	 * Clears matched orders from the fourheap, and returns a List of
	 * MatchedOrders, which contains the two matched orders, and the quantity
	 * matched by that order.
	 * 
	 * Complexity: O(m) where m is the number of matched orders
	 * 
	 * @return The MatchedOrders
	 */
	public List<MatchedOrders<P, T>> clear() {
		List<Order<P, T>> buys = Lists.newArrayList(buyMatched);
		Collections.sort(buys, buyUnmatched.comparator());
		List<Order<P, T>> sells = Lists.newArrayList(sellMatched);
		Collections.sort(sells, sellUnmatched.comparator());
		
		buyMatched.clear();
		sellMatched.clear();

		Order<P, T> buy = null, sell = null;
		Iterator<Order<P, T>> buyIt = buys.iterator();
		Iterator<Order<P, T>> sellIt = sells.iterator();
		
		Builder<MatchedOrders<P, T>> transactions = ImmutableList.builder();
		while (buyIt.hasNext() || sellIt.hasNext()) {
			if (buy == null || buy.matchedQuantity == 0) buy = buyIt.next();
			if (sell == null || sell.matchedQuantity == 0) sell = sellIt.next();
			
			int quantity = Math.min(buy.matchedQuantity, sell.matchedQuantity);
			buy.matchedQuantity -= quantity;
			sell.matchedQuantity -= quantity;
			transactions.add(MatchedOrders.create(buy, sell, quantity));
			size -= 2*quantity;
		}
		return transactions.build();
	}

	/**
	 * Check if an order is in the fourheap.
	 * 
	 * Complexity: O(n)
	 * 
	 * @param order
	 *            The order to check for containment
	 * @return True if in the fourheap
	 */
	public boolean contains(Order<P, T> order) {
		if (order.matchedQuantity > 0)
			return buyMatched.contains(order) || sellMatched.contains(order);
		else if (order.unmatchedQuantity > 0)
			return buyUnmatched.contains(order) || sellUnmatched.contains(order);
		else
			return false;
	}

	/**
	 * Returns the bid quote for the fourheap. A sell order with a price below
	 * this is guaranteed to get matched.
	 * 
	 * Complexity: O(1)
	 * 
	 * @return The price of the bid quote.
	 */
	public P bidQuote() {
		Order<P, T> sin = sellMatched.peek(), bout = buyUnmatched.peek();
		
		if (sin == null && bout == null) return null;
		else if (sin == null) return bout.price;
		else if (bout == null) return sin.price;
		else return pord.max(sin.price, bout.price);
	}

	/**
	 * Returns the ask quote for the fourheap. A buy order with a price above
	 * this is guaranteed to get matched.
	 * 
	 * Complexity: O(1)
	 * 
	 * @return The price of the bid quote.
	 */
	public P askQuote() {
		Order<P, T> sout = sellUnmatched.peek(), bin = buyMatched.peek();
		
		if (bin == null && sout == null) return null;
		else if (bin == null) return sout.price;
		else if (sout == null) return bin.price;
		else return pord.min(bin.price, sout.price);
	}
	
	/**
	 * The number of orders weighted by quantity in the fourheap.
	 * 
	 * Complexity: O(1)
	 * 
	 * @return total quantity of every order in the Fourheap
	 */
	public int size() {
		return size;
	}
	
	/**
	 * Return whether the fourheap is empty.
	 * 
	 * Complexity: O(1)
	 * 
	 * @return True if there are no orders in the fourheap
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	/**
	 * Complexity: O(n)
	 */
	public String toString() {
		return "<Bo: " + buyUnmatched + ", So: " + sellUnmatched + ", Bi: "
				+ buyMatched + ", Si: " + sellMatched + ">";
	}

	// These had to be declared separately so they could implements serializable
	
	/**
	 * Sorts an Order by its price.
	 * 
	 * @author ebrink
	 * 
	 */
	protected class PriceOrdering extends Ordering<Order<P, T>> implements Serializable {
		private static final long serialVersionUID = -6083048512440275282L;

		public int compare(Order<P, T> first, Order<P, T> second) {
			return first.price.compareTo(second.price);
		}
	}
	
	/**
	 * Sorts and Order by its time
	 * 
	 * @author ebrink
	 * 
	 */
	protected class TimeOrdering extends Ordering<Order<P, T>> implements Serializable {
		private static final long serialVersionUID = -5355682963794695579L;
		
		public int compare(Order<P, T> first, Order<P, T> second) {
			return first.submitTime.compareTo(second.submitTime);
		}
	}
}
