package entity.agent;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import systemmanager.Consts.OrderType;
import entity.market.Price;

public class PrivateValueTest {

	@Test
	public void emptyPV() {
		PrivateValue pv = new PrivateValue();
		assertEquals(0, pv.getMaxAbsPosition());
		assertEquals(new Price(0), pv.prices.get(0));
	}
	
	@Test
	public void basicPV() {
		PrivateValue pv = new PrivateValue(10, 0, new Random());
		
		// Verify correct number of elements
		assertEquals(10, pv.getMaxAbsPosition());
		assertEquals(20, pv.prices.size());
		
		/// Verify list is in descending order
		Price prevPrice = Price.INF;
		for (Price p : pv.prices) {
			assertTrue(p.lessThanEqual(prevPrice));
			assertEquals(Price.ZERO, p);
			prevPrice = p;
		}
	}
	
	@Test
	public void buySellSingle() {
		PrivateValue pv = new PrivateValue(1, 1000, new Random());
		
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(0, OrderType.BUY));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(0, OrderType.SELL));
		assertEquals(1, pv.getMaxAbsPosition());
		
		// More detailed checks on buy, with boundary current position values
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(1, OrderType.BUY));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(2, OrderType.BUY));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(10, OrderType.BUY));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-1, OrderType.BUY));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-2, OrderType.BUY));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-10, OrderType.BUY));
		
		// More detailed checks on sell, with boundary current position values
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-1, OrderType.SELL));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-2, OrderType.SELL));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-10, OrderType.SELL));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(1, OrderType.SELL));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(2, OrderType.SELL));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(10, OrderType.SELL));
	}
	
	@Test
	public void buySellMulti() {
		PrivateValue pv = new PrivateValue(2, 1000, new Random());
		// 0 1 2 3
		assertEquals(pv.prices.get(2), pv.getValueFromQuantity(0, 1, OrderType.BUY));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(0, 1, OrderType.SELL));
		assertEquals(2, pv.getMaxAbsPosition());
		
		// More detailed checks on buy, with boundary current position values
		assertEquals(pv.prices.get(3), pv.getValueFromQuantity(0, 2, OrderType.BUY));
		assertEquals(pv.prices.get(3), pv.getValueFromQuantity(0, 3, OrderType.BUY));
		assertEquals(pv.prices.get(2), pv.getValueFromQuantity(0, 1, OrderType.BUY));
		assertEquals(pv.prices.get(3), pv.getValueFromQuantity(1, 1, OrderType.BUY));
		assertEquals(pv.prices.get(3), pv.getValueFromQuantity(2, 1, OrderType.BUY));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(-1, 1, OrderType.BUY));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-2, 1, OrderType.BUY));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(-2, 2, OrderType.BUY));
		assertEquals(pv.prices.get(2), pv.getValueFromQuantity(-1, 2, OrderType.BUY));
		
		// More detailed checks on sell, with boundary current position values
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(0, 2, OrderType.SELL));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(0, 3, OrderType.SELL));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(0, 1, OrderType.SELL));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-1, 1, OrderType.SELL));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-2, 1, OrderType.SELL));
		assertEquals(pv.prices.get(2), pv.getValueFromQuantity(1, 1, OrderType.SELL));
		assertEquals(pv.prices.get(3), pv.getValueFromQuantity(2, 1, OrderType.SELL));
		assertEquals(pv.prices.get(2), pv.getValueFromQuantity(2, 2, OrderType.SELL));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(1, 2, OrderType.SELL));
	}
	
	@Test
	public void getValueFromQuantity() {
		PrivateValue pv = new PrivateValue(5, 1000, new Random());
		
		assertEquals(pv.prices.get(5), pv.getValueFromQuantity(0, OrderType.BUY));
		assertEquals(pv.prices.get(4), pv.getValueFromQuantity(0, OrderType.SELL));
		
		// Checking buying and selling from current position = 1
		assertEquals(pv.prices.get(6), pv.getValueFromQuantity(1, OrderType.BUY));
		assertEquals(pv.prices.get(5), pv.getValueFromQuantity(1, OrderType.SELL));
		
		// Checking buying and selling from current position = -1
		assertEquals(pv.prices.get(4), pv.getValueFromQuantity(-1, OrderType.BUY));
		assertEquals(pv.prices.get(3), pv.getValueFromQuantity(-1, OrderType.SELL));
		
		// Checking buying and selling from current position = 5
		assertEquals(pv.prices.get(9), pv.getValueFromQuantity(5, OrderType.BUY));
		assertEquals(pv.prices.get(9), pv.getValueFromQuantity(5, OrderType.SELL));
		
		// Checking buying and selling from current position = -5
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-5, OrderType.BUY));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-5, OrderType.SELL));
	}
	
	@Test
	public void getValueFromMultiQuantity() {
		PrivateValue pv = new PrivateValue(5, 1000, new Random());
		
		assertEquals(pv.prices.get(6), pv.getValueFromQuantity(0, 2, OrderType.BUY));
		assertEquals(pv.prices.get(3), pv.getValueFromQuantity(0, 2, OrderType.SELL));
		
		// Checking buying and selling from current position = 1
		assertEquals(pv.prices.get(8), pv.getValueFromQuantity(1, 3, OrderType.BUY));
		assertEquals(pv.prices.get(4), pv.getValueFromQuantity(1, 2, OrderType.SELL));
		
		// Checking buying and selling from current position = -1
		assertEquals(pv.prices.get(8), pv.getValueFromQuantity(-1, 4, OrderType.BUY));
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(-1, 3, OrderType.SELL));
		
		// Checking buying and selling from current position = 5
		assertEquals(pv.prices.get(9), pv.getValueFromQuantity(5, 2, OrderType.BUY));
		assertEquals(pv.prices.get(8), pv.getValueFromQuantity(5, 2, OrderType.SELL));
		
		// Checking buying and selling from current position = -5
		assertEquals(pv.prices.get(1), pv.getValueFromQuantity(-5, 2, OrderType.BUY));
		assertEquals(pv.prices.get(0), pv.getValueFromQuantity(-5, 2, OrderType.SELL));
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			buySellSingle();
			buySellMulti();
			getValueFromQuantity();
		}
	}
}
