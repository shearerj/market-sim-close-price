package data;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class TimeSeriesTest {
	
	protected final static Random rand = new Random();

	@Test
	public void lengthTest() {
		TimeSeries t;
		double[] array;
		
		t = new TimeSeries();
		array = t.sample(1, 100);
		assertEquals(100, array.length);
		
		t = new TimeSeries();
		array = t.sample(2, 100);
		assertEquals(50, array.length);
		
		t = new TimeSeries();
		array = t.sample(3, 100);
		assertEquals(33, array.length);
		
		for (int i = 0; i < 100; i++) {
			int length = rand.nextInt(500) + 100;
			int period = rand.nextInt(length) + 1;
			t = new TimeSeries();
			array = t.sample(period, length);
			assertEquals(length / period, array.length);
		}
	}
	
	@Test
	public void duplicateTest() {
		TimeSeries t;
		double[] array;
		
		t = new TimeSeries();
		array = t.sample(1, 100);
		for (double d : array)
			assertTrue(Double.isNaN(d));
		
		t = new TimeSeries();
		t.add(0, 5.6);
		array = t.sample(1, 100);
		for (double d : array)
			assertEquals(5.6, d, 0);
	}
	
	@Test
	public void truncationTest() {
		TimeSeries t;
		double[] array;
		
		t = new TimeSeries();
		t.add(0, 5.6);
		t.add(101, 3.6);
		array = t.sample(1, 100);
		for (double d : array)
			assertEquals(5.6, d, 0);
		
		t = new TimeSeries();
		t.add(0, 5.6);
		t.add(99, 3.6);
		array = t.sample(1, 100);
		for (double d : Arrays.copyOfRange(array, 0, 99))
			assertEquals(5.6, d, 0);
		assertEquals(3.6, array[99], 0);
	}
	
	@Test
	public void sansNansTest() {
		TimeSeries t;
		double[] array;
		
		t = new TimeSeries();
		t.add(50, 5.6);
		array = TimeSeries.sansNans(t.sample(1, 100));
		assertEquals(50, array.length);
		for (double d : Arrays.copyOfRange(array, 0, 50))
			assertEquals(5.6, d, 0);
		
		t = new TimeSeries();
		t.add(0, 5.6);
		t.add(25, Double.NaN);
		t.add(50, 7.2);
		array = TimeSeries.sansNans(t.sample(1, 100));
		assertEquals(75, array.length);
		for (double d : Arrays.copyOfRange(array, 0, 25))
			assertEquals(5.6, d, 0);
		for (double d : Arrays.copyOfRange(array, 25, 75))
			assertEquals(7.2, d, 0);
	}
	
	@Test
	public void logRatioTest() {
		TimeSeries t;
		double[] array;
		
		t = new TimeSeries();
		t.add(0, 1);
		t.add(1, Math.E);
		t.add(2, Math.E * Math.E);
		t.add(3, Math.pow(Math.E, 4));
		array = TimeSeries.logRatio(t.sample(1, 4));
		assertEquals(1, array[0], 0.001);
		assertEquals(1, array[1], 0.001);
		assertEquals(2, array[2], 0.001);
	}

}
