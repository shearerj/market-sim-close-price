package entity.market;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import event.TimeStamp;

public class MarketTimeTest {

	@Test
	public void comparisonTest() {
		assertEquals(0, MarketTime.from(TimeStamp.of(0), 0).compareTo(MarketTime.from(TimeStamp.of(0), 0)));
		assertEquals(-1, MarketTime.from(TimeStamp.of(0), 0).compareTo(MarketTime.from(TimeStamp.of(0), 1)));
		assertEquals(1, MarketTime.from(TimeStamp.of(0), 1).compareTo(MarketTime.from(TimeStamp.of(0), 0)));
		assertEquals(-1, MarketTime.from(TimeStamp.of(0), 0).compareTo(MarketTime.from(TimeStamp.of(1), 0)));
		assertEquals(1, MarketTime.from(TimeStamp.of(1), 0).compareTo(MarketTime.from(TimeStamp.of(0), 0)));
		assertEquals(-1, MarketTime.from(TimeStamp.of(0), 1).compareTo(MarketTime.from(TimeStamp.of(1), 0)));
		assertEquals(1, MarketTime.from(TimeStamp.of(1), 0).compareTo(MarketTime.from(TimeStamp.of(0), 1)));
	}

}
