package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import utils.Sparse.SparseElement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;

public class SparseTest {
	
	private static final Random rand = new Random();

	@Test
	public void emptyMedianTest() {
		assertEquals(Double.NaN, Sparse.median(ImmutableList.<SparseElement<? extends Number>> of(), 100), 0);
		assertEquals(Double.NaN, Sparse.median(ImmutableList.of(SparseElement.create(6, 5)), 0), 0);
		assertEquals(Double.NaN, Sparse.median(ImmutableList.of(SparseElement.create(6, 5)), 5), 0);
	}
	
	@Test
	public void singletonMedianTest() {
		assertEquals(5, Sparse.median(ImmutableList.of(SparseElement.create(0, 5)), 1), 0);
		assertEquals(5, Sparse.median(ImmutableList.of(SparseElement.create(0, 5)), 100), 0);
		assertEquals(5, Sparse.median(ImmutableList.of(SparseElement.create(6, 5)), 7), 0);
	}
	
	@Test
	public void medianTest() {
		List<SparseElement<Double>> points = Lists.newArrayList();
		
		points.add(SparseElement.create(0, 3d));
		assertEquals(3, Sparse.median(points, 5), 0);
		
		points.add(SparseElement.create(6, 8d));
		assertEquals(3, Sparse.median(points, 11), 0);
		assertEquals(5.5, Sparse.median(points, 12), 0);
		assertEquals(8, Sparse.median(points, 13), 0);
		
		points.add(SparseElement.create(13, 5d));
		assertEquals(6.5, Sparse.median(points, 14), 0);
		
		points.add(SparseElement.create(14, 7d));
		assertEquals(7, Sparse.median(points, 15), 0);
	}
	
	@Test
	public void emptyStddevTest() {
		assertEquals(Double.NaN, Sparse.stddev(ImmutableList.<SparseElement<? extends Number>> of(), 1, 100), 0);
		assertEquals(Double.NaN, Sparse.stddev(ImmutableList.<SparseElement<? extends Number>> of(), 5, 100), 0);
		assertEquals(Double.NaN, Sparse.stddev(ImmutableList.of(SparseElement.create(6, 5)), 1, -1), 0);
		assertEquals(Double.NaN, Sparse.stddev(ImmutableList.of(SparseElement.create(6, 5)), 1, 5), 0);
		assertEquals(Double.NaN, Sparse.stddev(ImmutableList.of(SparseElement.create(0, 5)), 5, -1), 0);
		assertEquals(Double.NaN, Sparse.stddev(ImmutableList.of(SparseElement.create(0, 5)), 100, 50), 0);
		assertEquals(Double.NaN, Sparse.stddev(ImmutableList.of(SparseElement.create(0, 5)), 100, 98), 0);
	}
	
	@Test
	public void stddevTest() {
		assertEquals(Double.NaN, Sparse.stddev(ImmutableList.of(SparseElement.create(0, 5)), 100, 98), 0);
		assertEquals(0, Sparse.stddev(ImmutableList.of(SparseElement.create(0, 5)), 100, 100), 0); // Samples at end of interval
		
		List<SparseElement<Double>> test = ImmutableList.of(SparseElement.create(0, 6d), SparseElement.create(100, 100d), SparseElement.create(160, 12d));
		assertEquals(Math.sqrt(12), Sparse.stddev(test, 100, 300), 1e-6); // At end should have one 6 and two 12 
		assertEquals(37.02251567177285, Sparse.stddev(test, 50, 300), 1e-6); // At end should have two 6 one 100 and three 12
	}
	
	// TODO Should be more thoroughly tested... sorry
	@Test
	public void logRatioTest() {
		List<SparseElement<Double>> test = ImmutableList.of(SparseElement.create(0, 6d), SparseElement.create(100, 100d), SparseElement.create(160, 12d));
		assertEquals(0.49012907173427367, Sparse.logRatioStddev(test, 100, 300), 0); // 6 12 12
		assertEquals(1.7546158212936742, Sparse.logRatioStddev(test, 50, 300), 0); // 6 6 100 12 12 12
	}
	
