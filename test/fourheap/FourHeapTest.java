package fourheap;

import static org.junit.Assert.*;

import org.junit.Test;

public class FourHeapTest {

	@Test
	public void heapOrderTest() {
		FourHeap<Integer, Integer> fh = new FourHeap<Integer, Integer>();
		
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
		FourHeap<Integer, Integer> fh = new FourHeap<Integer, Integer>();
		Order<Integer, Integer> o1 = new Order<Integer, Integer>(5, 3, 3); 
		fh.insertOrder(o1);
		Pair<?, ?> active = fh.activeOrders.get(o1);
		assertNotEquals(null, active.left());
		assertEquals(null, active.right());
		assertTrue(fh.buyMatched.isEmpty());
		assertFalse(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
	}
	
	@Test
	public void insertOneSellTest() {
		FourHeap<Integer, Integer> fh = new FourHeap<Integer, Integer>();
		Order<Integer, Integer> o1 = new Order<Integer, Integer>(5, -3, 3); 
		fh.insertOrder(o1);
		Pair<?, ?> active = fh.activeOrders.get(o1);
		assertNotEquals(null, active.left());
		assertEquals(null, active.right());
		assertTrue(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertTrue(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
	}
	
	@Test
	public void insertTest() {
		FourHeap<Integer, Integer> fh = new FourHeap<Integer, Integer>();
		
		Order<Integer, Integer> o1 = new Order<Integer, Integer>(5, -3, 3); 
		Order<Integer, Integer> o2 = new Order<Integer, Integer>(6, 3, 4);
		fh.insertOrder(o1);
		fh.insertOrder(o2);
		
		Pair<?, ?> active = fh.activeOrders.get(o1);
		assertEquals(null, active.left());
		assertNotEquals(null, active.right());
		active = fh.activeOrders.get(o2);
		assertEquals(null, active.left());
		assertNotEquals(null, active.right());
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertTrue(fh.sellUnmatched.isEmpty());
		
		Order<Integer, Integer> o3 = new Order<Integer, Integer>(5, -4, 3); 
		Order<Integer, Integer> o4 = new Order<Integer, Integer>(5, 2, 4);
		fh.insertOrder(o3);
		fh.insertOrder(o4);
		
		active = fh.activeOrders.get(o3);
		assertNotEquals(null, active.left());
		assertNotEquals(null, active.right());
		
		assertFalse(fh.buyMatched.isEmpty());
		assertTrue(fh.buyUnmatched.isEmpty());
		assertFalse(fh.sellMatched.isEmpty());
		assertFalse(fh.sellUnmatched.isEmpty());
		
		System.out.println(fh);
	}

}
