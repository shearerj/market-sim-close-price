package entity.market;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import event.TimeStamp;

public class MarketTimeTest {

	@Test
	public void comparisonTest() {
		// Compare to other market times
		assertEquals(0, MarketTime.from(TimeStamp.of(0), 0).compareTo(MarketTime.from(TimeStamp.of(0), 0)));
		assertEquals(-1, MarketTime.from(TimeStamp.of(0), 0).compareTo(MarketTime.from(TimeStamp.of(0), 1)));
		assertEquals(1, MarketTime.from(TimeStamp.of(0), 1).compareTo(MarketTime.from(TimeStamp.of(0), 0)));
		assertEquals(-1, MarketTime.from(TimeStamp.of(0), 0).compareTo(MarketTime.from(TimeStamp.of(1), 0)));
		assertEquals(1, MarketTime.from(TimeStamp.of(1), 0).compareTo(MarketTime.from(TimeStamp.of(0), 0)));
		assertEquals(-1, MarketTime.from(TimeStamp.of(0), 1).compareTo(MarketTime.from(TimeStamp.of(1), 0)));
		assertEquals(1, MarketTime.from(TimeStamp.of(1), 0).compareTo(MarketTime.from(TimeStamp.of(0), 1)));
		
		// Compare to time stamps
		assertEquals(0, MarketTime.from(TimeStamp.of(0), 0).compareTo(TimeStamp.of(0)));
		assertEquals(-1, MarketTime.from(TimeStamp.of(0), 0).compareTo(TimeStamp.of(1)));
		assertEquals(1, MarketTime.from(TimeStamp.of(1), 0).compareTo(TimeStamp.of(0)));
		assertEquals(0, MarketTime.from(TimeStamp.of(0), 1).compareTo(TimeStamp.of(0)));
		assertEquals(-1, MarketTime.from(TimeStamp.of(0), 1).compareTo(TimeStamp.of(1)));
		assertEquals(1, MarketTime.from(TimeStamp.of(1), 1).compareTo(TimeStamp.of(0)));
		
		// Compare time stamps to market time (order)
		assertEquals(0, TimeStamp.of(0).compareTo(MarketTime.from(TimeStamp.of(0), 0)));
		assertEquals(-1, TimeStamp.of(0).compareTo(MarketTime.from(TimeStamp.of(1), 0)));
		assertEquals(1, TimeStamp.of(1).compareTo(MarketTime.from(TimeStamp.of(0), 0)));
		assertEquals(0, TimeStamp.of(0).compareTo(MarketTime.from(TimeStamp.of(0), 1)));
		assertEquals(-1, TimeStamp.of(0).compareTo(MarketTime.from(TimeStamp.of(1), 1)));
		assertEquals(1, TimeStamp.of(1).compareTo(MarketTime.from(TimeStamp.of(0), 1)));
	}

}
