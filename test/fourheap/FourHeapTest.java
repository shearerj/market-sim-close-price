package fourheap;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;

import org.junit.Test;

public class FourHeapTest {
	
	protected final static Random rand = new Random();

	@Test
	public void heapOrderTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		
		FourHeap<Integer, Integer>.SplitOrder b1 = fh.new SplitOrder(Order.create(5, 3, 5));
		FourHeap<Integer, Integer>.SplitOrder b2 = fh.new SplitOrder(Order.create(10, 3, 5));
		FourHeap<Integer, Integer>.SplitOrder b3 = fh.new SplitOrder(Order.create(5, 3, 4));
		FourHeap<Integer, Integer>.SplitOrder b4 = fh.new SplitOrder(Order.create(5, 4, 5));
		
		fh.buyUnmatched.offer(b1);
		fh.buyUnmatched.offer(b2);
		assertEquals(fh.buyUnmatched.poll(), b2);
		fh.buyUnmatched.offer(b3);
		assertEquals(fh.buyUnmatched.poll(), b3);
		fh.buyUnmatched.offer(b4);
		assertEquals(fh.buyUnmatched.poll(), b4);
		
		fh.buyMatched.offer(b1);
		fh.buyMatched.offer(b2);
		assertEquals(fh.buyMatched.peek(), b1);
		fh.buyMatched.offer(b3);
		assertEquals(fh.buyMatched.poll(), b3);
		fh.buyMatched.offer(b4);
		assertEquals(fh.buyMatched.poll(), b4);
		
		FourHeap<Integer, Integer>.SplitOrder s1 = fh.new SplitOrder(Order.create(5, -3, 5));
		FourHeap<Integer, Integer>.SplitOrder s2 = fh.new SplitOrder(Order.create(10, -3, 5));
		FourHeap<Integer, Integer>.SplitOrder s3 = fh.new SplitOrder(Order.create(5, -3, 4));
		FourHeap<Integer, Integer>.SplitOrder s4 = fh.new SplitOrder(Order.create(5, -4, 5));
		
		fh.sellUnmatched.offer(s1);
		fh.sellUnmatched.offer(s2);
		assertEquals(fh.sellUnmatched.peek(), s1);
		fh.sellUnmatched.offer(s3);
		assertEquals(fh.sellUnmatched.poll(), s3);
		fh.sellUnmatched.offer(s4);
		assertEquals(fh.sellUnmatched.poll(), s4);
		
		fh.sellMatched.offer(s1);
		fh.sellMatched.offer(s2);
		assertEquals(fh.sellMatched.poll(), s2);
		fh.sellMatched.offer(s3);
		assertEquals(fh.sellMatched.poll(), s3);
		fh.sellMatched.offer(s4);
		assertEquals(fh.sellMatched.poll(), s4);
	}
	
	@Test
	public void insertOneBuyTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		insertOrder(fh, 5, 3, 0);
		
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
		insertOrder(fh, 5, -3, 0);
		
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
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(6, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.create();
		insertOrder(fh, 5, -5, 0);
		insertOrder(fh, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(8, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.create();
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 5, 1);
		
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
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 4, 1, 2);
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);
		
		fh = FourHeap.create();		
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 6, 1, 2);
		assertEquals((Integer) 6, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);

		fh = FourHeap.create();		
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 8, 1, 2);
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertInvariants(fh);
	}
	
	@Test
	public void withdrawOneBuyTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		Order<Integer, Integer> o = insertOrder(fh, 5, 3, 0);
		fh.withdrawOrder(o, 2);
		
		assertEquals(1, o.totalQuantity);
		assertEquals(1, o.quantity);
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
		Order<Integer, Integer> o = insertOrder(fh, 5, -3, 0);
		fh.withdrawOrder(o, -2);
		
		assertEquals(-1, o.totalQuantity);
		assertEquals(-1, o.quantity);
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
		os = insertOrder(fh, 5, -3, 0);
		ob = insertOrder(fh, 7, 3, 1);
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
		os = insertOrder(fh, 5, -5, 0);
		ob = insertOrder(fh, 7, 3, 1);
		fh.withdrawOrder(os, -3);
		
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
		os = insertOrder(fh, 5, -3, 0);
		ob = insertOrder(fh, 7, 5, 1);
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
	
	@Test
	public void emptyClearTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = FourHeap.create();
		insertOrder(fh, 7, -3, 0);
		insertOrder(fh, 5, 3, 1);
		assertTrue(fh.clear().isEmpty());
	}
	
	@Test
	public void clearTest() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> os, ob;
		List<Transaction<Integer, Integer>> transactions;
		Transaction<Integer, Integer> trans;
		
		fh = FourHeap.create();
		os = insertOrder(fh, 5, -2, 0);
		ob = insertOrder(fh, 7, 3, 1);
		transactions = fh.clear();
		
		assertEquals(1, transactions.size());
		trans = transactions.get(0);
		assertEquals(os, trans.getSell());
		assertEquals(ob, trans.getBuy());
		assertEquals(2, trans.getQuantity());
		assertEquals(1, ob.quantity);
		assertEquals(3, ob.totalQuantity);
		assertEquals(1, fh.size());
		assertFalse(fh.contains(os));
		assertTrue(fh.contains(ob));
		assertInvariants(fh);
	}
	
	@Test
	public void multiOrderClearTest() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> os, ob;
		List<Transaction<Integer, Integer>> transactions;
		Transaction<Integer, Integer> trans;
		
		fh = FourHeap.create();
		os = insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 6, -2, 0);
		ob = insertOrder(fh, 7, 4, 1);
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
		insertOrder(fh, 2, 1, 0);
		insertOrder(fh, 1, -1, 1);
		insertOrder(fh, 4, -1, 2);
		insertOrder(fh, 3, 1, 3);
		insertOrder(fh, 5, 1, 4);
		
		assertInvariants(fh);
		
		fh = FourHeap.create();
		insertOrder(fh, 4, -1, 0);
		insertOrder(fh, 5, 1, 1);
		insertOrder(fh, 2, 1, 2);
		insertOrder(fh, 3, -1, 3);
		insertOrder(fh, 1, -1, 4);
		
		assertInvariants(fh);
	}
	
	@Test
	public void quoteInvariantTest() {
		FourHeap<Integer, Integer> fh = FourHeap.create();
		for (int i = 0; i < 1000; i++) {
			insertOrder(fh, rand.nextInt(900000) + 100000,
					rand.nextBoolean() ? 1 : -1, i);
			assertInvariants(fh);
		}
		
	}
	
	@Test
	public void repeatedInvarianceTest() {
		for (int i = 0; i < 100; i++)
			quoteInvariantTest();
	}
	
	protected static Order<Integer, Integer> insertOrder(
			FourHeap<Integer, Integer> fh, int price, int quantity, int time) {
		Order<Integer, Integer> order = Order.create(price, quantity, time);
		fh.insertOrder(order);
		return order;
	}
	
	protected static int size(BinaryHeap<FourHeap<Integer, Integer>.SplitOrder> bh) {
		int size = 0;
		for (FourHeap<Integer, Integer>.SplitOrder so : bh)
			size += so.quantity;
		return size;
	}
	
	protected static void assertInvariants(FourHeap<Integer, Integer> fh) {
		FourHeap<Integer, Integer>.SplitOrder bi, bo, si, so;
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
		assertEquals(size(fh.sellMatched), -size(fh.buyMatched));
	}

}
