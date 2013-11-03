package fourheap;

import static org.junit.Assert.*;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import org.junit.Test;

import fourheap.Order.OrderType;

public class FourHeapTest {
	
	protected final static Random rand = new Random();
	
	@Test
	public void heapOrderTest() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> b1, b2, b3, s1, s2, s3;
		
		fh = FourHeap.create();
		
		b1 = Order.create(OrderType.BUY, 5, 3, 5);
		b2 = Order.create(OrderType.BUY, 10, 3, 5);
		b3 = Order.create(OrderType.BUY, 5, 3, 4);
		
		fh.buyUnmatched.offer(b1);
		fh.buyUnmatched.offer(b2);
		assertEquals(b2, fh.buyUnmatched.poll());
		fh.buyUnmatched.offer(b3);
		assertEquals(b3, fh.buyUnmatched.poll());
		
		fh.buyMatched.offer(b1);
		fh.buyMatched.offer(b2);
		assertEquals(fh.buyMatched.peek(), b1);
		fh.buyMatched.offer(b3);
		assertEquals(fh.buyMatched.poll(), b3);
		
		s1 = Order.create(OrderType.SELL, 5, 3, 5);
		s2 = Order.create(OrderType.SELL, 10, 3, 5);
		s3 = Order.create(OrderType.SELL, 5, 3, 4);
		
		fh.sellUnmatched.offer(s1);
		fh.sellUnmatched.offer(s2);
		assertEquals(fh.sellUnmatched.peek(), s1);
		fh.sellUnmatched.offer(s3);
		assertEquals(fh.sellUnmatched.poll(), s3);
		
