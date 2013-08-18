package event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.Test;

import activity.Activity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterators;

public class EventQueueTest {
	
	@Test
	public void basicUsageTest() {
		EventQueue q = new EventQueue();
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
		
		Activity first = new DummyActivity(0);
		q.add(first);
		assertFalse(q.isEmpty());
		assertEquals(1, q.size());
		assertEquals(first, q.peek());
		
		assertEquals(first, q.remove());
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
		
		Activity second = new DummyActivity(1);
		Activity third = new DummyActivity(2);
		q.add(third);
		q.add(second);
		assertEquals(second, q.peek());
		assertEquals(2, q.size());
		
		Activity inf1 = new DummyActivity(TimeStamp.IMMEDIATE);
		Activity inf2 = new DummyActivity(TimeStamp.IMMEDIATE);
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
		q.addAll(ImmutableList.of(new DummyActivity(0), new DummyActivity(1),
				new DummyActivity(2)));
		assertFalse(q.isEmpty());
		q.clear();
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void removeTest() {
		EventQueue q = new EventQueue();

		Activity first = new DummyActivity(0);
		Activity second = new DummyActivity(1);
		Activity third = new DummyActivity(2);

		q.addAll(ImmutableList.of(first, second, third));
		assertTrue(q.remove(second));
		assertEquals(first, q.poll());
		assertEquals(third, q.poll());
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void removeAllTest() {
		EventQueue q = new EventQueue();

		Activity first = new DummyActivity(0);
		Activity second = new DummyActivity(1);
		Activity third = new DummyActivity(2);

		q.addAll(ImmutableList.of(first, second, third));
		assertTrue(q.removeAll(ImmutableList.of(first, second)));
		assertEquals(third, q.poll());
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void retainAllTest() {
		EventQueue q = new EventQueue();

		Activity first = new DummyActivity(0);
		Activity second = new DummyActivity(1);
		Activity third = new DummyActivity(2);

		q.addAll(ImmutableList.of(first, second, third));
		assertTrue(q.retainAll(ImmutableList.of(second)));
		assertEquals(second, q.poll());
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void toArrayTest() {
		EventQueue q = new EventQueue();

		List<? extends Activity> acts = ImmutableList.of(new DummyActivity(0),
				new DummyActivity(1), new DummyActivity(2));
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

		Activity first = new DummyActivity(0);
		Activity second = new DummyActivity(1);
		Activity third = new DummyActivity(2);
		List<Activity> acts = ImmutableList.of(first, second, third);

		q.addAll(acts);
		for (Activity a : q) {
			assertTrue(acts.contains(a));
		}
		
		assertEquals(3, q.size());
		for (Iterator<Activity> it = q.iterator(); it.hasNext();)
			if (it.next() == second)
				it.remove();
		assertEquals(2, q.size());
		assertFalse(q.contains(second));
		
		assertEquals(2, Iterators.size(q.iterator()));
		
		assertFalse(second == q.poll());
		assertFalse(second == q.poll());
		assertTrue(q.isEmpty());
	}
	
	public void randomDeterminismTest() {
		Random rand = new Random();
		long seed = rand.nextLong();
		EventQueue q1 = new EventQueue(new Random(seed));
		EventQueue q2 = new EventQueue(new Random(seed));
		
		Builder<Activity> builder = ImmutableList.builder();
		for (int i = 0; i < 1000; i++) {
			builder.add(new DummyActivity(rand.nextInt(100)));
		}
		List<Activity> acts = builder.build();
		q1.addAll(acts);
		q2.addAll(acts);
		
		while (!q1.isEmpty())
			assertEquals(q1.remove(), q2.remove());
	}

	private static class DummyActivity extends Activity {

		public DummyActivity(TimeStamp t) {
			super(t);
		}
		
		public DummyActivity(long t) {
			this(new TimeStamp(t));
		}

		@Override
		public Collection<? extends Activity> execute(TimeStamp time) {
			return ImmutableList.of();
		}
		
	}
	
}
