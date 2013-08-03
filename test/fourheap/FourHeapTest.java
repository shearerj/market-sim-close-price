package fourheap;

import static org.junit.Assert.*;

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
		assertEquals(5, (int) fh.quote().left());
		assertEquals(null, fh.quote().right());
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
		assertEquals(null, fh.quote().left());
		assertEquals(5, (int) fh.quote().right());
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
		assertEquals(5, (int) fh.quote().left());
		assertEquals(7, (int) fh.quote().right());
		
		fh = new FourHeap<Integer, Integer>();
		insertOrder(fh, 5, -5, 0);
		insertOrder(fh, 7, 3, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		assertEquals(5, (int) fh.quote().left());
		assertEquals(5, (int) fh.quote().right());
		
		fh = new FourHeap<Integer, Integer>();
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 5, 1);
		
		assertFalse(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		assertEquals(7, (int) fh.quote().left());
		assertEquals(7, (int) fh.quote().right());
	}
	
	@Test
	public void insertMatchedTest() {
		FourHeap<Integer, Integer> fh;
		
		fh = new FourHeap<Integer, Integer>();		
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 4, 1, 2);
		assertEquals(5, (int) fh.quote().left());
		assertEquals(7, (int) fh.quote().right());
		
		fh = new FourHeap<Integer, Integer>();		
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 6, 1, 2);
		assertEquals(6, (int) fh.quote().left());
		assertEquals(7, (int) fh.quote().right());

		fh = new FourHeap<Integer, Integer>();		
		insertOrder(fh, 5, -3, 0);
		insertOrder(fh, 7, 3, 1);
		insertOrder(fh, 8, 1, 2);
		assertEquals(7, (int) fh.quote().left());
		assertEquals(7, (int) fh.quote().right());
	}
	
	protected Order<Integer, Integer> insertOrder(FourHeap<Integer, Integer> fh, int price, int quantity, int time) {
		Order<Integer, Integer> order = new Order<Integer, Integer>(price, quantity, time);
		fh.insertOrder(order);
		return order;
	}

}
