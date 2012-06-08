package event;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

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
	private boolean eventCompletion = false;			// flag indicating whether or not the event has been executed?
	
	private LinkedList<Activity> activities;			// activities in the event
	
	
	/**
	 * Empty constructor for Event object.
	 */
	public Event() {
		eventTime = new TimeStamp(-1);
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
	 * @return	boolean of event completion status.
	 */
	public boolean isCompleted() {
		return this.eventCompletion;
	}
	
	/**
	 * @param cmd	Activity to be stored in LinkedList for Event.
	 */
	public void storeActivity(Activity cmd) {
		//System.out.println("Storing an Activity in this Event");
		this.activities.add(cmd);
	}
	

	/**
	 * Executes all activities stored in an event.
	 * @return 		List of Events to be added to the EventQueue.
	 */
	public List<Event> executeAll() {
		List<Event> eventsGenerated = new ArrayList<Event>();
		
		System.out.println("Executing event of time " + this.eventTime.toString() + "!: ");
		
		ListIterator<Activity> itr = activities.listIterator();
		while (itr.hasNext()) {
			Activity cmd = itr.next();
			Event e = cmd.execute();
			if (e!=null) { 	// only add non-null Events
				eventsGenerated.add(e);
			}
		}
		this.eventCompletion = true;
		
		return eventsGenerated;
	}
	
}
