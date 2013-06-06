package event;

import java.util.Collection;
import java.util.Random;

import utils.RandomQueue;
import activity.Activity;

/**
 * Class for an Event object. Each Event has a TimeStamp indicating when it is
 * to occur.
 * 
 * This class serves as the invoker in the Command pattern, so Event objects are
 * responsible for proving the information to call the Activity methods at a
 * later time.
 * 
 * @author ewah
 */
public class Event extends RandomQueue<Activity> implements Comparable<Event> {

	private final TimeStamp eventTime; // time in microseconds (only positive)

	/**
	 * Constructor that creates empty event for a given time.
	 * 
	 * @param time
	 * @param acts
	 */
	public Event(TimeStamp time) {
		super();
		eventTime = time;
	}
	
	public Event(TimeStamp time, Random seed) {
		super(seed);
		eventTime = time;
	}

	/**
	 * Constructor for an Event given TimeStamp time of occurrence
	 * 
	 * @param time
	 * @param acts
	 *            PriorityActivityList
	 */
	public Event(TimeStamp time, Collection<? extends Activity> acts) {
		super(acts);
		eventTime = time;
	}
	
	public Event(TimeStamp time, Collection<? extends Activity> acts, Random seed) {
		super(acts, seed);
		eventTime = time;
	}

//	/**
//	 * Copy constructor
//	 * 
//	 * @param e
//	 */
//	public Event(Event e) {
//		this(e.eventTime, e.elements);
//		// FIXME This will re-randomize the activities. Maybe not a good thing?
//	}

	/**
	 * @return TimeStamp of the event's time.
	 */
	public TimeStamp getTime() {
		return eventTime;
	}

	@Override
	public boolean add(Activity act) {
		if (act == null)
			return false; // FIXME Handle Appropriately
		else if (!eventTime.equals(act.getTime()))
			return false; // FIXME Handle Appropriately
		else
			return super.add(act);
	}

	/**
	 * @param acts
	 *            List of Activities to be added.
	 */
	public boolean addAll(Collection<? extends Activity> acts) {
		if (acts == null)
			return false; // FIXME Handle Appropriately
		boolean modified = false;
		for (Activity act : acts)
			modified |= add(act);
		return modified;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(eventTime.toString()).append(" | ");
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
