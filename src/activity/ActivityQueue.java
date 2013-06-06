package activity;

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

import event.Event;
import event.TimeStamp;

/**
 * @author ewah
 * 
 *         This is essentially a wrapper class for PriorityQueue, where priority
 *         is the TimeStamp at which an event occurs.
 */
public class ActivityQueue implements Queue<Activity> {

	// TODO Add Random Number Handler
	// TODO Don't allow null activities
	// TODO Don't allow time stamps below current time unless = inf time
	// TODO Switch PriorityQueue to HashPriorityQueue so we get constant time removal...

	// Invariant that no event is ever empty at the end of execution.
	// Note: Current implementation means instantTime stuff gets executed in a
	// random order as well... May not be what we want, but if it is it provides
	// a much simpler implementation.

	protected PriorityQueue<Event> eventQueue;
	protected HashMap<TimeStamp, Event> eventIndex;
	protected int size;

	/**
	 * Constructor for the EventQueue
	 */
	public ActivityQueue() {
		this(8);
	}

	/**
	 * Constructor for the EventQueue where capacity is specified by a
	 * parameter.
	 * 
	 * @param capacity
	 */
	public ActivityQueue(int capacity) {
		eventQueue = new PriorityQueue<Event>(capacity);
		eventIndex = new HashMap<TimeStamp, Event>(capacity);
		size = 0;
	}

	@Override
	public boolean isEmpty() {
		return eventQueue.isEmpty();
	}

	@Override
	public String toString() {
		// This is super slow!
		StringBuilder sb = new StringBuilder("Q: ");
		List<Event> copy = new ArrayList<Event>(eventQueue);
		Collections.sort(copy);
		for (Event e : copy)
			sb.append(e).append("// ");
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

	@Override
	public Iterator<Activity> iterator() {
		return new ActivityQueueIterator(eventQueue.iterator());
	}

	@Override
	public boolean remove(Object o) {
		// Super slow
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

	@Override
	public boolean removeAll(Collection<?> c) {
		// Super inefficient
		boolean modified = false;
		for (Object o : c)
			modified |= remove(o);
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// Super inefficient
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

	@Override
	public Object[] toArray() {
		// Not sorted, maybe not an issue...
		Object[] array = new Object[size];
		int start = 0;
		for (Event e : eventQueue) {
			System.arraycopy(e.toArray(), 0, array, start, e.size());
			start += e.size();
		}
		return array;
	}

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
			e = new Event(t);
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
			throw new NoSuchElementException("ActivityQueue is empty");
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
	
	protected static class ActivityQueueIterator implements Iterator<Activity> {

		protected Iterator<Event> eventIterator;
		protected Iterator<Activity> activityIterator;
		
		protected ActivityQueueIterator(Iterator<Event> events) {
			eventIterator = events;
			activityIterator = Collections.emptyIterator();
		}
		
		@Override
		public boolean hasNext() {
			return eventIterator.hasNext() || activityIterator.hasNext(); 
		}

		@Override
		public Activity next() {
			if (!activityIterator.hasNext())
				activityIterator = eventIterator.next().iterator();
			return activityIterator.next();
		}

		@Override
		public void remove() {
			activityIterator.remove();
		}
		
	}

}
