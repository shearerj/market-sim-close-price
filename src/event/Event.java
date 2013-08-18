package event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Random;

import com.google.common.base.Objects;

import utils.RandomQueue;
import activity.Activity;

/**
 * Class representing an event in time. Each Event is a randomly ordered queue
 * of activities and a TimeStamp indicating when it is to occur.
 * 
 * Null activities may not be added to an Event
 * 
 * @author ebrink
 */
public class Event extends RandomQueue<Activity> implements Comparable<Event> {

	private final TimeStamp eventTime; // time in microseconds (only positive)

	public Event(TimeStamp time) {
		this(time, new Random());
	}

	public Event(TimeStamp time, Random seed) {
		super(seed);
		eventTime = checkNotNull(time, "Time");
	}

	public Event(TimeStamp time, Collection<? extends Activity> acts) {
		this(time, acts, new Random());
	}

	public Event(TimeStamp time, Collection<? extends Activity> acts,
			Random seed) {
		super(acts, seed);
		eventTime = checkNotNull(time, "Time");
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

}
