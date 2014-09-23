package data;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import utils.SummStats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import event.TimeStamp;

public class StatsTest {
	
	private static final String key = "test";
	private Stats stats;
	
	@Before
	public void setup() {
		stats = Stats.create();
	}

	@Test
	public void postStatTest() {
		stats.post(key, 0);
		stats.post(key, -1);
		stats.post(key, 1);
		
		SummStats summ = stats.getSummaryStats().get(key);
		assertEquals(0, summ.mean(), 1e-6);
		assertEquals(3, summ.n());
		assertEquals(1, summ.stddev(), 1e-6);
	}
	
	@Test
	public void postTimedTest() {
		stats.postTimed(TimeStamp.of(0), key, 0);
		stats.postTimed(TimeStamp.of(1), key, -1);
		stats.postTimed(TimeStamp.of(2), key, 1);
		
		TimeSeries time = stats.getTimeStats().get(key);
		assertEquals(ImmutableList.of(0d, -1d, 1d), ImmutableList.copyOf(Iterators.limit(time.iterator(), 3)));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void postIllegalTime() {
		stats.postTimed(TimeStamp.of(5), key, 0);
		stats.postTimed(TimeStamp.of(0), key, 0);
	}

}
