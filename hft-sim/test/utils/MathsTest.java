package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;

import utils.Maths.Median;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

public class MathsTest {
	
	private static final Random rand = new Random();

	@Rule
	public RepeatRule repeatRule = new RepeatRule();
	
	@Test
	public void testingQuantizeInt() { 	
		//standard input
		int n = 1001;
		int quanta= 10;
		int q = Maths.quantize(n, quanta);
		assertEquals(1000, q);
		
		//used to verify function rounds to positive infinity for negative numbers
		n = -32; 
		q = Maths.quantize(n, quanta);
		assertEquals(-30, q);
		
		//verifies that when provided a value that is a multiple of the quanta
		//the program will round to the same value
		n=0;
		q = Maths.quantize(n, quanta);
		assertEquals(0, q);
	}
	
	@Test
	public void testingQuantizeDouble() { 	
		double eps = 0.00001;
		//standard form of input for decimal variant of quantize function
		double n = 100.003;
		double quanta= 10;
		double qresult = Maths.quantize(n, quanta);
		assertEquals(100, qresult, eps);
		
		//verifies that n is not rounded incorrectly
		//verifies that  negative numbers below middle round up to quanta closer to 0
		n = -34.99999; 
		qresult = Maths.quantize(n, quanta);
		assertEquals(-30, qresult, eps);
		
		//in cases where the double n is closer to the lower quanta, quantize rounds down to lower quanta
		n=-35.999999;
		qresult = Maths.quantize(n, quanta);
		assertEquals(-40, qresult, eps);

		//checking that  when provided a value that is a multiple of the quanta
		//the program will round to the same value
		n=-0.000000000000000000;
		qresult = Maths.quantize(n, quanta);
		assertEquals(0, qresult, eps);
		//assertTrue(n==0)// also can be used to check floating point accuracy
	}

	@Test
	public void testingBound() { 
		//testing basic functionality of bound function
		//verifies minimizes num and upper
		//maximizes result and lower bound
		int num = 0;
		int lower = 1;
		int upper = 2;
		int b = Maths.bound(num,lower, upper);
		assertEquals(1, b);
		
		//testing valid results using repeated values 
		// for variables
		num=0; 
		lower = 0;
		upper = 1;
		b = Maths.bound(num,lower, upper);
		assertEquals(0, b);
		
		num=0; lower = 1; upper =0;
		b = Maths.bound(num,lower, upper);
		assertNotSame(0, b);
		
		num=0; lower = 0; upper =1;
		b = Maths.bound(num,lower, upper);
		assertEquals(0, b);
		
		//verifying case when all three inputs provided are same
		num = 0;
		lower = 0;
		upper = 0;
		b = Maths.bound(num,lower, upper);
		assertEquals(0, b);
		
		//testing with negative numbers and large values provided
		num = -999999899;
		//noting that this is the max length int num can have before IDE throws an error 
		lower = -88;
		upper = -875;
		b = Maths.bound(num,lower, upper);
		assertEquals(-88, b);
		
		//testing using inputs for largest integer values possible
		int numLargest =  2147483647; //testing with likely largest number storable in Java x32
		num = numLargest;
		lower = -num; //using smallest number possible in Java
		upper = num;
		b = Maths.bound(num,lower, upper);
		assertEquals(num, b);
		upper = -numLargest;
		b = Maths.bound(num,lower, upper);
		assertEquals(upper, b);
	}
	
	@Test
	public void logRatioTest() {
		List<Double> logRatio = ImmutableList.copyOf(Maths.logRatio(1, Math.E, 1/Math.E));
		assertEquals(1, logRatio.get(0), 0.001);
		assertEquals(-2, logRatio.get(1), 0.001);
	}
	
	@Test
	@Repeat(100)
	public void singleMedianTest() {
		org.apache.commons.math3.stat.descriptive.rank.Median median = new org.apache.commons.math3.stat.descriptive.rank.Median();
		Collection<Double> record = Lists.newArrayList();
		Median med = Median.of();
		for (int i = 0; i < 1000; ++i) {
			double val = rand.nextDouble();
			med.add(val);
			record.add(val);
		}
		median.setData(Doubles.toArray(record));
		assertEquals(median.evaluate(), med.get(), 1e-6);
	}
	
	@Test
	@Repeat(100)
	public void bulkMedianTest() {
		org.apache.commons.math3.stat.descriptive.rank.Median median = new org.apache.commons.math3.stat.descriptive.rank.Median();
		Collection<Double> record = Lists.newArrayList();
		Median med = Median.of();
		for (int i = 0; i < 100; ++i) {
			int count = rand.nextInt(10) + 1;
			double val = rand.nextDouble();
			med.add(val, count);
			Iterators.addAll(record, Iterators.limit(Iterators.cycle(val), count));
		}
		median.setData(Doubles.toArray(record));
		assertEquals(median.evaluate(), med.get(), 1e-6);
	}
	
	@Test
	public void emptyMedianTest() {
		assertEquals(Double.NaN, Median.of().get(), 0);
	}
	
}
