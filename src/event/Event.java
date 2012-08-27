package event;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;

import activity.*;

/**
 * Class for an Event object. Each Event has a TimeStamp indicating when it
 * is to occur.
 * 
 * This class serves as the invoker in the Command pattern, so Event objects
 * are responsible for proving the information to call the Activity methods 
 * at a later time.
 * 
 * @author ewah
 */
public class Event {

	private TimeStamp eventTime; 						// time in microseconds (only positive)
	private boolean eventCompletion = false;			// flag indicating whether or not the event has been executed
	private LinkedList<Activity> activities;			// activities in the event
	
	/**
	 * Constructor for Event object.
	 */
	public Event() {
		eventTime = new TimeStamp(-1);
		activities = new LinkedList<Activity>();
	}
	
	/**
	 * Constructor that creates empty event for a given time.
	 * @param time
	 */
	public Event(TimeStamp time) {
		eventTime = time;
		activities = new LinkedList<Activity>();
	}
	
	/**
	 * Constructor for an Event given TimeStamp time of occurrence
	 * @param time
	 */
	public Event(TimeStamp time, LinkedList<Activity> acts) {
		eventTime = time;
		activities = acts;
	}
	
	/**
	 * @return	TimeStamp of the event's time.
	 */
	public TimeStamp getTime() {
		return this.eventTime;
	}

	
	/**
	 * @return linked list of activities.
	 */
	public LinkedList<Activity> getActivities() {
		return this.activities;
	}
	
	/**
	 * @return	boolean of event completion status.
	 */
	public boolean isCompleted() {
		return this.eventCompletion;
	}
	
	/**
	 * @param act	Activity to be added to LinkedList for Event.
	 */
	public void addActivity(Activity act) {
		if (act != null) {
			if (eventTime.checkActivityTimeStamp(act)) {
				this.activities.add(act);
			} else {
				System.out.println("Event::addActivity::ERROR: activity does not match Event time.");
			}
		}
			
	}

	/**
	 * @param acts	LinkedList of Activities to be added
	 */
	public void addActivity(LinkedList<Activity> acts) {
		if (acts != null) {
			if (eventTime.checkActivityTimeStamp(acts)) {
				this.activities.addAll(acts);
			} else {
				System.out.println("Event::addActivity::ERROR: activity list does not match Event time.");
			}
		}
	}
	
	
	/**
	 * Executes all activities stored in an event. Note that newly
	 * generated activities cannot be added to the current event, but may
	 * have the same TimeStamp as the current event.
	 * 
	 * @return 		List of ActivityHashMaps to parse and add to Event Queue
	 */
	public ArrayList<ActivityHashMap> executeAll() {
//		System.out.print("" + this.eventTime.toString() + " |  ");
//		System.out.println("" + this.eventTime.toString() + " |  " + this.toString());
		
		ArrayList<ActivityHashMap> actList = new ArrayList<ActivityHashMap>();
		Iterator<Activity> itr = activities.iterator();
		while (itr.hasNext()) {
			ActivityHashMap ahm = itr.next().execute();
			if (ahm != null) {
				actList.add(ahm);
			}
		}
		this.eventCompletion = true;
		return actList;
	}
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String s = "" + eventTime.toString() + " | ";
		for (Iterator<Activity> it = activities.iterator(); it.hasNext(); ) {
//			s += it.next().getClass().getName() + "->";
			s += it.next().toString() + " -> ";
		}
		return s;
	}
}
