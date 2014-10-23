package entity.agent.position;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.*;

import org.junit.Test;

public class AggressionTest {

	@Test
	public void testAggressionDataStructure() {
		Aggression agg = Aggression.create(2, 0.75);
		assertEquals(0.75, agg.getValue(0, BUY), 0.001);
		assertEquals(0.75, agg.getValue(-1, SELL), 0.001);
		assertEquals(0, agg.getValue(-2, SELL), 0.001);
		assertEquals(0, agg.getValue(2, BUY), 0.001);
		
		agg.setValue(0, BUY, 0.5);
		assertEquals(0.5, agg.getValue(0, BUY), 0.001);
		
		agg.setValue(-2, SELL, -0.5);
		// still 0 since outside max position
		assertEquals(0, agg.getValue(-2, SELL), 0.001);
	}

}
