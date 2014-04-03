package entity.agent;

import static org.junit.Assert.*;
import static fourheap.Order.OrderType.*;

import java.util.Random;

import org.junit.Test;

import entity.market.Price;

public class PrivateValueTest {

	@Test
	public void emptyPV() {
		PrivateValue pv = new PrivateValue();
		assertEquals(0, pv.getMaxAbsPosition());
		assertEquals(new Price(0), pv.values.get(0));
	}
	
	@Test
	public void basicPV() {
		PrivateValue pv = new PrivateValue(10, 1000, new Random());
		
		// Verify correct number of elements
		assertEquals(10, pv.getMaxAbsPosition());
		assertEquals(20, pv.values.size());
		
		/// Verify list is in descending order
		Price prevPrice = Price.INF;
		for (Price p : pv.values) {
			assertTrue(p.lessThanEqual(prevPrice));
			prevPrice = p;
		}
	}
	
	@Test
	public void buySellSingle() {
		PrivateValue pv = new PrivateValue(1, 1000, new Random());
		// indices 0 1
		
		assertEquals(pv.values.get(1), pv.getValue(0, BUY));
		assertEquals(pv.values.get(0), pv.getValue(0, SELL));
		assertEquals(1, pv.getMaxAbsPosition());
		
		// More detailed checks on buy, with boundary current position values
		assertEquals(Price.ZERO, pv.getValue(1, BUY));
		assertEquals(Price.ZERO, pv.getValue(2, BUY));
		assertEquals(Price.ZERO, pv.getValue(10, BUY));
		assertEquals(pv.values.get(0), pv.getValue(-1, BUY));
		assertEquals(Price.ZERO, pv.getValue(-2, BUY));
		assertEquals(Price.ZERO, pv.getValue(-10, BUY));
		
		// More detailed checks on sell, with boundary current position values
		assertEquals(Price.ZERO, pv.getValue(-1, SELL));
		assertEquals(Price.ZERO, pv.getValue(-2, SELL));
		assertEquals(Price.ZERO, pv.getValue(-10, SELL));
		assertEquals(pv.values.get(1), pv.getValue(1, SELL));
		assertEquals(Price.ZERO, pv.getValue(2, SELL));
		assertEquals(Price.ZERO, pv.getValue(10, SELL));
	}
	
	@Test
	public void buySellMulti() {
		PrivateValue pv = new PrivateValue(2, 1000, new Random());
		// indices 0 1 2 3
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		
		assertEquals(new Price(pv2), pv.getValueFromQuantity(0, 1, BUY));
		assertEquals(new Price(pv1), pv.getValueFromQuantity(0, 1, SELL));
		assertEquals(2, pv.getMaxAbsPosition());
		
		// More detailed checks on buy, with boundary current position values
		assertEquals(new Price(pv2 + pv3), pv.getValueFromQuantity(0, 2, BUY));
		assertEquals(new Price(pv2 + pv3), pv.getValueFromQuantity(0, 3, BUY));
		assertEquals(new Price(pv2), pv.getValueFromQuantity(0, 1, BUY));
		assertEquals(new Price(pv3), pv.getValueFromQuantity(1, 1, BUY));
		assertEquals(Price.ZERO, pv.getValueFromQuantity(2, 1, BUY));
		assertEquals(new Price(pv1), pv.getValueFromQuantity(-1, 1, BUY));
		assertEquals(new Price(pv0), pv.getValueFromQuantity(-2, 1, BUY));
		assertEquals(new Price(pv0 + pv1), pv.getValueFromQuantity(-2, 2, BUY));
		assertEquals(new Price(pv1 + pv2), pv.getValueFromQuantity(-1, 2, BUY));
		
		// More detailed checks on sell, with boundary current position values
		assertEquals(new Price(pv1 + pv0), pv.getValueFromQuantity(0, 2, SELL));
		assertEquals(new Price(pv1 + pv0), pv.getValueFromQuantity(0, 3, SELL));
		assertEquals(new Price(pv1), pv.getValueFromQuantity(0, 1, SELL));
		assertEquals(new Price(pv0), pv.getValueFromQuantity(-1, 1, SELL));
		assertEquals(Price.ZERO, pv.getValueFromQuantity(-2, 1, SELL));
		assertEquals(new Price(pv2), pv.getValueFromQuantity(1, 1, SELL));
		assertEquals(new Price(pv3), pv.getValueFromQuantity(2, 1, SELL));
		assertEquals(new Price(pv3 + pv2), pv.getValueFromQuantity(2, 2, SELL));
		assertEquals(new Price(pv2 + pv1), pv.getValueFromQuantity(1, 2, SELL));
	}
	
