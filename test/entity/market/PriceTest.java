package entity.market;

import static org.junit.Assert.*;

import org.junit.Test;

public class PriceTest {

	@Test
	public void toStringTest() {
		assertEquals("$100.00", new Price(100000).toString());
		assertEquals("$100.01", new Price(100010).toString());
		assertEquals("$100.001", new Price(100001).toString());
		assertEquals("-$100.00", new Price(-100000).toString());
		assertEquals("-$100.01", new Price(-100010).toString());
		assertEquals("-$100.001", new Price(-100001).toString());
	}
	
	@Test
	public void infinitePriceTest() {
		// note INF prices occur in the AA strategy
		assertEquals(Price.INF, new Price((double) Integer.MAX_VALUE));
		assertEquals(Price.INF, new Price((double) Integer.MAX_VALUE+1));
		assertEquals(Price.INF, new Price((double) Integer.MAX_VALUE+3));
	}

}
