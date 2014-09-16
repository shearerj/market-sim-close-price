package data;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import org.junit.Test;

import utils.Iterables2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Doubles;

public class TimeSeriesTest {
	
	@Test(expected=UnsupportedOperationException.class)
	public void immutableTest() {
		TimeSeries ts = TimeSeries.create();
		ts.add(10, 5);
		Iterator<Double> it = ts.iterator();
		it.next();
		it.remove();
		fail();
	}
	
	@Test
	public void filterTest() {
		TimeSeries ts;
		List<Double> test;
		
		ts = TimeSeries.create();
		ts.add(0, 4.5);
		ts.add(50, Double.NaN);
		for (double d : Iterables.limit(ts.removeNans(), 100))
			assertEquals(4.5, d, 0);
		
		ts = TimeSeries.create();
		ts.add(0, 5.6);
		ts.add(25, 7.4);
		ts.add(30, Double.NaN);
		ts.add(50, 3.9);
		
		test = ImmutableList.copyOf(Iterables.limit(Iterables2.sample(ts, 25, -1), 3));
		assertEquals(3, test.size());
		assertEquals(Doubles.asList(5.6, Double.NaN, 3.9), test);
		
		test = ImmutableList.copyOf(Iterables.limit(Iterables2.sample(ts.removeNans(), 25, -1), 3));
		assertEquals(3, Iterables.size(test));
		assertEquals(Doubles.asList(5.6, 7.4, 3.9), test);
	}

}
