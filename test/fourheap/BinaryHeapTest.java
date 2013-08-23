package fourheap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

public class BinaryHeapTest {

	protected final static Random rand = new Random();

	@Test
	public void emptyConstructorTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		assertTrue(a.isEmpty());
	}

	@Test
	public void peekTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		assertEquals(null, a.peek());
		assertEquals(0, a.size());
		a.add(4);
		assertEquals(4, (int) a.peek());
		assertEquals(1, a.size());
		a.add(2);
		assertEquals(2, (int) a.peek());
		assertEquals(2, a.size());
		a.add(8);
		assertEquals(2, (int) a.peek());
		assertEquals(3, a.size());
	}

	@Test
	public void addTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		List<Integer> nums = randomInts(1000);
		for (int i : nums)
			a.offer(i);
		Collections.sort(nums);
		for (int i : nums)
			assertEquals(i, (int) a.poll());
	}
	
	@Test
	public void addAllTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		List<Integer> nums = randomInts(1000);
		a.addAll(nums);
		Collections.sort(nums);
		for (int i : nums)
			assertEquals(i, (int) a.poll());
	}
	
	@Test
	public void removeTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		List<Integer> nums = randomInts(1000);
		a.addAll(nums);
		assertTrue(a.containsAll(nums));
		Collections.shuffle(nums);
		List<Integer> removed = nums.subList(0, 100);
		List<Integer> left = nums.subList(100, nums.size());
		for (int i : removed)
			assertTrue(a.remove(i));
		Collections.sort(left);
		for (int i : left)
			assertEquals(i, (int) a.poll());
	}
	
	@Test
	public void removeTest2() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		Set<Integer> nums = Sets.newHashSet(randomInts(1000));
		a.addAll(nums);
		assertTrue(a.containsAll(nums));
		
		for (Iterator<Integer> it = a.iterator(); it.hasNext();) {
			nums.remove(it.next());
			if (rand.nextBoolean()) it.remove();
		}
		assertTrue(nums.isEmpty());
	}
	
	@Test
	public void removeAllTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		List<Integer> nums = randomInts(1000);
		a.addAll(nums);
		assertTrue(a.containsAll(nums));
		assertTrue(a.removeAll(nums));
		assertTrue(a.isEmpty());
		for (int i : nums)
			assertFalse(a.contains(i));
	}
	
	@Test
	public void removeAllTest2() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		List<Integer> nums = randomInts(1000);
		a.addAll(nums);
		assertTrue(a.containsAll(nums));
		Collections.shuffle(nums);
		List<Integer> removed = nums.subList(0, 100);
		List<Integer> left = nums.subList(100, nums.size());
		assertTrue(a.removeAll(removed));
		Collections.sort(left);
		for (int i : left)
			assertEquals(i, (int) a.poll());
	}
	
	@Test
	public void cycleTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		List<Integer> nums = randomInts(1000);
		a.addAll(nums);
		assertTrue(a.containsAll(nums));
		assertEquals(1000, a.size());
		
		for (int i = 0; i < 100; i++) {
			Collections.shuffle(nums);
			List<Integer> removed = nums.subList(0, 100);
			List<Integer> left = nums.subList(100, nums.size());
			assertTrue(a.removeAll(removed));
			assertTrue(a.containsAll(left));
			assertEquals(900, a.size());
			
			nums = randomInts(100);
			a.addAll(nums);
			nums.addAll(left);
			assertTrue(a.containsAll(nums));
			assertEquals(1000, a.size());
		}
		Collections.sort(nums);
		for (int i : nums)
			assertEquals(i, (int) a.poll());
	}
	
	@Test
	public void comparatorTest() {
		BinaryHeap<Integer> a = BinaryHeap.create(Ordering.<Integer> natural().reverse());
		List<Integer> nums = randomInts(1000);
		a.addAll(nums);
		Collections.sort(nums, Collections.reverseOrder());
		for (int i : nums)
			assertEquals(i, (int) a.poll());
	}
	
	@Test
	public void iteratorTest() {
		BinaryHeap<Integer> a = BinaryHeap.create(Ordering.<Integer> natural().reverse());
		Set<Integer> nums = new HashSet<Integer>(randomInts(1000));
		a.addAll(nums);
		for (int i : a)
			assertTrue(nums.contains(i));
	}
	
	@Test (expected = NoSuchElementException.class)
	public void elementTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		a.element();
	}
	
	@Test (expected = NoSuchElementException.class)
	public void removeExceptionTest() {
		BinaryHeap<Integer> a = BinaryHeap.<Integer>create();
		a.remove();
	}

	protected List<Integer> randomInts(int num) {
		List<Integer> collection = Lists.newArrayList();
		for (int i = 0; i < num; i++)
			collection.add(rand.nextInt());
		return collection;
	}

}
