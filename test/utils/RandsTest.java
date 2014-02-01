package utils;

import static org.junit.Assert.*;

import java.util.Random;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

public class RandsTest {
	
	@Test
	public void gaussianTest() {
		double delta = 0.1;
		
		SummaryStatistics ds = new SummaryStatistics();
		Random rand = new Random();
		double mean = 100;
		double variance = 10;
		
		for (int i = 0; i < 100000; i++) {
			ds.addValue(Rands.nextGaussian(rand, mean, variance));
		}
		
		assertEquals(mean, ds.getMean(), delta);
		assertEquals(Math.sqrt(variance), ds.getStandardDeviation(), delta);
	}
	
	@Test
	public void exponentialTest() {
		double delta = 10;
		
		SummaryStatistics ds = new SummaryStatistics();
		Random rand = new Random();
		double rate = 0.001;
		
		assertEquals(Double.POSITIVE_INFINITY, Rands.nextExponential(rand, 0), 0);
		
		for (int i = 0; i < 100000; i++) {
			ds.addValue(Rands.nextExponential(rand, rate));
		}
		
		assertEquals(1 / rate, ds.getMean(), delta);
		assertEquals(1 / rate, ds.getStandardDeviation(), delta);
	}
	
	@Test
	public void uniformTest() {
		double delta = 0.5;
		
		SummaryStatistics ds = new SummaryStatistics();
		Random rand = new Random();
		double a = 0;
		double b = 100;
		
		for (int i = 0; i < 100000; i++) {
			ds.addValue(Rands.nextUniform(rand, a, b));
		}
		
		assertEquals((a + b)/2, ds.getMean(), delta);
		assertEquals(Math.sqrt(Math.pow(b - a, 2) / 12), ds.getStandardDeviation(), delta);
	}
}
