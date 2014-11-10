package entity.agent.position;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import utils.Rand;

import com.google.common.collect.Ordering;

import entity.market.Price;

public class PrivateValueTest {
	
	@Test
	public void randomGenerationTest() {
		ListPrivateValue pv = ListPrivateValue.createRandomly(10, 1000, Rand.create());
		
		// Verify correct number of elements
		assertEquals(10, pv.getMaxAbsPosition());
		assertEquals(20, pv.getList().size());
		
		/// Verify list is in descending order
		assertTrue("Not reverse sorted", Ordering.natural().reverse().isOrdered(pv.getList()));
	}
	
	@Test
	public void buySellSingle() {
		ListPrivateValue pv = ListPrivateValue.createRandomly(1, 1000, Rand.create());
		// indices 0 1
		
		Price sell = pv.getList().get(0);
		Price buy = pv.getList().get(1);
		
		assertEquals(buy, pv.getValue(0, BUY));
		assertEquals(sell, pv.getValue(0, SELL));
		assertEquals(1, pv.getMaxAbsPosition());
		
		// More detailed checks on buy, with boundary current position values
		assertEquals(buy, pv.getValue(1, BUY));
		assertEquals(buy, pv.getValue(2, BUY));
		assertEquals(buy, pv.getValue(10, BUY));
		assertEquals(sell, pv.getValue(-1, BUY));
		assertEquals(sell, pv.getValue(-2, BUY));
		assertEquals(sell, pv.getValue(-10, BUY));
		
		// More detailed checks on sell, with boundary current position values
		assertEquals(sell, pv.getValue(-1, SELL));
		assertEquals(sell, pv.getValue(-2, SELL));
		assertEquals(sell, pv.getValue(-10, SELL));
		assertEquals(buy, pv.getValue(1, SELL));
		assertEquals(buy, pv.getValue(2, SELL));
		assertEquals(buy, pv.getValue(10, SELL));
	}
	
	@Test
	public void buySellMulti() {
		ListPrivateValue pv = ListPrivateValue.createRandomly(2, 1000, Rand.create());
		// indices 0 1 2 3
		int pv0 = pv.getList().get(0).intValue();
		int pv1 = pv.getList().get(1).intValue();
		int pv2 = pv.getList().get(2).intValue();
		int pv3 = pv.getList().get(3).intValue();
		
		assertEquals(Price.of(pv2), pv.getValue(0, 1, BUY));
		assertEquals(Price.of(pv1), pv.getValue(0, 1, SELL));
		assertEquals(2, pv.getMaxAbsPosition());
		
		// More detailed checks on buy, with boundary current position values
		assertEquals(Price.of(pv2 + pv3), pv.getValue(0, 2, BUY));
		assertEquals(Price.of(pv2 + pv3 + pv3), pv.getValue(0, 3, BUY));
		assertEquals(Price.of(pv2), pv.getValue(0, 1, BUY));
		assertEquals(Price.of(pv3), pv.getValue(1, 1, BUY));
		assertEquals(Price.of(pv3), pv.getValue(2, 1, BUY));
		assertEquals(Price.of(pv1), pv.getValue(-1, 1, BUY));
		assertEquals(Price.of(pv0), pv.getValue(-2, 1, BUY));
		assertEquals(Price.of(pv0 + pv1), pv.getValue(-2, 2, BUY));
		assertEquals(Price.of(pv1 + pv2), pv.getValue(-1, 2, BUY));
		
		// More detailed checks on sell, with boundary current position values
		assertEquals(Price.of(pv1 + pv0), pv.getValue(0, 2, SELL));
		assertEquals(Price.of(pv1 + pv0 + pv0), pv.getValue(0, 3, SELL));
		assertEquals(Price.of(pv1), pv.getValue(0, 1, SELL));
		assertEquals(Price.of(pv0), pv.getValue(-1, 1, SELL));
		assertEquals(Price.of(pv0), pv.getValue(-2, 1, SELL));
		assertEquals(Price.of(pv2), pv.getValue(1, 1, SELL));
		assertEquals(Price.of(pv3), pv.getValue(2, 1, SELL));
		assertEquals(Price.of(pv3 + pv2), pv.getValue(2, 2, SELL));
		assertEquals(Price.of(pv2 + pv1), pv.getValue(1, 2, SELL));
	}

