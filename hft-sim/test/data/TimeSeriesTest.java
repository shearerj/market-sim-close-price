package data;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

import utils.Sparse.SparseElement;

import com.google.common.collect.ImmutableList;

import event.TimeStamp;

public class TimeSeriesTest {
	
	@Test(expected=UnsupportedOperationException.class)
	public void immutableTest() {
		TimeSeries ts = TimeSeries.create();
		ts.add(TimeStamp.of(10), 5);
		Iterator<SparseElement<Double>> it = ts.iterator();
		it.next();
		it.remove();
	}
	
	@Test
	public void overwriteTest() {
		TimeSeries ts = TimeSeries.create();
		ts.add(TimeStamp.of(10), 5);
		ts.add(TimeStamp.of(10), 7);
		assertEquals(ImmutableList.of(SparseElement.create(10, 7d)), ImmutableList.copyOf(ts));
	}
	
	@Test
	public void redundantTest() {
		TimeSeries ts = TimeSeries.create();
		ts.add(TimeStamp.of(10), 5);
		ts.add(TimeStamp.of(20), 5);
		assertEquals(ImmutableList.of(SparseElement.create(10, 5d)), ImmutableList.copyOf(ts));
	}
	
	@Test
	public void redundantOverwriteTest() {
		TimeSeries ts = TimeSeries.create();
		ts.add(TimeStamp.of(10), 5);
		ts.add(TimeStamp.of(20), 7);
		ts.add(TimeStamp.of(20), 5);
		assertEquals(ImmutableList.of(SparseElement.create(10, 5d)), ImmutableList.copyOf(ts));
	}
	
	@Test
	public void equalityTest() {
		TimeSeries ts1 = TimeSeries.create();
		ts1.add(TimeStamp.of(10), 5);
		ts1.add(TimeStamp.of(20), 7);
		ts1.add(TimeStamp.of(20), 5);
		
		TimeSeries ts2 = TimeSeries.create();
		ts2.add(TimeStamp.of(10), 5);
		
		assertEquals(ts1, ts2);
		assertNotEquals(ts1, new Object());
		assertNotEquals(ts1, null);
	}
	
	@Test
	public void toStringTest() {
		TimeSeries ts = TimeSeries.create();
		ts.add(TimeStamp.of(10), 5);
		assertEquals("[" + SparseElement.create(10, 5d) + "]", ts.toString());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void beforeZeroTest() {
		TimeSeries.create().add(TimeStamp.of(-1), 6);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void beforePreviousTest() {
		TimeSeries ts = TimeSeries.create();
		ts.add(TimeStamp.of(10), 5);
		ts.add(TimeStamp.of(5), 5);
	}

}
