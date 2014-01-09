package event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Queues;

import utils.RandomQueue;
import activity.Activity;

/**
 * Class representing an event in time. Each event is really a queue of queues of activities. The
 * inner queues are always FIFO, the outer queue is LIFO for infinite time and Random for any other
 * time. The purpose of this organization is to enforce the invariant that added collections are
 * always executed in the order they are added. e.g. if you add a collection of two activities
 * [WithdrawOrder, SubmitOrder], the WithdrawOrder will always be executed before the SubmitOrder,
 * although other activities may be polled inbetween the two.
 * 
 * @author ebrink
 */
public class Event extends AbstractQueue<Activity> implements Comparable<Event> {

	protected final TimeStamp eventTime;
	// Is a LiFo queue for infinite time activities, and a random queue otherwise
	protected final Queue<Queue<Activity>> backedQueue;
	protected int size;

	public Event(TimeStamp time) {
		this(time, new Random());
	}

	public Event(TimeStamp time, Random seed) {
		eventTime = checkNotNull(time, "Time");
		size = 0;
		if (time.equals(TimeStamp.IMMEDIATE))
			backedQueue = Collections.asLifoQueue(Queues.<Queue<Activity>> newArrayDeque());
		else
			backedQueue = RandomQueue.create(seed);
	}

	public TimeStamp getTime() {
		return eventTime;
	}

	@Override
	public boolean addAll(Collection<? extends Activity> acts) {
		return addAll((Iterable<? extends Activity>) acts);
	}
	
	public boolean addAll(Iterable<? extends Activity> acts) {
		if (Iterables.isEmpty(acts)) return false;
		for (Activity act : acts)
			checkArgument(eventTime.equals(act.getTime()),
					"Can't add an activity that doesn't share the time of the event");
		
		Queue<Activity> newQueue = Queues.newArrayDeque(acts);
		size += newQueue.size();
		return backedQueue.add(newQueue);
	}

	@Override
	public boolean offer(Activity e) {
		return addAll(ImmutableList.of(e));
	}

	@Override
	public Activity poll() {
		if (backedQueue.isEmpty()) return null;
		Queue<Activity> seq = backedQueue.poll();
		Activity ret = seq.poll();
		size--;
		if (!seq.isEmpty()) backedQueue.offer(seq);
		return ret;
	}

	@Override
	public Activity peek() {
		if (backedQueue.isEmpty()) return null;
		return backedQueue.peek().peek();
	}

	@Override
	public Iterator<Activity> iterator() {
		// TODO Remove from this iterator won't update size appropriately
		return Iterators.unmodifiableIterator(Iterables.concat(backedQueue).iterator());
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int compareTo(Event e) {
		checkNotNull(e, "Event");
		return getTime().compareTo(e.getTime());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Event)) return false;
		Event that = (Event) obj;
		return backedQueue.equals(that.backedQueue) && eventTime.equals(that.eventTime);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), eventTime);
	}

	@Override
	public String toString() {
		return eventTime + " | " + super.toString();
	}	

}
