package utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class RandTest {
	
	@Test
	public void gaussianTest() {
		double delta = 0.1;
		
		SummStats ds = SummStats.on();
		Rand rand = Rand.create();
		double mean = 100;
		double variance = 10;
		
		for (int i = 0; i < 100000; i++) {
			ds.add(rand.nextGaussian(mean, variance));
		}
		
		assertEquals(mean, ds.mean(), delta);
		assertEquals(Math.sqrt(variance), ds.stddev(), delta);
	}
	
	@Test
	public void exponentialTest() {
		double delta = 10;
		
		SummStats ds = SummStats.on();
		Rand rand = Rand.create();
		double rate = 0.001;
		
		assertEquals(Double.POSITIVE_INFINITY, rand.nextExponential(0), 0);
		
		for (int i = 0; i < 1000000; i++) {
			ds.add(rand.nextExponential(rate));
		}
		
		assertEquals(1 / rate, ds.mean(), delta);
		assertEquals(1 / rate, ds.stddev(), delta);
	}
	
	@Test
	public void uniformTest() {
		double delta = 0.5;
		
		SummStats ds = SummStats.on();
		Rand rand = Rand.create();
		double a = 0;
		double b = 100;
		
		for (int i = 0; i < 100000; i++) {
			ds.add(rand.nextUniform(a, b));
		}
		
		assertEquals((a + b)/2, ds.mean(), delta);
		assertEquals(Math.sqrt(Math.pow(b - a, 2) / 12), ds.stddev(), delta);
	}
}