		fh.sellMatched.offer(s1);
		fh.sellMatched.offer(s2);
		assertEquals(fh.sellMatched.poll(), s2);
		fh.sellMatched.offer(s3);
		assertEquals(fh.sellMatched.poll(), s3);
	}
	
	@Test
	public void insertOneBuyTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.BUY, 5, 3, 0);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(3, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void insertOneSellTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 5, 3, 0);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(3, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void matchTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 5, 3, 0);
		insertOrder(fh, OrderType.BUY, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(6, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 5, 5, 0);
		insertOrder(fh, OrderType.BUY, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(8, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 5, 3, 0);
		insertOrder(fh, OrderType.BUY, 7, 5, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(8, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void insertMatchedTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 5, 3, 0);
		insertOrder(fh, OrderType.BUY, 7, 3, 1);
		insertOrder(fh, OrderType.BUY, 4, 1, 2);
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 5, 3, 0);
		insertOrder(fh, OrderType.BUY, 7, 3, 1);
		insertOrder(fh, OrderType.BUY, 6, 1, 2);
		assertEquals((Integer) 6, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);

		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 5, 3, 0);
		insertOrder(fh, OrderType.BUY, 7, 3, 1);
		insertOrder(fh, OrderType.BUY, 8, 1, 2);
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void withdrawOneBuyTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		Order<Integer, Integer> o = insertOrder(fh, OrderType.BUY, 5, 3, 0);
		fh.withdrawOrder(o, 2);
		
		assertEquals(1, o.unmatchedQuantity);
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(1, fh.size());
		assertInvariants(fh);
		
		fh.withdrawOrder(o);
		
		assertFalse(fh.contains(o));
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(0, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void withdrawOneSellTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		Order<Integer, Integer> o = insertOrder(fh, OrderType.SELL, 5, 3, 0);
		fh.withdrawOrder(o, 2);
		
		assertEquals(1, o.unmatchedQuantity);
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(1, fh.size());
		assertInvariants(fh);
		
		fh.withdrawOrder(o);
		
		assertFalse(fh.contains(o));
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(0, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void withdrawMatchTest() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> os, ob;
		
		fh = FourHeap.create();
		os = insertOrder(fh, OrderType.SELL, 5, 3, 0);
		ob = insertOrder(fh, OrderType.BUY, 7, 3, 1);
		fh.withdrawOrder(ob, 2);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(4, fh.size());
		assertInvariants(fh);
		
		fh.withdrawOrder(os);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(1, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.create();
		ob = insertOrder(fh, OrderType.BUY, 7, 3, 1);
		os = insertOrder(fh, OrderType.SELL, 5, 5, 0);
		fh.withdrawOrder(os, 3);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(5, fh.size());
		assertInvariants(fh);
		
		fh.withdrawOrder(ob);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(2, fh.size());
		assertInvariants(fh);

		fh = FourHeap.create();
		os = insertOrder(fh, OrderType.SELL, 5, 3, 0);
		ob = insertOrder(fh, OrderType.BUY, 7, 5, 1);
		fh.withdrawOrder(ob, 4);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(4, fh.size());
		assertInvariants(fh);
	}
	
	/**
	 * Test that withdrawing with orders waiting to get matched actually works
	 * appropriately
	 */
	@Test
	public void withdrawWithWaitingOrders() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> o;

		fh = FourHeap.create();
		o = insertOrder(fh, OrderType.BUY, 4, 3, 0);
		insertOrder(fh, OrderType.SELL, 1, 3, 1);
		insertOrder(fh, OrderType.SELL, 2, 2, 2);
		insertOrder(fh, OrderType.BUY, 3, 4, 3);
		assertInvariants(fh);
		fh.withdrawOrder(o);
		assertInvariants(fh);
		fh.clear();
		assertInvariants(fh);

		fh = FourHeap.create();
		o = insertOrder(fh, OrderType.SELL, 1, 3, 0);
		insertOrder(fh, OrderType.BUY, 4, 3, 1);
		insertOrder(fh, OrderType.BUY, 3, 2, 2);
		insertOrder(fh, OrderType.SELL, 2, 4, 3);
		assertInvariants(fh);
		fh.withdrawOrder(o);
		assertInvariants(fh);
		fh.clear();
		assertInvariants(fh);
	}
	
	/**
	 * Test a strange edge case with withdrawing orders, where quantity may get
	 * misinterpreted.
	 */
	@Test
	public void strangeWithdrawEdgeCase() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> o;

		fh = FourHeap.create();
		insertOrder(fh, OrderType.BUY, 4, 3, 0);
		o = insertOrder(fh, OrderType.SELL, 1, 3, 1);
		insertOrder(fh, OrderType.SELL, 2, 2, 2);
		insertOrder(fh, OrderType.BUY, 3, 4, 3);
		assertInvariants(fh);
		fh.withdrawOrder(o);
		assertInvariants(fh);
		fh.clear();
		assertInvariants(fh);

		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 1, 3, 0);
		o = insertOrder(fh, OrderType.BUY, 4, 3, 1);
		insertOrder(fh, OrderType.BUY, 3, 2, 2);
		insertOrder(fh, OrderType.SELL, 2, 4, 3);
		assertInvariants(fh);
		fh.withdrawOrder(o);
		assertInvariants(fh);
		fh.clear();
		assertInvariants(fh);
	}
	
	@Test
	public void emptyClearTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 7, 3, 0);
		insertOrder(fh, OrderType.BUY, 5, 3, 1);
		assertTrue(fh.clear().isEmpty());
	}
	
	@Test
	public void clearTest() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> os, ob;
		List<MatchedOrders<Integer, Integer>> transactions;
		MatchedOrders<Integer, Integer> trans;
		
		fh = FourHeap.create();
		os = insertOrder(fh, OrderType.SELL, 5, 2, 0);
		ob = insertOrder(fh, OrderType.BUY, 7, 3, 1);
		transactions = fh.clear();
		
		assertEquals(1, transactions.size());
		trans = transactions.get(0);
		assertEquals(os, trans.getSell());
		assertEquals(ob, trans.getBuy());
		assertEquals(2, trans.getQuantity());
		assertEquals(1, ob.unmatchedQuantity);
		assertEquals(1, fh.size());
		assertFalse(fh.contains(os));
		assertTrue(fh.contains(ob));
		assertInvariants(fh);
	}
	
	@Test
	public void multiOrderClearTest() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> os, ob;
		List<MatchedOrders<Integer, Integer>> transactions;
		MatchedOrders<Integer, Integer> trans;
		
		fh = FourHeap.create();
		os = insertOrder(fh, OrderType.SELL, 5, 3, 0);
		insertOrder(fh, OrderType.SELL, 6, 2, 0);
		ob = insertOrder(fh, OrderType.BUY, 7, 4, 1);
		transactions = fh.clear();
		
		assertEquals(2, transactions.size());
		trans = transactions.get(0);
		assertEquals(os, trans.getSell());
		assertEquals(ob, trans.getBuy());
		assertEquals(3, trans.getQuantity());
		trans = transactions.get(1);
		assertEquals(ob, trans.getBuy());
		assertEquals(1, trans.getQuantity());
		assertEquals(1, fh.size());
		assertFalse(fh.contains(os));
		assertFalse(fh.contains(ob));
		assertInvariants(fh);
	}
	
	@Test
	public void specificInvariantTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.BUY, 2, 1, 0);
		insertOrder(fh, OrderType.SELL, 1, 1, 1);
		insertOrder(fh, OrderType.SELL, 4, 1, 2);
		insertOrder(fh, OrderType.BUY, 3, 1, 3);
		insertOrder(fh, OrderType.BUY, 5, 1, 4);
		
		assertInvariants(fh);
		
		fh = FourHeap.create();
		insertOrder(fh, OrderType.SELL, 4, 1, 0);
		insertOrder(fh, OrderType.BUY, 5, 1, 1);
		insertOrder(fh, OrderType.BUY, 2, 1, 2);
		insertOrder(fh, OrderType.SELL, 3, 1, 3);
		insertOrder(fh, OrderType.SELL, 1, 1, 4);
		
		assertInvariants(fh);
	}
	
	@Test
	public void quoteInvariantTest() {
		FourHeap<Integer, Integer> fh = FourHeap.create();
		for (int i = 0; i < 1000; i++) {
			insertOrder(fh, rand.nextBoolean() ? OrderType.BUY : OrderType.SELL,
					rand.nextInt(900000) + 100000, 1, i);
			assertInvariants(fh);
		}
		
	}
	
	@Test
	public void repeatedInvarianceTest() {
		for (int i = 0; i < 100; i++)
			quoteInvariantTest();
	}
	
	protected static Order<Integer, Integer> insertOrder(
			FourHeap<Integer, Integer> fh, OrderType type, int price, int quantity, int time) {
		Order<Integer, Integer> order = Order.create(type, price, quantity, time);
		fh.insertOrder(order);
		return order;
	}
	
	protected static int matchedSize(PriorityQueue<Order<Integer, Integer>> bh) {
		int size = 0;
		for (Order<Integer, Integer> so : bh)
			size += so.matchedQuantity;
		return size;
	}
	
	protected static void assertInvariants(FourHeap<Integer, Integer> fh) {
		Order<Integer, Integer> bi, bo, si, so;
		Integer bid, ask;
		
		bi = fh.buyMatched.peek();
		bo = fh.buyUnmatched.peek();
		si = fh.sellMatched.peek();
		so = fh.sellUnmatched.peek();
		bid = fh.bidQuote();
		ask = fh.askQuote();
		
		assertTrue(bi == null || bo == null || bi.price >= bo.price);
		assertTrue(so == null || si == null || so.price >= si.price);
		assertTrue(so == null || bo == null || so.price >= bo.price);
		assertTrue(bi == null || si == null || bi.price >= si.price);
		assertTrue(bid == null || ask == null || bid <= ask);
		assertEquals(matchedSize(fh.sellMatched), matchedSize(fh.buyMatched));
	}

}
