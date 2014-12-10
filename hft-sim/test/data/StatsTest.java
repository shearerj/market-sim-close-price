package data;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import utils.Sparse.SparseElement;
import utils.SummStats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

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
		Builder<SparseElement<Double>> truth = ImmutableList.builder();
		stats.postTimed(TimeStamp.of(0), key, 0);
		truth.add(SparseElement.create(0, 0d));
		stats.postTimed(TimeStamp.of(1), key, -1);
		truth.add(SparseElement.create(1, -1d));
		stats.postTimed(TimeStamp.of(2), key, 1);
		truth.add(SparseElement.create(2, 1d));
		
		assertEquals(truth.build(), ImmutableList.copyOf(stats.getTimeStats().get(key)));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void postIllegalTime() {
		stats.postTimed(TimeStamp.of(5), key, 0);
		stats.postTimed(TimeStamp.of(0), key, 0);
	}

}
