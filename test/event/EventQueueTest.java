package event;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.Test;

import activity.Activity;

import systemmanager.Consts;

import event.EventQueue;
import event.TimeStamp;

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
		
		Activity inf1 = new DummyActivity(Consts.INF_TIME);
		Activity inf2 = new DummyActivity(Consts.INF_TIME);
		q.add(inf1);
		assertEquals(3, q.size());
		assertEquals(inf1, q.poll());
		assertEquals(2, q.size());
		q.add(inf2);
		assertEquals(3, q.size());
		assertEquals(inf2, q.poll());
		assertEquals(2, q.size());
		
		assertEquals(second, q.peek());
		q.addAll(Arrays.asList(new Activity[] { inf1, inf2 }));
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
		q.addAll(Arrays.asList(new Activity[] { new DummyActivity(0),
				new DummyActivity(1), new DummyActivity(2) }));
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

		q.addAll(Arrays.asList(new Activity[] { first, second, third }));
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

		q.addAll(Arrays.asList(new Activity[] { first, second, third }));
		assertTrue(q.removeAll(Arrays.asList(new Activity[] { first, second })));
		assertEquals(third, q.poll());
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void retainAllTest() {
		EventQueue q = new EventQueue();

		Activity first = new DummyActivity(0);
		Activity second = new DummyActivity(1);
		Activity third = new DummyActivity(2);

		q.addAll(Arrays.asList(new Activity[] { first, second, third }));
		assertTrue(q.retainAll(Arrays.asList(new Activity[] { second })));
		assertEquals(second, q.poll());
		assertTrue(q.isEmpty());
	}
	
	@Test
	public void toArrayTest() {
		EventQueue q = new EventQueue();

		List<Activity> acts = Arrays.asList(new Activity[] { new DummyActivity(0),
				new DummyActivity(1), new DummyActivity(2) });
		q.addAll(acts);
		Object[] arrayO = q.toArray();
		for (Object o : arrayO) {
			assertTrue(acts.contains(o));
		}
		
		Activity[] arrayA = q.toArray(new Activity[0]);
		for (Activity a : arrayA) {
			assertTrue(acts.contains(a));
		}
	}
	
	@Test
	public void iteratorTest() {
		EventQueue q = new EventQueue();

		Activity first = new DummyActivity(0);
		Activity second = new DummyActivity(1);
		Activity third = new DummyActivity(2);
		List<Activity> acts = Arrays.asList(new Activity[] { first, second, third });

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
		
		int bruteSize = 0;
		for (@SuppressWarnings("unused") Activity a : q)
			bruteSize++;
		assertEquals(2, bruteSize);
		
		assertFalse(second == q.poll());
		assertFalse(second == q.poll());
		assertTrue(q.isEmpty());
	}
	
	public void randomDeterminismTest() {
		Random rand = new Random();
		long seed = rand.nextLong();
		EventQueue q1 = new EventQueue(seed);
		EventQueue q2 = new EventQueue(seed);
		
		List<Activity> acts = new ArrayList<Activity>();
		for (int i = 0; i < 1000; i++) {
			acts.add(new DummyActivity(rand.nextInt(100)));
		}
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
		public Collection<Activity> execute() {
			return Collections.emptyList();
		}

		@Override
		public Activity deepCopy() {
			return new DummyActivity(time);
		}
		
	}
	
}
