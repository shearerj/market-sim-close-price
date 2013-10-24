package fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

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
 * Produces undefined ordering if P and T for an order are the same. To remove this behavior, ensure T (or P) always has a strict ordering
 * 
 * Note about uniqueness of inserted elements, otherwise withdraw won't work appropriately. Change to binary heap?
 * 
 * @author ebrink
 *
 * @param <P>
 * @param <T>
 */
public class FourHeap<P extends Comparable<? super P>, T extends Comparable<? super T>> implements Serializable {

	private static final long serialVersionUID = -7322375558427133915L;
	protected final Ordering<P> pord = Ordering.natural();
	
	/*
	 * TODO Changing these to SortedMaps will give better remove performance
	 * while everything else remains, but the remove doesn't happen very often,
	 * and the priority queue may be more performant.
	 */
	protected final PriorityQueue<Order<P, T>> sellUnmatched, sellMatched,
			buyUnmatched, buyMatched;
	protected int size;

	protected final Ordering<Order<P, T>> priceComp = new PriceOrdering(),
			timeComp = new TimeOrdering();

	protected FourHeap() {
		sellUnmatched = new PriorityQueue<Order<P, T>>(1, priceComp.compound(timeComp));
		sellMatched   = new PriorityQueue<Order<P, T>>(1, priceComp.reverse().compound(timeComp));
		buyUnmatched  = new PriorityQueue<Order<P, T>>(1, priceComp.reverse().compound(timeComp));
		buyMatched    = new PriorityQueue<Order<P, T>>(1, priceComp.compound(timeComp));
		size = 0;
	}
	
	public static <P extends Comparable<? super P>, T extends Comparable<? super T>> FourHeap<P, T> create() {
		return new FourHeap<P, T>();
	}

