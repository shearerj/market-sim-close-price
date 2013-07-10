package event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

import utils.CollectionUtils;

import activity.Activity;

/**
 * The EventQueue is a a queue of upcoming events which are composed of
 * activities to be executed. The actual events are abstracted in this
 * implementation, so this functions as a queue of activities. The main methods
 * that should be used are "add," which adds a single activity to the queue,
 * "addAll," which adds a collection of activities to the queue, and "remove,"
 * which removes the activity at the head of the queue. Events that are added at
 * the same TimeStamp will be dequeued in a uniform random order. To make an event
 * occur instantaneously give it a time of Consts.INF_TIME.
 * 
 * Note that because of the dequeuing mechanism, if Activity A is supposed to
 * happen after Activity B, Activity A should queue up Activity B. Anything else
 * may not guarantee that A always happens before B.
 * 
 * @author ebrink
 * 
 * 
 */
public class EventQueue implements Queue<Activity> {

	// TODO Don't allow null activities / Handled kind of by Event...
	// TODO Don't allow time stamps below current time unless = inf time
	// TODO Switch PriorityQueue to HashPriorityQueue so we get constant time
	// removal...

	// Invariant that no event is ever empty at the end of execution.
	//
	// Note: Current implementation means instantTime stuff gets executed in a
	// random order as well... May not be what we want, but if it is it provides
	// a much simpler implementation.
	//
	// In general the rule should be, if one activity comes logically after
	// another activity it should be scheduled by the activity that always
	// proceeds it. Activities scheduled at the same time (even infinitely fast)
	// may occur in any order.

	protected PriorityQueue<Event> eventQueue;
	protected HashMap<TimeStamp, Event> eventIndex;
	protected int size;
	protected Random rand;

	public EventQueue(Random seed) {
		this(8, seed);
	}

	public EventQueue(int capacity) {
		this(capacity, new Random());
	}

	public EventQueue(int capacity, Random seed) {
		eventQueue = new PriorityQueue<Event>(capacity);
		eventIndex = new HashMap<TimeStamp, Event>(capacity);
		size = 0;
		rand = seed;
	}

	public EventQueue() {
		this(new Random());
	}

	@Override
	public boolean isEmpty() {
		return eventQueue.isEmpty();
	}

	/**
	 * This method can be very inefficient, and shouldn't be used if it can be
	 * helped.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Q: ");
		List<Event> copy = new ArrayList<Event>(eventQueue);
		Collections.sort(copy);
		for (Event e : copy)
			sb.append(e).append(".  ");
		return sb.substring(0, sb.length() - 3);
	}

	@Override
	public boolean addAll(Collection<? extends Activity> c) {
		boolean modified = false;
		for (Activity act : c)
			modified |= add(act);
		return modified;
	}

	@Override
	public void clear() {
		eventQueue.clear();
		eventIndex.clear();
	}

	@Override
	public boolean contains(Object o) {
		if (o == null || !(o instanceof Activity))
			return false;
		Event e = eventIndex.get(((Activity) o).getTime());
		return e != null && e.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c)
			if (!contains(o))
				return false;
		return true;
	}

	/**
	 * This iterator is not sorted by time, but within a given time, the
	 * activities are in the random order they will be drawn from.
	 */
	@Override
	public Iterator<Activity> iterator() {
		return new EventQueueIterator(eventQueue.iterator());
	}

	/**
	 * This method can be very inefficient, and shouldn't be used if it can be
	 * helped.
	 */
	@Override
	public boolean remove(Object o) {
		if (o == null || !(o instanceof Activity))
			return false;
		Activity act = (Activity) o;
		Event e = eventIndex.get(act.getTime());
		if (e == null)
			return false;
		boolean modified = e.remove(act);
		if (modified)
			size--;
		if (e.isEmpty()) {
			eventIndex.remove(act.getTime());
			eventQueue.remove(e);
		}
		return modified;
	}

