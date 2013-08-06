package fourheap;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class FourHeapTest {

	@Test
	public void heapOrderTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = new FourHeap<Integer, Integer>();
		
		FourHeap<Integer, Integer>.SplitOrder b1 = fh.new SplitOrder(new Order<Integer, Integer>(5, 3, 5));
		FourHeap<Integer, Integer>.SplitOrder b2 = fh.new SplitOrder(new Order<Integer, Integer>(10, 3, 5));
		FourHeap<Integer, Integer>.SplitOrder b3 = fh.new SplitOrder(new Order<Integer, Integer>(5, 3, 4));
		FourHeap<Integer, Integer>.SplitOrder b4 = fh.new SplitOrder(new Order<Integer, Integer>(5, 4, 5));
		
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
		
		FourHeap<Integer, Integer>.SplitOrder s1 = fh.new SplitOrder(new Order<Integer, Integer>(5, -3, 5));
		FourHeap<Integer, Integer>.SplitOrder s2 = fh.new SplitOrder(new Order<Integer, Integer>(10, -3, 5));
		FourHeap<Integer, Integer>.SplitOrder s3 = fh.new SplitOrder(new Order<Integer, Integer>(5, -3, 4));
		FourHeap<Integer, Integer>.SplitOrder s4 = fh.new SplitOrder(new Order<Integer, Integer>(5, -4, 5));
		
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
		
		fh = new FourHeap<Integer, Integer>();
		insertOrder(fh, 5, 3, 0);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(3, fh.size());
		assertConsistency(fh);
	}
	
	@Test
	public void insertOneSellTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = new FourHeap<Integer, Integer>();
		insertOrder(fh, 5, -3, 0);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(3, fh.size());
		assertConsistency(fh);
	}
	
	@Test
	public void matchTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = new FourHeap<Integer, Integer>();
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(6, fh.size());
		assertConsistency(fh);
		
		fh = new FourHeap<Integer, Integer>();
		insertOrder(fh, 5, -5, 0);
		insertOrder(fh, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(8, fh.size());
		assertConsistency(fh);
		
		fh = new FourHeap<Integer, Integer>();
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 5, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(8, fh.size());
		assertConsistency(fh);
	}
	
	@Test
	public void insertMatchedTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = new FourHeap<Integer, Integer>();		
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 4, 1, 2);
		assertEquals((Integer) 5, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertConsistency(fh);
		
		fh = new FourHeap<Integer, Integer>();		
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 6, 1, 2);
		assertEquals((Integer) 6, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertConsistency(fh);

		fh = new FourHeap<Integer, Integer>();		
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 8, 1, 2);
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals((Integer) 7, fh.askQuote());
		assertEquals(7, fh.size());
		assertConsistency(fh);
	}
	
	@Test
	public void withdrawOneBuyTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = new FourHeap<Integer, Integer>();
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
		assertConsistency(fh);
		
		fh.withdrawOrder(o);
		
		assertFalse(fh.contains(o));
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(0, fh.size());
		assertConsistency(fh);
	}
	
	@Test
	public void withdrawOneSellTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = new FourHeap<Integer, Integer>();
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
		assertConsistency(fh);
		
		fh.withdrawOrder(o);
		
		assertFalse(fh.contains(o));
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(0, fh.size());
		assertConsistency(fh);
	}
	
	@Test
	public void withdrawMatchTest() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> os, ob;
		
		fh = new FourHeap<Integer, Integer>();
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
		assertConsistency(fh);
		
		fh.withdrawOrder(os);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals((Integer) 7, fh.bidQuote());
		assertEquals(null, fh.askQuote());
		assertEquals(1, fh.size());
		assertConsistency(fh);
		
		fh = new FourHeap<Integer, Integer>();
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
		assertConsistency(fh);
		
		fh.withdrawOrder(ob);
		
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(null, fh.bidQuote());
		assertEquals((Integer) 5, fh.askQuote());
		assertEquals(2, fh.size());
		assertConsistency(fh);

		fh = new FourHeap<Integer, Integer>();
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
		assertConsistency(fh);
	}
	
	@Test
	public void emptyClearTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = new FourHeap<Integer, Integer>();
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
		
		fh = new FourHeap<Integer, Integer>();
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
	}
	
	@Test
	public void multiOrderClearTest() {
		FourHeap<Integer, Integer> fh;
		Order<Integer, Integer> os, ob;
		List<Transaction<Integer, Integer>> transactions;
		Transaction<Integer, Integer> trans;
		
		fh = new FourHeap<Integer, Integer>();
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
	}
	
	protected static Order<Integer, Integer> insertOrder(
			FourHeap<Integer, Integer> fh, int price, int quantity, int time) {
		Order<Integer, Integer> order = new Order<Integer, Integer>(price, quantity, time);
		fh.insertOrder(order);
		return order;
	}
	
	protected static void assertConsistency(FourHeap<Integer, Integer> fh) {
		assertEquals(size(fh.sellMatched), -size(fh.buyMatched));
	}
	
	protected static int size(BinaryHeap<FourHeap<Integer, Integer>.SplitOrder> bh) {
		int size = 0;
		for (FourHeap<Integer, Integer>.SplitOrder so : bh)
			size += so.quantity;
		return size;
	}

}
