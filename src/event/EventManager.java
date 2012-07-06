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
	
	/**
	 * Constructor
	 */
	public EventManager() {
		eventQueue = new EventQueue();
	}
	
	/**
	 * Returns time (i.e. priority) of event at head of event queue
	 * @return
	 */
	public TimeStamp getCurrentTime() {
		return this.eventQueue.peek().getTime();
	}
	
	/**
	 * Adds an event to the event queue
	 * @param e		Event to be added
	 */
	public void addEvent(Event e) {
		this.eventQueue.add(e);
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

		Event toExecute = this.eventQueue.peek();
		ArrayList<ActivityHashMap> acts = toExecute.executeAll();
		
		// Check if event has completed execution
		if (toExecute.isCompleted()) {
			this.eventQueue.remove(toExecute);
			System.out.println("Event at time " + toExecute.getTime().toString() + " has been executed");
		} else {
			// ERROR - TODO
			System.out.println("Some error has occurred.");
		}
		if (!acts.isEmpty()) {
			for (Iterator<ActivityHashMap> i = acts.listIterator(); i.hasNext();) {
				manageActivityMap(i.next());
			}
		}
	}

	/**
	 * Create event with specified TimeStamp and single activity, and inserts
	 * into the EventQueue.
	 * @param t
	 * @param act
	 * @return true if Event created successfully
	 */
//	public boolean createEvent(TimeStamp t, Activity act) {
//		LinkedList<Activity> activities = new LinkedList<Activity>();
//		Event e = new Event(t, activities);
//		e.addActivity(act);
//		this.eventQueue.add(e);
//		return true;
//	}
	
	/**
	 * Creates event at TimeStamp matching Activity parameter.
	 * @param act
	 * @return true if Event created successfully
	 */
	public boolean createEvent(Activity act) {
		LinkedList<Activity> activities = new LinkedList<Activity>();
		Event e = new Event(act.getTime(), activities);
		e.addActivity(act);
		this.eventQueue.add(e);
		return true;
	}	
	
	/**
	 * Create event with specified TimeStamp and activity list, and inserts
	 * into the EventQueue.
	 * @param t
	 * @param acts
	 * @return
	 */
	public boolean createEvent(TimeStamp t, LinkedList<Activity> acts) {
		if (acts.isEmpty()) return false;

		this.eventQueue.add(new Event(t, acts));
		return true;
	}

	
	/**
	 * Takes the activities in an ActivityHashMap and inserts them into the
	 * EventQueue as events.
	 * @param actMap
	 */
	public void manageActivityMap(ActivityHashMap actMap) {
		
		if (!actMap.isEmpty()) {
			for (Map.Entry<TimeStamp,LinkedList<Activity>> entry : actMap.entrySet()) {
				createEvent(entry.getKey(), entry.getValue());
				// TODO - do not append vectors, insert as new (separate) Event
			}
		}
		
	}
}