	/**
	 * This method can be very inefficient, and shouldn't be used if it can be
	 * helped.
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		for (Object o : c)
			modified |= remove(o);
		return modified;
	}

	/**
	 * This method can be very inefficient, and shouldn't be used if it can be
	 * helped.
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = false;
		for (Iterator<Event> it = eventQueue.iterator(); it.hasNext();) {
			Event e = it.next();
			size -= e.size();
			modified |= e.retainAll(c);
			size += e.size();
			if (e.isEmpty()) {
				it.remove();
				eventIndex.remove(e.getTime());
			}
		}
		return modified;
	}

	/**
	 * The elements in this array are not sorted.
	 */
	@Override
	public Object[] toArray() {
		Object[] array = new Object[size];
		int start = 0;
		for (Event e : eventQueue) {
			System.arraycopy(e.toArray(), 0, array, start, e.size());
			start += e.size();
		}
		return array;
	}

	/**
	 * The elements in this array are not sorted.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		if (a.length < size)
			return (T[]) Arrays.copyOf(toArray(), size, a.getClass());
		int start = 0;
		for (Event e : eventQueue) {
			T[] copy = (T[]) e.toArray(Arrays.copyOf(a, 0, a.getClass()));
			System.arraycopy(copy, 0, a, start, e.size());
			start += e.size();
		}
		return a;
	}

	@Override
	public boolean add(Activity act) {
		TimeStamp t = act.getTime();
		Event e = eventIndex.get(t);
		if (e == null) {
			// This makes all event's use the same random number generator. This
			// may a little chaotic, but at the moment I can't think of a way
			// around it.
			e = new Event(t, rand);
			eventIndex.put(t, e);
			eventQueue.add(e);
		}
		e.add(act);
		size++;
		return true;
	}

	@Override
	public Activity element() {
		if (isEmpty())
			throw new NoSuchElementException("ActivityQueue is empty");
		return peek();
	}

	@Override
	public boolean offer(Activity e) {
		return add(e);
	}

	@Override
	public Activity remove() {
		if (isEmpty())
			throw new NoSuchElementException(this.getClass().getSimpleName() +
					" is empty");
		return poll();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Activity peek() {
		if (isEmpty())
			return null;
		return eventQueue.peek().peek();
	}

	@Override
	public Activity poll() {
		if (isEmpty())
			return null;
		Activity act = eventQueue.element().remove();
		if (eventQueue.element().isEmpty()) {
			eventIndex.remove(eventQueue.remove().getTime());
		}
		size--;
		return act;
	}

	protected class EventQueueIterator implements Iterator<Activity> {

		protected Iterator<Event> eventIterator;
		protected Iterator<Activity> activityIterator;
		// These two booleans keep track of whether every activity found in the
		// event so far has been removed. If that's the case, and you remove an
		// activity when there are no more activities left in the event, then it
		// also removed the event. Thus preserving the invariant that there are
		// no empty events in the eventQueue.
		protected boolean removedEveryActivity;
		protected boolean removedCurrentActivity;

		protected EventQueueIterator(Iterator<Event> events) {
			eventIterator = events;
			activityIterator = CollectionUtils.emptyIterator();
			removedEveryActivity = true;
			removedCurrentActivity = false;
		}

		@Override
		public boolean hasNext() {
			return eventIterator.hasNext() || activityIterator.hasNext();
		}

		@Override
		public Activity next() {
			removedEveryActivity &= removedCurrentActivity;
			removedCurrentActivity = false;
			if (!activityIterator.hasNext()) {
				activityIterator = eventIterator.next().iterator();
				removedEveryActivity = true;
			}
			return activityIterator.next();
		}

		@Override
		public void remove() {
			activityIterator.remove();
			size--;
			removedCurrentActivity = true;
			if (removedEveryActivity && !activityIterator.hasNext())
				eventIterator.remove();
		}

	}

}