	@Test
	public void getValueFromMultiQuantity() {
		ListPrivateValue pv = ListPrivateValue.createRandomly(5, 1000, Rand.create());
		// indices 0 1 2 3 4 . 5 6 7 8 9
		int pv0 = pv.getList().get(0).intValue();
		int pv1 = pv.getList().get(1).intValue();
		int pv2 = pv.getList().get(2).intValue();
		int pv3 = pv.getList().get(3).intValue();
		int pv4 = pv.getList().get(4).intValue();
		int pv5 = pv.getList().get(5).intValue();
		int pv6 = pv.getList().get(6).intValue();
		int pv7 = pv.getList().get(7).intValue();
		int pv8 = pv.getList().get(8).intValue();
		int pv9 = pv.getList().get(9).intValue();
		
		assertEquals(Price.of(pv5 + pv6), pv.getValue(0, 2, BUY));
		assertEquals(Price.of(pv4 + pv3), pv.getValue(0, 2, SELL));
		
		// Checking buying and selling from current position = 1
		assertEquals(Price.of(pv6 + pv7 + pv8), pv.getValue(1, 3, BUY));
		assertEquals(Price.of(pv5 + pv4), pv.getValue(1, 2, SELL));
		
		// Checking buying and selling from current position = -1
		assertEquals(Price.of(pv4 + pv5 + pv6 + pv7), pv.getValue(-1, 4, BUY));
		assertEquals(Price.of(pv3 + pv2 + pv1), pv.getValue(-1, 3, SELL));
		
		// Checking buying and selling from current position = 5
		assertEquals(Price.of(pv9 + pv9), pv.getValue(5, 2, BUY));
		assertEquals(Price.of(pv9 + pv8), pv.getValue(5, 2, SELL));
		
		// Checking buying and selling from current position = -5
		assertEquals(Price.of(pv0 + pv1), pv.getValue(-5, 2, BUY));
		assertEquals(Price.of(pv0 + pv0), pv.getValue(-5, 2, SELL));
		
		// Checking buying and selling from current position = 6 & -6 (out of bounds)
		assertEquals(Price.of(pv9 + pv9), pv.getValue(6, 2, SELL));
		assertEquals(Price.of(pv0 + pv0), pv.getValue(-6, 2, BUY));
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			buySellSingle();
			buySellMulti();
			getValueFromMultiQuantity();
		}
	}
	
	@Test
	public void testBounds() {
		ListPrivateValue pv = ListPrivateValue.createRandomly(5, 1000, Rand.create());
		Price sell = Price.of(pv.getList().get(0).intValue());
		Price buy = Price.of(pv.getList().get(9).intValue());
		
		assertEquals(buy, pv.getValue(6, BUY));
		assertEquals(sell, pv.getValue(-6, SELL));
		
		assertEquals(buy, pv.getValue(5, BUY));
		assertEquals(sell, pv.getValue(-5, SELL));
		
		assertEquals(sell, pv.getValue(-6, BUY));
		assertEquals(buy, pv.getValue(6, SELL));
		
		assertEquals(sell, pv.getValue(-5, BUY));
		assertEquals(buy, pv.getValue(5, SELL));
		
		assertEquals(buy, pv.getValue(4, BUY));
		assertEquals(sell, pv.getValue(-4, SELL));
		
		assertEquals(buy, pv.getValue(4, 1, BUY));
		assertEquals(sell, pv.getValue(-4, 1, SELL));
		
		assertEquals(sell, pv.getValue(-5, 1, BUY));
		assertEquals(buy, pv.getValue(5, 1, SELL));
		
		assertEquals(buy, pv.getValue(5, 1, BUY));
		assertEquals(sell, pv.getValue(-5, 1, SELL));
	}

}