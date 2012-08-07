package event;

import activity.*;
import systemmanager.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * This class is responsible for updating and managing the Event priority queue.
 * This involves inserting new events as well as removing them when execution
 * is complete.
 * 
 * The event manager can also store a list of infinitely fast activities, which it
 * will then insert & execute after every Event.
 * 
 * @author ewah
 */
public class EventManager {

	private EventQueue eventQueue;
	private TimeStamp duration;
	private LinkedList<Activity> fastActs;	// list of infinitely fast activities
	private Log log;
	
	/**
	 * Constructor
	 */
	public EventManager(TimeStamp ts, Log l) {
		eventQueue = new EventQueue();
		duration = ts;
		log = l;
	}
	
	/**
	 * Sets the list of infinitely fast activities.
	 * @param acts
	 */
	public void setInfiniteFastActs(LinkedList<Activity> acts) {
		if (!acts.isEmpty()) {
			fastActs = acts;
		} else {
			fastActs = null;
		}
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

		log.log(Log.DEBUG, "executeCurrentEvent: " + this.eventQueue.toString());
		
		Event toExecute = this.eventQueue.peek();
		
		// Update time of infinitely fast activities to current time
		for (Iterator<Activity> it = fastActs.iterator(); it.hasNext(); ) {
			it.next().changeTime(toExecute.getTime());
		}
		toExecute.addActivity(fastActs);
		ArrayList<ActivityHashMap> acts = toExecute.executeAll();
		
		// Check if event has completed execution
		if (toExecute.isCompleted()) {
			removeEvent(toExecute);
		} else {
			log.log(Log.DEBUG, "ERROR: Event did not complete correctly.");
		}
		if (!acts.isEmpty()) {
			for (Iterator<ActivityHashMap> i = acts.listIterator(); i.hasNext();) {
				ActivityHashMap act = i.next();
				manageActivityMap(act);
			}
		}
	}
	
	/**
	 * Creates event at TimeStamp matching Activity parameter.
	 * @param act
	 * @return true if Event created successfully
	 */
	public boolean createEvent(Activity act) {
		if (act == null) return false;
		
		Event e = this.eventQueue.contains(act.getTime());
		if (e != null) { 	
			// append at end of existing event
			e.addActivity(act);
			log.log(Log.DEBUG, "Activity " + act.toString() + "@" + act.getTime().toString());
		} else {	
			// create new Event
			e = new Event(act.getTime());
			e.addActivity(act);
			
			// only add to queue if will occur within the duration of the simulation
			if (act.getTime().before(duration)) { 
				addEvent(e);
				log.log(Log.DEBUG, "Event " + e.toString() + "@" + act.getTime().toString());
			}
		}
		return true;
	}
	
	/**
	 * Create event with specified TimeStamp and activity list, and inserts
	 * into the EventQueue.
	 * 
	 * @param t
	 * @param acts
	 * @return true if Event created successfully
	 */
	public boolean createEvent(TimeStamp ts, LinkedList<Activity> acts) {
		if (acts.isEmpty() || acts == null) return false;
		
		if (!ts.checkActivityTimeStamp(acts)) {
			log.log(Log.DEBUG, "ERROR: Activities do not have same timestamp");
			return false;
		}
		Event e = this.eventQueue.contains(ts);
		if (e != null) {
			e.addActivity(acts);
			log.log(Log.DEBUG, "Activities " + acts.toString() + "@" + ts.toString());
		} else {
			// only add to queue if will occur within the duration of the simulation
			if (ts.before(duration)) {
				e = new Event(ts, acts);
				this.eventQueue.add(e);
				log.log(Log.DEBUG, "Event " + e.toString() + "@" + ts.toString());
			}
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