	@Test
	public void emptyRmsdTest() {
		List<SparseElement<Double>> a = ImmutableList.of();
		List<SparseElement<Double>> b = ImmutableList.of(SparseElement.create(0, 5d));
		List<SparseElement<Double>> c = ImmutableList.of(SparseElement.create(100, 5d));
		assertEquals(Double.NaN, Sparse.rmsd(a, b, 1, 100), 1e-6);
		assertEquals(Double.NaN, Sparse.rmsd(b, a, 1, 100), 1e-6);
		assertEquals(Double.NaN, Sparse.rmsd(a, b, 10, 100), 1e-6);
		assertEquals(Double.NaN, Sparse.rmsd(a, b, 1000, 100), 1e-6);
		assertEquals(Double.NaN, Sparse.rmsd(a, b, 1, -1), 1e-6);
		assertEquals(Double.NaN, Sparse.rmsd(b, c, 1, 99), 1e-6);
	}
	
	@Test
	public void negativeCountRmsdTest() {
		List<SparseElement<Double>> a = ImmutableList.of(SparseElement.create(0, 5d), SparseElement.create(20, 7d));
		List<SparseElement<Double>> b = ImmutableList.of(SparseElement.create(100, 5d));
		assertEquals(2, Sparse.rmsd(a, b, 1, 101), 1e-6);
	}
	
	// TODO Should be more thoroughly tested... sorry
	@Test
	public void rmsdTest() {
		List<SparseElement<Double>> a = ImmutableList.of(
				SparseElement.create(40, 6d),
				SparseElement.create(149, 10d), // This will get sampled at freq 50
				SparseElement.create(170, 16d),
				SparseElement.create(190, 12d),
				SparseElement.create(200, 8d)); // This will just miss the sample at 199
		List<SparseElement<Double>> b = ImmutableList.of(
				SparseElement.create(0, 12d),
				SparseElement.create(60, 9d),
				SparseElement.create(210, 6d));
		assertEquals(Math.sqrt(22d/3), Sparse.rmsd(a, b, 100, 300), 1e-6); // 6,9 - 12,9 - 8,6 
		assertEquals(Math.sqrt(63d/6), Sparse.rmsd(a, b, 50, 300), 1e-6); // 6,12 - 6,9 - 10,9 - 12,9 - 8,6 - 8,6
	}
	
	@Test
	public void rmsdIgnoreNanTest() {
		List<SparseElement<Double>> a = ImmutableList.of(
				SparseElement.create(0, 8d),
				SparseElement.create(1, 4d),
				SparseElement.create(2, 3d));
		List<SparseElement<Double>> b = ImmutableList.of(
				SparseElement.create(0, 5d),
				SparseElement.create(1, Double.NaN),
				SparseElement.create(2, 7d));
		assertEquals(Math.sqrt(25d/2), Sparse.rmsd(a, b, 1, 3), 1e-6);
		assertEquals(Math.sqrt(25d/2), Sparse.rmsd(b, a, 1, 3), 1e-6);
	}
	
	@Test
	public void symmetricRmsdTest() {
		for (int i = 0; i < 100; ++i) {
			List<SparseElement<Double>> a = randSparseList();
			List<SparseElement<Double>> b = randSparseList();
			long length = rand.nextInt(1500) - 1;
			long period = rand.nextInt(200) + 1;
			assertEquals(Sparse.rmsd(a, b, period, length), Sparse.rmsd(b, a, period, length), 1e-6);
		}
	}
	
	@Test
	public void sparseElementEqualsTest() {
		assertEquals(SparseElement.create(100, 5d), SparseElement.create(100, 5d));
		assertNotEquals(SparseElement.create(100, 5d), SparseElement.create(99, 5d));
		assertNotEquals(SparseElement.create(100, 5d), SparseElement.create(100, 6d));
		assertNotEquals(SparseElement.create(100, 5d), new Object());
		assertNotEquals(SparseElement.create(100, 5d), null);
	}
	
	@Test
	public void sparseElementToStringTest() {
		assertEquals("5.0 @ 100", SparseElement.create(100, 5d).toString());
	}

	public static List<SparseElement<Double>> randSparseList() {
		long index = -1;
		int length = rand.nextInt(100) + 100;
		Builder<SparseElement<Double>> builder = ImmutableList.builder();
		for (int i = 0; i < length; ++i) {
			index += rand.nextInt(10) + 1;
			builder.add(SparseElement.create(index, rand.nextDouble()));
		}
		return builder.build();
	}
	
	// This exists solely to claim 100% coverage on a class that only contains methods
	@SuppressWarnings("unused")
	@Test
	public void coverageTest() {
		new Sparse();
	}
	
}
