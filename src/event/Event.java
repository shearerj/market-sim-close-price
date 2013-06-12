package event;

import java.util.ArrayList;
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
	private PriorityActivityList activities;
	
//	/**
//	 * Constructor for Event object.
//	 */
//	public Event() {
//		eventTime = new TimeStamp(-1);
//		activities = new PriorityActivityList();
//	}
	
	/**
	 * Constructor that creates empty event for a given time.
	 * @param time
	 * @param acts
	 */
	public Event(TimeStamp time) {
		eventTime = time;
		activities = new PriorityActivityList();
	}
	
	/**
	 * Constructor for an Event given TimeStamp time of occurrence
	 * @param time
	 * @param acts PriorityActivityList
	 */
	public Event(TimeStamp time, PriorityActivityList acts) {
		eventTime = time;
		activities = acts;
	}
	
	/**
	 * Constructor for an Event given ActivityList.
	 * @param time
	 * @param acts ActivityList
	 */
	public Event(TimeStamp time, ActivityList acts) {
		eventTime = time;
		activities = new PriorityActivityList();
		activities.add(acts);
	}
	
	/**
	 * Copy constructor
	 * @param e
	 */
	public Event(Event e) {
		eventTime = new TimeStamp(e.eventTime);
		activities = new PriorityActivityList(e.activities);
	}
	
	/**
	 * @return	TimeStamp of the event's time.
	 */
	public TimeStamp getTime() {
		return this.eventTime;
	}
	
	/**
	 * @return copy of the list of activities.
	 */
	public PriorityActivityList getCopyOfActivities() {
		return new PriorityActivityList(this.activities);
	}
	
	/**
	 * @return ActivityList
	 */
	public ActivityList getActivities() {
		return this.activities.getActivities();
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
				System.err.println("Event::addActivity::ERROR: activity does not match Event time.");
			}
		}	
	}
	
	/**
	 * @param priority 	Priority of Activity to insert.
	 * @param act		Activity to be added to LinkedList for Event.
	 */
	public void addActivity(int priority, Activity act) {
		if (act != null) {
			if (eventTime.checkActivityTimeStamp(act)) {
				this.activities.add(priority, act);
			} else {
				System.err.println("Event::addActivity::ERROR: activity does not match Event time.");
			}
		}	
	}
	

	/**
	 * @param acts	List of Activities to be added.
	 */
	public void addActivity(ActivityList acts) {
		if (acts != null) {
			if (eventTime.checkActivityTimeStamp(acts)) {
				this.activities.add(acts);
			} else {
				System.err.println("Event::addActivity::ERROR: activity list does not match Event time.");
			}
		}
	}
	
	/**
	 * Will convert the priority of the ActivityList to the given one.
	 * @param priority 	Priority of Activity to insert.
	 * @param acts		List of Activities to be added.
	 */
	public void addActivity(int priority, ActivityList acts) {
		if (acts != null) {
			if (eventTime.checkActivityTimeStamp(acts)) {
				ActivityList tmp = new ActivityList(priority, acts);
				this.activities.add(tmp);
			} else {
				System.err.println("Event::addActivity::" + 
						"ERROR: activity list does not match Event time.");
			}
		}
	}

	
	/**
	 * @param pal	PriorityActivityList to be added.
	 */
	public void addActivity(PriorityActivityList pal) {
		if (pal != null) {
			this.activities.add(pal);
		}
	}
	
//	/**
//	 * Executes all activities stored in an event. Note that newly
//	 * generated activities cannot be added to the current event, but may
//	 * have the same TimeStamp as the current event.
//	 * 
//	 * @return 		List of ActivityHashMaps to parse and add to Event Queue
//	 */
//	public ArrayList<ActivityHashMap> executeAll() {
//		
//		ArrayList<ActivityHashMap> actList = new ArrayList<ActivityHashMap>();
//		Iterator<Activity> itr = activities.iterator();
//		while (itr.hasNext()) {
//			ActivityHashMap ahm = itr.next().execute();
//			if (ahm != null) {
//				actList.add(ahm);
//			}
//		}
//		this.eventCompletion = true;
//		return actList;
//	}
	
	/**
	 * Executes all activities stored in an event. Newly generated 
	 * activities with the same TimeStamp are inserted into the current event.
	 * Any activities with different TimeStamps are returned in the
	 * ActivityHashMap.
	 * 
	 * @return 		List of ActivityHashMaps to parse and add to Event Queue
	 */
	public ArrayList<ActivityHashMap> executeOneByOne() {
//		System.out.println(this);
		
		ArrayList<ActivityHashMap> actList = new ArrayList<ActivityHashMap>();
		
		while (!activities.isEmpty()) {
			// execute one activity, then find elts in returned AHM w/ same TimeStamp
			ArrayList<ActivityHashMap> acts = this.executeHighPriority();
			for (Iterator<ActivityHashMap> it = acts.iterator(); it.hasNext();) {
				ActivityHashMap tmp = it.next();
				if (tmp.containsKey(eventTime)) {
					// remove the associated PAL & insert into this event's activities
					activities.add(tmp.remove(eventTime));
				}
				actList.add(tmp);
			}
		}
		this.eventCompletion = true;
		return actList;
	}
	
	/**
	 * Executes activities with the highest priority in the event.
	 * 
	 * @return
	 */
	private ArrayList<ActivityHashMap> executeHighPriority() {
		ArrayList<ActivityHashMap> actList = new ArrayList<ActivityHashMap>();
		
		int priority = activities.getFirstPriority();
		PriorityActivityList pal = activities.getActsPriorityLessThan(priority);
//		System.out.println("" + this.eventTime.toString() + " |  " + pal.toString());
		
		Iterator<Activity> it = pal.iterator();
		while (it.hasNext()) {
			ActivityHashMap ahm = it.next().execute();
			if (ahm != null) {
				actList.add(ahm);
			}
		}
		return actList;
	}
	
	
	/**
	 * @return priority of last Activity in this Event.
	 */
	public int getLastActivityPriority() {
		return activities.getLastPriority();
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String s = "" + eventTime.toString() + " | ";
		for (Iterator<Activity> it = activities.iterator(); it.hasNext(); ) {
			s += it.next().toString() + " -> ";
		}
		return s;
	}
}
