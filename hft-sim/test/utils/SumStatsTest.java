package utils;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;

public class SumStatsTest {
	
	private static final Random rand = new Random();

	@Rule
	public RepeatRule repeatRule = new RepeatRule();
	
	@Test
	@Repeat(100)
	public void positiveTest() {
		SummStats test = SummStats.on();
		BigStat truth = new BigStat();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = rand.nextDouble();
			test.add(d);
			truth.add(d);
		}
		assertEquals(truth.sum(), test.sum(), 1e-8);
		assertEquals(truth.mean(), test.mean(), 1e-8);
		assertEquals(truth.variance(), test.variance(), 1e-8);
		assertEquals(truth.stddev(), test.stddev(), 1e-8);
	}
	
	@Test
	public void negativeTest() {
		SummStats test = SummStats.on();
		BigStat truth = new BigStat();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = -rand.nextDouble();
			test.add(d);
			truth.add(d);
		}
		assertEquals(truth.sum(), test.sum(), 1e-8);
		assertEquals(truth.mean(), test.mean(), 1e-8);
		assertEquals(truth.variance(), test.variance(), 1e-8);
		assertEquals(truth.stddev(), test.stddev(), 1e-8);
	}
	
	@Test
	@Repeat(100)
	public void mixedTest() {
		SummStats test = SummStats.on();
		BigStat truth = new BigStat();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = rand.nextDouble() - .5;
			test.add(d);
			truth.add(d);
		}
		assertEquals(truth.sum(), test.sum(), 1e-8);
		assertEquals(truth.mean(), test.mean(), 1e-8);
		assertEquals(truth.variance(), test.variance(), 1e-8);
		assertEquals(truth.stddev(), test.stddev(), 1e-8);
	}
	
	@Test
	public void emptyTest() {
		SummStats test = SummStats.on();
		assertEquals(Double.NaN, test.sum(), 1e-8);
		assertEquals(Double.NaN, test.mean(), 1e-8);
		assertEquals(Double.NaN, test.variance(), 1e-8);
		assertEquals(Double.NaN, test.stddev(), 1e-8);
		assertEquals(Double.NaN, test.min(), 1e-8);
		assertEquals(Double.NaN, test.max(), 1e-8);
	}
	
	@Test
	public void singleTest() {
		SummStats test = SummStats.on(5);
		assertEquals(5, test.sum(), 1e-8);
		assertEquals(5, test.mean(), 1e-8);
		assertEquals(0, test.variance(), 1e-8);
		assertEquals(0, test.stddev(), 1e-8);
		assertEquals(5, test.min(), 1e-8);
		assertEquals(5, test.max(), 1e-8);
	}
	
	@Test
	@Repeat(100)
	public void singletonMergeTest() {
		SummStats test = SummStats.on();
		BigStat truth = new BigStat();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			double d = rand.nextDouble() - .5;
			test.merge(SummStats.on(d));
			truth.add(d);
		}
		assertEquals(truth.sum(), test.sum(), 1e-8);
		assertEquals(truth.mean(), test.mean(), 1e-8);
		assertEquals(truth.variance(), test.variance(), 1e-8);
		assertEquals(truth.stddev(), test.stddev(), 1e-8);
	}
	
	@Test
	@Repeat(100)
	public void multipleMergeTest() {
		SummStats test = SummStats.on();
		BigStat truth = new BigStat();
		int n = rand.nextInt(1000) + 1000;
		for (int i = 0; i < n; ++i) {
			SummStats sub = SummStats.on();
			int m = rand.nextInt(10) + 1;
			for (int j = 0; j < m; ++j) {
				double d = rand.nextDouble() - .5;
				sub.add(d);
				truth.add(d);
			}
			test.merge(sub);
		}
		assertEquals(truth.sum(), test.sum(), 1e-8);
		assertEquals(truth.mean(), test.mean(), 1e-8);
		assertEquals(truth.variance(), test.variance(), 1e-8);
		assertEquals(truth.stddev(), test.stddev(), 1e-8);
	}
	
	public static class BigStat {

		long n;
		BigDecimal sum, sumsq;
		
		BigStat() {
			n = 0;
			sum = new BigDecimal(0);
			sumsq = new BigDecimal(0);
		}
		
		void add(double val) {
			++n;
			sum = sum.add(new BigDecimal(val));
			sumsq = sumsq.add(new BigDecimal(val).pow(2));
		}
		
		double sum() {
			return sum.doubleValue();
		}
		
		double mean() {
			return sum.doubleValue() / n;
		}
		
		double variance() {
			return sumsq.multiply(new BigDecimal(n)).subtract(sum.pow(2)).doubleValue() / (n * (n - 1));
		}
		
		double stddev() {
			return Math.sqrt(variance());
		}
		
	}

}