	@Test
	public void getValueFromQuantity() {
		PrivateValue pv = new PrivateValue(5, 1000, new Random());
		
		assertEquals(pv.values.get(5), pv.getValue(0, BUY));
		assertEquals(pv.values.get(4), pv.getValue(0, SELL));
		
		// Checking buying and selling from current position = 1
		assertEquals(pv.values.get(6), pv.getValue(1, BUY));
		assertEquals(pv.values.get(5), pv.getValue(1, SELL));
		
		// Checking buying and selling from current position = -1
		assertEquals(pv.values.get(4), pv.getValue(-1, BUY));
		assertEquals(pv.values.get(3), pv.getValue(-1, SELL));
		
		// Checking buying and selling from current position = 5
		assertEquals(Price.ZERO, pv.getValue(5, BUY));
		assertEquals(pv.values.get(9), pv.getValue(5, SELL));
		
		// Checking buying and selling from current position = -5
		assertEquals(pv.values.get(0), pv.getValue(-5, BUY));
		assertEquals(Price.ZERO, pv.getValue(-5, SELL));
	}
	
	@Test
	public void getValueFromMultiQuantity() {
		PrivateValue pv = new PrivateValue(5, 1000, new Random());
		// indices 0 1 2 3 4 . 5 6 7 8 9
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		int pv4 = pv.values.get(4).intValue();
		int pv5 = pv.values.get(5).intValue();
		int pv6 = pv.values.get(6).intValue();
		int pv7 = pv.values.get(7).intValue();
		int pv8 = pv.values.get(8).intValue();
		int pv9 = pv.values.get(9).intValue();
		
		assertEquals(new Price(pv5 + pv6), pv.getValueFromQuantity(0, 2, BUY));
		assertEquals(new Price(pv4 + pv3), pv.getValueFromQuantity(0, 2, SELL));
		
		// Checking buying and selling from current position = 1
		assertEquals(new Price(pv6 + pv7 + pv8), pv.getValueFromQuantity(1, 3, BUY));
		assertEquals(new Price(pv5 + pv4), pv.getValueFromQuantity(1, 2, SELL));
		
		// Checking buying and selling from current position = -1
		assertEquals(new Price(pv4 + pv5 + pv6 + pv7), pv.getValueFromQuantity(-1, 4, BUY));
		assertEquals(new Price(pv3 + pv2 + pv1), pv.getValueFromQuantity(-1, 3, SELL));
		
		// Checking buying and selling from current position = 5
		assertEquals(Price.ZERO, pv.getValueFromQuantity(5, 2, BUY));
		assertEquals(new Price(pv9 + pv8), pv.getValueFromQuantity(5, 2, SELL));
		
		// Checking buying and selling from current position = -5
		assertEquals(new Price(pv0 + pv1), pv.getValueFromQuantity(-5, 2, BUY));
		assertEquals(Price.ZERO, pv.getValueFromQuantity(-5, 2, SELL));
		
		// Checking buying and selling from current position = 6 & -6
		assertEquals(new Price(pv9), pv.getValueFromQuantity(6, 2, SELL));
		assertEquals(new Price(pv0), pv.getValueFromQuantity(-6, 2, BUY));
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			buySellSingle();
			buySellMulti();
			getValueFromQuantity();
			getValueFromMultiQuantity();
		}
	}
}