package entity.market;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.Ordering;

public class PriceTest {

	@Test
	public void toDollarStringTest() {
		assertEquals("$100.00", Price.of(100000).toDollarString());
		assertEquals("$100.01", Price.of(100010).toDollarString());
		assertEquals("$100.001", Price.of(100001).toDollarString());
		assertEquals("-$100.00", Price.of(-100000).toDollarString());
		assertEquals("-$100.01", Price.of(-100010).toDollarString());
		assertEquals("-$100.001", Price.of(-100001).toDollarString());
	}
	
	@Test
	public void infinitePriceTest() {
		assertEquals(Integer.MAX_VALUE, Price.of((double) Integer.MAX_VALUE).intValue());
		assertEquals(Price.INF, Price.of((double) Integer.MAX_VALUE+1));
		assertEquals(Price.INF, Price.of((double) Integer.MAX_VALUE+3));
		
		assertEquals(Integer.MIN_VALUE, Price.of((double) Integer.MIN_VALUE).intValue());
		assertEquals(Price.NEG_INF, Price.of((double) Integer.MIN_VALUE-1));
		assertEquals(Price.NEG_INF, Price.of((double) Integer.MIN_VALUE-3));
		
		assertTrue(Price.INF.greaterThan(Price.of(Integer.MAX_VALUE)));
		assertTrue(Price.NEG_INF.lessThan(Price.of(Integer.MIN_VALUE)));
	}
	
	@Test
	public void comparisonTest() {
		Ordering<Price> ord = Ordering.natural();
		assertEquals(1, ord.compare(Price.of(4), Price.of(3)));
		assertEquals(0, ord.compare(Price.of(3), Price.of(3)));
		assertEquals(-1, ord.compare(Price.of(2), Price.of(3)));
		assertEquals(1, ord.compare(Price.INF, Price.of(3)));
		assertEquals(-1, ord.compare(Price.NEG_INF, Price.of(3)));
	}

}
