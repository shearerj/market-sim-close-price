package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import utils.SparseIterator.SparseElement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.math.IntMath;

public class Iterables2Test {
	
	@Test
	public void duplicateTest() {
		List<SparseElement<Integer>> base = Lists.newArrayList();
		Iterable<Integer> test = Iterables.limit(Iterables2.toSparse(base), 100);
		
		for (Integer i : test)
			assertTrue(i == null);
		
		base.add(SparseElement.create(0, 153));
		for (int i : test)
			assertEquals(153, i);
	}
	
	@Test
	public void truncationTest() {
		List<SparseElement<Integer>> base = Lists.newArrayList();
		Iterable<Integer> test = Iterables2.toSparse(base);
		
		base.add(SparseElement.create(0, 153));
		base.add(SparseElement.create(100, 370));
		for (int i : Iterables.limit(test, 100))
			assertEquals(153, i);
		assertEquals(370, (int) Iterables.get(test, 101));
	}
	
	@Test
	public void denseSampleTest() {
		List<Integer> base;
		
		base = ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
		assertEquals(base, ImmutableList.copyOf(Iterables2.sample(base, 1, 0)));
		for (int offset = -2; offset <= 0; ++offset)
			assertEquals(base, ImmutableList.copyOf(Iterables2.sample(base, 1, offset)));
		
		assertEquals(ImmutableList.of(1, 3, 5, 7, 9), ImmutableList.copyOf(Iterables2.sample(base, 2, 0)));
		assertEquals(ImmutableList.of(1, 3, 5, 7, 9), ImmutableList.copyOf(Iterables2.sample(base, 2, -2)));
		assertEquals(ImmutableList.of(2, 4, 6, 8), ImmutableList.copyOf(Iterables2.sample(base, 2, -1)));
		assertEquals(ImmutableList.of(2, 4, 6, 8), ImmutableList.copyOf(Iterables2.sample(base, 2, 1)));
		
		assertEquals(ImmutableList.of(3, 5, 7, 9), ImmutableList.copyOf(Iterables2.sample(base, 2, 2)));
		for (int skip = 1; skip < 10; ++skip)
			assertEquals(ImmutableList.of(9), ImmutableList.copyOf(Iterables2.sample(base, skip, 8)));
		
		assertEquals(ImmutableList.of(1, 4, 7), ImmutableList.copyOf(Iterables2.sample(base, 3, 0)));
		assertEquals(ImmutableList.of(3, 6, 9), ImmutableList.copyOf(Iterables2.sample(base, 3, -1)));
	}
	
	@Test
	public void denseSampleRandomTest() {
		Random rand = new Random();
		List<Integer> sizes = ImmutableList
				.of(99, 100, 101, 102, 103, 104, 105);
		List<Double> base = Lists.newArrayList();
		for (int i = 0; i < 100; ++i) {
			for (int size : sizes) {
				base.clear();
				for (int j = 0; j < size; ++j)
					base.add(rand.nextDouble());

				for (int skip = 1; skip < 6; ++skip) {
					for (int offset = -skip + 1; offset < 100; ++offset) {
						Iterator<Double> test = Iterables2.sample(base, skip, offset).iterator();
						for (int k = offset < 0 ? IntMath.mod(offset, skip) : offset; k < base.size(); k += skip)
							assertEquals(base.get(k), test.next(), 0);
						assertFalse(test.hasNext());
					}
				}
			}
		}
	}

	@Test
	public void sparseSampleTest() {
		
	}

}
