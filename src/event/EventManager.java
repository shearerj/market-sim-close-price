package event;

import activity.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * This class is responsible for updating and managing the Event priority queue.
 * This involves inserting new events as well as removing them when execution
 * is complete.
 * 
 * @author ewah
 */
public class EventManager {

	private EventQueue eventQueue;
	private TimeStamp duration;
	
	/**
	 * Constructor
	 */
	public EventManager(TimeStamp ts) {
		eventQueue = new EventQueue();
		duration = ts;
	}
	
	/**
	 * Returns time (i.e. priority) of event at head of event queue
	 * @return
	 */
	public TimeStamp getCurrentTime() {
		return this.eventQueue.peek().getTime();
	}
	
	/**
	 * Adds an event to the event queue.
	 * @param e		Event to be added
	 */
	public void addEvent(Event e) {
		this.eventQueue.add(e);
	}
	
	/**
	 * Removes an event from the event queue.
	 * @param e		Event to remove
	 */
	public void removeEvent(Event e) {
		this.eventQueue.remove(e);
	}
	
	/**
	 * Checks if event queue is empty
	 * @return		boolean
	 */
	public boolean isEventQueueEmpty() {
		return this.eventQueue.isEmpty();
	}
	
	/**
	 * Executes event at head of Q. Removes from Q only when execution 
	 * is complete. Adds new events to EventQ after event completion.
	 */
	public void executeCurrentEvent() {

		// For logging - TODO
//		System.out.println(this.eventQueue.toString());
		
		Event toExecute = this.eventQueue.peek();
		ArrayList<ActivityHashMap> acts = toExecute.executeAll();
		
		// Check if event has completed execution
		if (toExecute.isCompleted()) {
			removeEvent(toExecute);
		} else {
			System.out.println("Some error has occurred.");
		}
		if (!acts.isEmpty()) {
			for (Iterator<ActivityHashMap> i = acts.listIterator(); i.hasNext();) {
				ActivityHashMap act = i.next();
//				System.out.println("manage: " + act.toString());
				manageActivityMap(act);
			}
		}
//		System.out.println("After mgmt:" + this.eventQueue.toString());
	}
	
	/**
	 * Creates event at TimeStamp matching Activity parameter.
	 * @param act
	 * @return true if Event created successfully
	 */
	public boolean createEvent(Activity act) {
		if (act == null) return false;
		
		Event e = this.eventQueue.contains(act.getTime());
		if (e != null) { 	// append at end of existing event
			e.addActivity(act);
			// TODO - logging
		} else {	// create new Event
			e = new Event(act.getTime());
			e.addActivity(act);
			// only add to queue if will occur within the duration of the simulation
			if (act.getTime().before(duration)) addEvent(e);
			// TODO - add logging!!!
		}
		return true;
	}
	
	/**
	 * Create event with specified TimeStamp and activity list, and inserts
	 * into the EventQueue.
	 * @param t
	 * @param acts
	 * @return true if Event created successfully
	 */
	public boolean createEvent(TimeStamp ts, LinkedList<Activity> acts) {
		if (acts.isEmpty() || acts == null) return false;
		
		if (!ts.checkActivityTimeStamp(acts)) {
			System.out.println("Activities do not have same timestamp");
			return false;
			// TODO - note that the activities in the list do not all have the same timestamp
		}
//		System.out.println("current EQ before event creation: " + this.eventQueue.toString());
		Event e = this.eventQueue.contains(ts);
		if (e != null) {
			e.addActivity(acts);
		} else {
			// only add to queue if will occur within the duration of the simulation
			if (ts.before(duration)) this.eventQueue.add(new Event(ts, acts));
		}
		return true;
	}

	/**
	 * Takes the activities in an ActivityHashMap and inserts them into the
	 * EventQueue as events.
	 * @param actMap
	 */
	public void manageActivityMap(ActivityHashMap actMap) {
		if (actMap == null) return;
		if (!actMap.isEmpty()) {
			for (Map.Entry<TimeStamp,LinkedList<Activity>> entry : actMap.entrySet()) {
				createEvent(entry.getKey(), entry.getValue());
			}
		}
	}
}
