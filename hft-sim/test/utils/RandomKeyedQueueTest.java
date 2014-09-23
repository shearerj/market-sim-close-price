package utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.math.LongMath;

public class RandomKeyedQueueTest {
	
	private RandomKeyedQueue<Integer, Integer> queue;
	
	@Before
	public void setup() {
		queue = RandomKeyedQueue.create();
	}

	@Test
	public void basicTest() {
		assertTrue(queue.isEmpty());
		assertEquals(0, queue.size());
		
		queue.add(1, 1);
		assertFalse(queue.isEmpty());
		assertEquals(1, queue.size());
		assertEquals(1, (int) queue.peek().getValue());
		
		assertEquals(1, (int) queue.remove().getValue());
		assertTrue(queue.isEmpty());
		assertEquals(0, queue.size());
		
		queue.add(3, 3);
		queue.add(2, 2);
		assertEquals(2, (int) queue.peek().getValue());
		assertEquals(2, queue.size());
		
		assertEquals(2, (int) queue.poll().getValue());
		assertEquals(3, (int) queue.poll().getValue());
		assertTrue(queue.isEmpty());
		assertEquals(0, queue.size());
	}
	
	/** Test that items added individually come out randomly ordered */
	@Test
	public void randomOrderingTest() {
		int n = 3;
		Set<Integer> listHashes = Sets.newHashSet();
		
		for (int i = 0; i < 1000; ++i) {
			RandomKeyedQueue<Integer, Integer> queue = RandomKeyedQueue.create();
			for (int j = 0; j < n; ++j)
				queue.add(0, j);
			Builder<Integer> builder = ImmutableList.builder();
			while (!queue.isEmpty())
				builder.add(queue.poll().getValue());
			listHashes.add(builder.build().hashCode());
		}
		
		assertEquals(LongMath.factorial(n), listHashes.size());
	}
	
	/** Test that items added together always come out in order */
	@Test
	public void addAllTest() {
		for (int i = 0; i < 1000; ++i) {
			RandomKeyedQueue<Integer, Integer> queue = RandomKeyedQueue.create();
			List<Integer> list = ImmutableList.copyOf(Iterators.limit(Iterators2.counter(), 10));
			queue.addAll(0, list);
			Builder<Integer> builder = ImmutableList.builder();
			while (!queue.isEmpty())
				builder.add(queue.poll().getValue());
			assertEquals(list, builder.build());
		}
	}
	
	/** Test that mixed keys get ordered appropraitely */
	@Test
	public void mixedTest() {
		Set<Integer> listHashes = Sets.newHashSet();
		
		for (int i = 0; i < 1000; ++i) {
			RandomKeyedQueue<Integer, Integer> queue = RandomKeyedQueue.create();
			queue.add(0, 0);
			queue.addAll(0, ImmutableList.of(1, 2));
			Builder<Integer> builder = ImmutableList.builder();
			while (!queue.isEmpty())
				builder.add(queue.poll().getValue());
			listHashes.add(builder.build().hashCode());
		}
		
		// Three ways to order them if 1 and 2 must come in order
		assertEquals(3, listHashes.size());
	}
	
	@Test
	public void emptyPeekTest() {
		assertEquals(null, queue.peek());
	}
	
	@Test
	public void emptyPollTest() {
		assertEquals(null, queue.poll());
	}
	
	@Test (expected = NoSuchElementException.class)
	public void emptyElementTest() {
		queue.element();
	}
	
	@Test (expected = NoSuchElementException.class)
	public void emptyRemoveTest() {
		queue.remove();
	}
	
	@Test
	public void clearTest() {
		queue.addAll(0, ImmutableList.of(1, 2, 3));
		assertFalse(queue.isEmpty());
		queue.clear();
		assertTrue(queue.isEmpty());
	}
	
	@Test
	public void pollTest() {
		List<Integer> list = Lists.newArrayList(1, 2, 3);
		for (int i = 0; i < 1000; ++i) {
			Collections.shuffle(list);
			for (int j : list)
				queue.add(j, j);

			// Check that poll will return activities in correct order & update size
			assertEquals(1, (int) queue.poll().getValue());
			assertEquals(2, queue.size());
			assertEquals(2, (int) queue.poll().getValue());
			assertEquals(1, queue.size());
			assertEquals(3, (int) queue.poll().getValue());
			assertTrue(queue.isEmpty());
			assertEquals(null, queue.poll());
		}
	}
	
	@Test
	public void iteratorTest() {
		List<Integer> acts = ImmutableList.of(1, 2, 3);

		queue.addAll(0, acts);
		for (Entry<Integer, Integer> a : queue)
			assertTrue(acts.contains(a.getValue()));
		
		assertEquals(3, queue.size());
	}
	
}
