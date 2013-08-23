package event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Queue;
import java.util.Random;

import com.google.common.base.Objects;
import com.google.common.collect.ForwardingQueue;
import com.google.common.collect.Queues;

import utils.RandomQueue;
import activity.Activity;

/**
 * Class representing an event in time. Each Event is a randomly ordered queue of activities and a
 * TimeStamp indicating when it is to occur.
 * 
 * Null activities may not be added to an Event
 * 
 * @author ebrink
 */
public class Event extends ForwardingQueue<Activity> implements Comparable<Event> {

	protected final TimeStamp eventTime;
	// Is a LiFo queue for infinite time activities, and a random queue otherwise
	protected final Queue<Activity> delegate;

	public Event(TimeStamp time) {
		this(time, new Random());
	}

	public Event(TimeStamp time, Random seed) {
		eventTime = checkNotNull(time, "Time");
		if (time.equals(TimeStamp.IMMEDIATE))
			delegate = Collections.asLifoQueue(Queues.<Activity> newArrayDeque());
		else
			delegate = RandomQueue.create(seed);
	}

	public Event(TimeStamp time, Iterable<? extends Activity> acts) {
		this(time, acts, new Random());
	}

	public Event(TimeStamp time, Iterable<? extends Activity> acts,
			Random seed) {
		eventTime = checkNotNull(time, "Time");
		if (time.equals(TimeStamp.IMMEDIATE))
			delegate = Collections.asLifoQueue(Queues.<Activity> newArrayDeque(acts));
		else
			delegate = RandomQueue.create(acts, seed);
	}

	public TimeStamp getTime() {
		return eventTime;
	}

	@Override
	public boolean add(Activity act) {
		checkArgument(eventTime.equals(act.getTime()),
				"Can't add an activity that doesn't share the time of the event");
		return super.add(act);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj.getClass().equals(getClass()))) return false;
		Event that = (Event) obj;
		return super.equals(that) && eventTime.equals(that.eventTime);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), eventTime);
	}

	@Override
	public String toString() {
		return eventTime + " | " + super.toString();
	}

	@Override
	public int compareTo(Event e) {
		checkNotNull(e, "Event");
		return getTime().compareTo(e.getTime());
	}

	@Override
	protected Queue<Activity> delegate() {
		return delegate;
	}

}
