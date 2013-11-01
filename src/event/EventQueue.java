package event;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Random;

import activity.Activity;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableListMultimap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

/**
 * The EventQueue is a a queue of upcoming events which are composed of
 * activities to be executed. The actual events are abstracted in this
 * implementation, so this functions as a queue of activities. The main methods
 * that should be used are "add," which adds a single activity to the queue,
 * "addAll," which adds a collection of activities to the queue, and "remove,"
 * which removes the activity at the head of the queue. Events that are added at
 * the same TimeStamp will be dequeued in a uniform random order. To make an
 * event occur instantaneously give it a time of TimeStamp.IMMEDIATE;
 * 
 * Note that because of the dequeuing mechanism, if Activity A is supposed to
 * happen after Activity B, Activity A should queue up Activity B. Anything else
 * may not guarantee that A always happens before B.
 * 
 * @author ebrink
 */
public class EventQueue extends AbstractQueue<Activity> {
	
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

	// TODO Keep modCount to detect concurrent modification exception
	
	protected PriorityQueue<Event> eventQueue;
	protected Map<TimeStamp, Event> eventIndex;
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
		eventIndex = Maps.newHashMapWithExpectedSize(capacity);
		size = 0;
		rand = seed;
	}

	public EventQueue() {
		this(new Random());
	}

	@Override
	public void clear() {
		eventQueue.clear();
		eventIndex.clear();
		size = 0;
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

	@Override
	public boolean offer(Activity act) {
		checkNotNull(act, "Activity");
		TimeStamp t = act.getTime();
		Event e = eventIndex.get(t);
		if (e == null) {
			// This makes all event's use the same random number generator. This
			// may a little chaotic, but at the moment I can't think of a meaningful way
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
	public Activity poll() {
		if (isEmpty()) return null;
		Activity act = eventQueue.element().remove();
		if (eventQueue.element().isEmpty())
			eventIndex.remove(eventQueue.remove().getTime());
		size--;
		return act;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Activity peek() {
		if (isEmpty()) return null;
		return eventQueue.peek().peek();
	}

	@Override
	public boolean addAll(Collection<? extends Activity> acts) {
		return addAll((Iterable<? extends Activity>) acts);
	}
	
	// Add collections in reverse. This ensures invariant.
	public boolean addAll(Iterable<? extends Activity> acts) {
		checkNotNull(acts);
		if (Iterables.isEmpty(acts)) return false;
		// Group Activities by Time
		Builder<TimeStamp, Activity> build = ImmutableListMultimap.builder();	// will not take nulls
		for (Activity act : acts) build.put(act.getTime(), act);
		
		// Add them all to the appropriate event as a collection to maintain execution orders 
		for (Entry<TimeStamp, Collection<Activity>> ent : build.build().asMap().entrySet()) {
			TimeStamp t = ent.getKey();
			Event e = eventIndex.get(t);
			if (e == null) {
				e = new Event(t, rand);
				eventIndex.put(t, e);
				eventQueue.add(e);
			}
			e.addAll(ent.getValue());
			size += ent.getValue().size();
		}
		return true;
	}

	@Override
	public String toString() {
		if (isEmpty()) return "[]";
		else if (eventQueue.size() == 1) return eventQueue.toString();
		else return "[" + eventQueue.peek() + ", ...]";
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
			activityIterator = Iterators.emptyIterator();
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
