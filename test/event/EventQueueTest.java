package event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.Test;

import activity.Activity;
import activity.MockActivity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class EventQueueTest {

	@Test
	public void basicUsageTest() {
		EventQueue q = new EventQueue();
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
		
		Activity first = new MockActivity(0);
		q.add(first);
		assertFalse(q.isEmpty());
		assertEquals(1, q.size());
		assertEquals(first, q.peek());
		
		assertEquals(first, q.remove());
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
		
		Activity second = new MockActivity(1);
		Activity third = new MockActivity(2);
		q.add(third);
		q.add(second);
		assertEquals(second, q.peek());
		assertEquals(2, q.size());
		
		Activity inf1 = new MockActivity(TimeStamp.IMMEDIATE);
		Activity inf2 = new MockActivity(TimeStamp.IMMEDIATE);
		q.add(inf1);
		assertEquals(3, q.size());
		assertEquals(inf1, q.poll());
		assertEquals(2, q.size());
		q.add(inf2);
		assertEquals(3, q.size());
		assertEquals(inf2, q.poll());
		assertEquals(2, q.size());
		
		assertEquals(second, q.peek());
		q.addAll(ImmutableList.of(inf1, inf2));
		assertEquals(4, q.size());
		q.poll();
		q.poll();
		assertEquals(second, q.peek());
		assertEquals(2, q.size());
		
		assertEquals(second, q.poll());
		assertEquals(third, q.poll());
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
	}
	
	@Test
	public void emptyPeekTest() {
		assertEquals(null, new EventQueue().peek());
	}
	
	@Test
	public void emptyPollTest() {
		assertEquals(null, new EventQueue().poll());
	}
	
	@Test (expected = NoSuchElementException.class)
	public void emptyElementTest() {
		new EventQueue().element();
	}
	
	@Test (expected = NoSuchElementException.class)
	public void emptyRemoveTest() {
		new EventQueue().remove();
	}
	
	@Test
	public void clearTest() {
		EventQueue q = new EventQueue();
		q.addAll(ImmutableList.of(new MockActivity(0), new MockActivity(1),
				new MockActivity(2)));
		assertFalse(q.isEmpty());
		q.clear();
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void pollTest() {
		EventQueue q = new EventQueue();

		Activity first = new MockActivity(0);
		Activity second = new MockActivity(1);
		Activity third = new MockActivity(2);

		q.addAll(ImmutableList.of(first, second, third));
		// Check that poll will return activities in correct order & update size
		assertEquals(first, q.poll());
		assertEquals(2, q.size());
		assertEquals(second, q.poll());
		assertEquals(1, q.size());
		assertEquals(third, q.poll());
		assertTrue(q.isEmpty());
		assertEquals(null, q.poll());
	}
	
	@Test
	public void addAllTest() {
		EventQueue q = new EventQueue();
			
		Activity first = new MockActivity(0);
		Activity second = new MockActivity(1);
		Activity third = new MockActivity(2);
		
		assertEquals("Incorrect initial size", q.size(), 0);
		q.addAll(ImmutableList.of(first, second));
		
		// Verify activities added correctly
		assertEquals("Size not updated", q.size(), 2);
		assertEquals(first, q.poll());
		assertEquals(second, q.poll());
		assertTrue(q.isEmpty());
		
		// Verify correct order with list of activities not in chronological order
		q.addAll(ImmutableList.of(third, second));
		assertEquals("Size not updated", q.size(), 2);
		assertEquals(second, q.poll());
		assertEquals(third, q.poll());
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void addImmediateTest() {
		EventQueue q = new EventQueue();
		
		Activity first = new MockActivity(TimeStamp.IMMEDIATE);
		Activity second = new MockActivity(TimeStamp.IMMEDIATE);
		Activity third = new MockActivity(TimeStamp.IMMEDIATE);
		Activity fourth = new MockActivity(TimeStamp.ZERO);
		
		List<Activity> list = Arrays.asList(third, first);
		
		q.addAll(list);
		// Verify that third always will be at top of queue, since immediate
		assertEquals("Size not updated", q.size(), 2);
		assertEquals(third, q.peek());
				
		q.add(second);
		assertEquals("Size not updated", q.size(), 3);
		q.add(fourth);
		assertEquals("Size not updated", q.size(), 4);
		
		// Verify that order correct (LIFO for immediate)
		assertEquals(second, q.poll());
		assertEquals(third, q.poll());
		assertEquals(first, q.poll());
		assertEquals(fourth, q.poll());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			addImmediateTest();
		}
	}
	
	
	@Test
	public void toArrayTest() {
		EventQueue q = new EventQueue();

		List<? extends Activity> acts = ImmutableList.of(new MockActivity(0),
				new MockActivity(1), new MockActivity(2));
		q.addAll(acts);
		for (Object o : q.toArray()) {
			assertTrue(acts.contains(o));
		}
		
		for (Activity a : q.toArray(new Activity[0])) {
			assertTrue(acts.contains(a));
		}
	}
	
	@Test
	public void iteratorTest() {
		EventQueue q = new EventQueue();

		Activity first = new MockActivity(0);
		Activity second = new MockActivity(1);
		Activity third = new MockActivity(2);
		List<Activity> acts = ImmutableList.of(first, second, third);

		q.addAll(acts);
		for (Activity a : q) {
			assertTrue(acts.contains(a));
		}
		
		assertEquals(3, q.size());
	}
	
	public void randomDeterminismTest() {
		Random rand = new Random();
		long seed = rand.nextLong();
		EventQueue q1 = new EventQueue(new Random(seed));
		EventQueue q2 = new EventQueue(new Random(seed));
		
		Builder<Activity> builder = ImmutableList.builder();
		for (int i = 0; i < 1000; i++) {
			builder.add(new MockActivity(rand.nextInt(100)));
		}
		List<Activity> acts = builder.build();
		q1.addAll(acts);
		q2.addAll(acts);
		
		while (!q1.isEmpty())
			assertEquals(q1.remove(), q2.remove());
	}
}