	public void insertOrder(Order<P, T> order) {
		checkArgument(order.unmatchedQuantity != 0, "Orders must have nonzero quantity");

		size += abs(order.unmatchedQuantity);
		int t = Integer.signum(order.unmatchedQuantity); // Sell or Buy
		PriorityQueue<Order<P, T>> matchUnmatchedHeap = t > 0 ? sellUnmatched : buyUnmatched;
		PriorityQueue<Order<P, T>> matchMatchedHeap = t > 0 ? sellMatched : buyMatched;
		PriorityQueue<Order<P, T>> orderUnmatchedHeap = t > 0 ? buyUnmatched : sellUnmatched;
		PriorityQueue<Order<P, T>> orderMatchedHeap = t > 0 ? buyMatched : sellMatched;

		// First match with unmatched orders
		while (abs(order.unmatchedQuantity) > 0 // Quantity left to match
				&& !matchUnmatchedHeap.isEmpty() // Orders to match with
				&& matchUnmatchedHeap.peek().price.compareTo(order.price) * t <= 0 // Can match with other order
				&& (orderMatchedHeap.isEmpty() || // Make sure it shouldn't kick out an order instead 
						matchUnmatchedHeap.peek().price.compareTo(orderMatchedHeap.peek().price) * t <= 0)) {

			Order<P, T> match = matchUnmatchedHeap.peek();
			if (match.matchedQuantity == 0) matchMatchedHeap.offer(match); // Will have nonzero matched after this
			
			int quantityMatched = t * Math.min(abs(order.unmatchedQuantity), abs(match.unmatchedQuantity));
			order.unmatchedQuantity -= quantityMatched;
			order.matchedQuantity += quantityMatched;
			match.unmatchedQuantity -= -quantityMatched;
			match.matchedQuantity += -quantityMatched;
			
			if (match.unmatchedQuantity == 0) matchUnmatchedHeap.poll(); // lost all unmatched, needed to be removed
		}

		// Next displace inferior matched orders
		while (abs(order.unmatchedQuantity) > 0 // Quantity left to match
				&& !orderMatchedHeap.isEmpty() // Orders to displace
				&& orderMatchedHeap.comparator().compare(order, orderMatchedHeap.peek()) > 0) { // Should displace order

			Order<P, T> match = orderMatchedHeap.peek();
			if (match.unmatchedQuantity == 0) orderUnmatchedHeap.offer(match);
			
			int quantityMatched = t * Math.min(abs(order.unmatchedQuantity), abs(match.matchedQuantity));
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

	public void withdrawOrder(Order<P, T> order) {
		withdrawOrder(order, abs(order.getQuantity()));
	}

	/**
	 * 
	 * @param order
	 * @param quantity always positive
	 */
	public void withdrawOrder(Order<P, T> order, int quantity) {
		int t = Integer.signum(order.getQuantity()); // Sell or Buy
		checkArgument(quantity <= abs(order.getQuantity()) && quantity > 0,
				"Can't withdraw more than in order");
		checkArgument(contains(order), "Can't remove an order not in the FourHeap");

		size -= quantity;
		PriorityQueue<Order<P, T>> orderUnmatchedHeap = t > 0 ? buyUnmatched : sellUnmatched;
		PriorityQueue<Order<P, T>> orderMatchedHeap = t > 0 ? buyMatched : sellMatched;
		PriorityQueue<Order<P, T>> matchUnmatchedHeap = t > 0 ? sellUnmatched : buyUnmatched;
		PriorityQueue<Order<P, T>> matchMatchedHeap = t > 0 ? sellMatched : buyMatched;

		// First remove any unmatched orders (easy)
		if (order.unmatchedQuantity != 0) {
			int qremove = t * Math.min(quantity, abs(order.unmatchedQuantity));
			order.unmatchedQuantity -= qremove;
			quantity -= t * qremove;
			if (order.unmatchedQuantity == 0) orderUnmatchedHeap.remove(order);
		}

		// Remove any amount of matched orders
		while (quantity > 0) {
			Order<P, T> match = matchMatchedHeap.peek();
			if (match.unmatchedQuantity == 0) matchUnmatchedHeap.offer(match);
			
			int quantityMatched = t * Math.min(quantity, abs(match.matchedQuantity));
			order.matchedQuantity -= quantityMatched;
			match.matchedQuantity -= -quantityMatched;
			match.unmatchedQuantity += -quantityMatched;
			quantity -= t * quantityMatched;
			
			if (match.matchedQuantity == 0) matchMatchedHeap.poll();
		}

		if (order.matchedQuantity == 0) orderMatchedHeap.remove(order);
	}

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
			int quantity = Math.min(buy.matchedQuantity, -sell.matchedQuantity);
			transactions.add(buy.match(sell, quantity));
			size -= 2*quantity;
		}
		return transactions.build();
	}

	public boolean contains(Order<P, T> order) {
		if (order.unmatchedQuantity > 0) return buyUnmatched.contains(order);
		else if (order.unmatchedQuantity < 0) return sellUnmatched.contains(order);
		else if (order.matchedQuantity > 0) return buyMatched.contains(order);
		else if (order.matchedQuantity < 0) return sellMatched.contains(order);
		else return false;
	}

	public P bidQuote() {
		Order<P, T> sin = sellMatched.peek(), bout = buyUnmatched.peek();
		
		if (sin == null && bout == null) return null;
		else if (sin == null) return bout.price;
		else if (bout == null) return sin.price;
		else return pord.max(sin.price, bout.price);
	}
	
	public P askQuote() {
		Order<P, T> sout = sellUnmatched.peek(), bin = buyMatched.peek();
		
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

	@Override
	public String toString() {
		return "<Bo: " + buyUnmatched + ", So: " + sellUnmatched + ", Bi: "
				+ buyMatched + ", Si: " + sellMatched + ">";
	}

	// These had to be declared separately so they could implements serializable
	protected class PriceOrdering extends Ordering<Order<P, T>> implements Serializable {
		private static final long serialVersionUID = -6083048512440275282L;

		public int compare(Order<P, T> first, Order<P, T> second) {
			return first.price.compareTo(second.price);
		}
	}
	
	protected class TimeOrdering extends Ordering<Order<P, T>> implements Serializable {
		private static final long serialVersionUID = -5355682963794695579L;
		
		public int compare(Order<P, T> first, Order<P, T> second) {
			return first.submitTime.compareTo(second.submitTime);
		}
	}

}
