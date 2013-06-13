package event;

import java.util.Collection;
import java.util.Random;

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
		super();
		eventTime = time;
	}

	public Event(TimeStamp time, Random seed) {
		super(seed);
		eventTime = time;
	}

	public Event(TimeStamp time, Collection<? extends Activity> acts) {
		super(acts);
		eventTime = time;
	}

	public Event(TimeStamp time, Collection<? extends Activity> acts,
			Random seed) {
		super(acts, seed);
		eventTime = time;
	}

	public TimeStamp getTime() {
		return eventTime;
	}

	@Override
	public boolean add(Activity act) {
		if (act == null) {
			System.err.println("Tried to add null Activity to Event");
			return false; // FIXME Handle Appropriately
		} else if (!eventTime.equals(act.getTime())) {
			throw new IllegalArgumentException("Can't add an activity that doesn't share the time of the event");
		} else {
			return super.add(act);
		}
	}


	@Override
	public boolean addAll(Collection<? extends Activity> acts) {
		if (acts == null) {
			System.err.println("Tried to add null Activity to Event");
			return false; // FIXME Handle Appropriately
		}
		boolean modified = false;
		for (Activity act : acts)
			modified |= add(act);
		return modified;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(eventTime.toString())
				.append(" | ");
		for (Activity act : elements)
			sb.append(act).append(" -> ");
		return sb.substring(0, sb.length() - 4);
	}

	@Override
	public int compareTo(Event e) {
		if (e == null)
			throw new NullPointerException("Can't compare to a null event");
		return getTime().compareTo(e.getTime());
	}

	@Override
	public boolean offer(Activity e) {
		return add(e);
	}

}
