package entity.agent.position;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import utils.Rand;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;

import fourheap.Order.OrderType;

public class MarginTest {
	
	@Test
	public void basicMargin() {
		Margin margin = Margin.createRandomly(1, Rand.create(), 1, 1);
		assertEquals(2, margin.getList().size());
		assertEquals(1, margin.getMaxAbsPosition());
		// check that positive margin for selling, negative for buying
		assertEquals(1, margin.getList().get(0), 0);
		assertEquals(-1, margin.getList().get(1), 0);
	}
	
	@Test
	public void multiUnitMargin() {
		Margin margin = Margin.createRandomly(2, Rand.create(), 0, 1);
		assertEquals(4, margin.getList().size());
		for (double value : margin.getList().subList(0, 2))
			assertTrue("Margins outside range", Range.closed(0d, 1d).contains(value));
		for (double value : margin.getList().subList(2, 4))
			assertTrue("Margins outside range", Range.closed(-1d, 0d).contains(value));
	}
	
	@Test
	public void getValueTest() {
		Margin margin = Margin.createRandomly(2, Rand.create(), 0, 1);
		double m0 = margin.getList().get(0);
		double m1 = margin.getList().get(1);
		double m2 = margin.getList().get(2);
		double m3 = margin.getList().get(3);
		
		assertEquals(m2, margin.getValue(0, OrderType.BUY), 0);
		assertEquals(m3, margin.getValue(1, OrderType.BUY), 0);
		assertEquals(m1, margin.getValue(0, OrderType.SELL), 0);
		assertEquals(m0, margin.getValue(-1, OrderType.SELL), 0);
		// returns 0 when exceed max position
		assertEquals(0, margin.getValue(-2, OrderType.SELL), 0);
		assertEquals(0, margin.getValue(2, OrderType.BUY), 0);
	}
	
	@Test
	public void setValueTest() {
		Margin margin = Margin.createRandomly(2, Rand.create(), 0, 1);
		double m2 = margin.getList().get(2);
		
		// basic set
		margin.setValue(0, OrderType.BUY, 0d);
		assertNotEquals(m2, margin.getValue(0, OrderType.BUY), 0);
		assertEquals(0, margin.getValue(0, OrderType.BUY), 0);
	
		// test setting outside valid position (won't change anything)
		margin.setValue(-2, OrderType.SELL, 1d);
		assertNotEquals(1, margin.getValue(-2, OrderType.SELL), 0);
		assertEquals(0, margin.getValue(-2, OrderType.SELL), 0);
	}
	
	@Test
	public void setMultipleValueTest() {
		Margin margin = Margin.createRandomly(2, Rand.create(), 0, 1);
		
		// set
		margin.setValue(0, BUY, 1d);
		margin.setValue(1, BUY, 2d);
		margin.setValue(0, SELL, -1d);
		margin.setValue(-1, SELL, 0d);
		
		assertEquals(Doubles.asList(0, -1, 1, 2), margin.getList());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			multiUnitMargin();
		}
	}
}
