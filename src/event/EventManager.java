package event;

import activity.*;
import systemmanager.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * EVENTMANAGER
 * 
 * This class is responsible for updating and managing the Event priority queue.
 * This involves inserting new events as well as removing them when execution
 * is complete.
 * 
 * The event manager can also store lists of infinitely fast activities, of which 
 * there are two types, POST and PRE. The POST list is inserted after every Event
 * on the queue, whereas the PRE list is inserted before every Event.
 * 
 * Any activity submitted with a -1 TimeStamp is a POST activity. Any activity
 * submitted with a -2 TimeStamp is a PRE activity.
 * 
 * @author ewah
 */
public class EventManager {

	private Log log;
	private EventQueue eventQueue;
	private TimeStamp duration;							// simulation length
	private PriorityActivityList fastPriorityActivityList;

	/**
	 * Constructor
	 */
	public EventManager(TimeStamp ts, Log l) {
		eventQueue = new EventQueue();
		duration = ts;
		log = l;
		fastPriorityActivityList = new PriorityActivityList();
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

		try {
			if (!fastPriorityActivityList.isEmpty()) {
				// Update ActivityList time to match current time
				TimeStamp insertTime = toExecute.getTime();
				PriorityActivityList copy = new PriorityActivityList(insertTime, fastPriorityActivityList);
				toExecute.addActivity(copy);
				fastPriorityActivityList.clear();
			}
			
			ArrayList<ActivityHashMap> acts = toExecute.executeAll();

			// Check if event has completed execution
			if (toExecute.isCompleted()) {
				removeEvent(toExecute);
			} else {
				log.log(Log.ERROR, this.getClass().getSimpleName() + 
						"::executeCurrentEvent: Event did not complete correctly.");
			}

			if (!acts.isEmpty()) {
				for (Iterator<ActivityHashMap> i = acts.listIterator(); i.hasNext();) {

					ActivityHashMap actMap = i.next();
					if (actMap != null) {
						// Iterate through the returned ActivityHashMap
						for (Map.Entry<TimeStamp, PriorityActivityList> entry : actMap.entrySet()) {
							if (entry.getKey().isInfinitelyFast()) {
								fastPriorityActivityList.add(entry.getValue());
							} else {
								createEvent(entry.getKey(), entry.getValue());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
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
	public boolean createEvent(TimeStamp ts, PriorityActivityList acts) {
		if (acts.isEmpty() || acts == null) return false;

		if (!ts.checkActivityTimeStamp(acts.getActivities())) {
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

	
//	/**
//	 * Inserts the linked list of activities into the current event.
//	 * @param t		FastActivityType
//	 * @param e		Event about to be executed
//	 * @param acts	List of activities
//	 * @return
//	 */
//	public boolean insertInfinitelyFastActivity(FastActivityType t, Event toExecute, 
//			ActivityList acts) {
//
//		TimeStamp ts = toExecute.getTime();
//		
//		if (!ts.checkActivityTimeStamp(acts)) {
//			log.log(Log.DEBUG, "ERROR: Activities do not have same timestamp");
//			return false;
//		}
//		if (t.equals(FastActivityType.PRE)) {
//			toExecute.addActivity(-1, acts); 	// add at beginning
//		} else if (t.equals(FastActivityType.POST)) {
//			toExecute.addActivity(1, acts);		// add at end
//		}
//		log.log(Log.DEBUG, "Activities " + acts.toString() + "@" + ts.toString());
//		
//		return true;
//	}
//	
//
//	/**
//	 * Checks whether or not to insert the infinitely fast activities at the time of the
//	 * just executed Event (for type POST).
//	 * 
//	 * Conditions to check for:
//	 * - happening after a certain type of other activity
//	 * - is the same as the just executed list of activities
//	 * - not happening after only a quote update
//	 * 
//	 * @param justExecuted
//	 * @return true if inserted properly
//	 */
//	public boolean toInsertInfinitelyFastPostActivity(Event justExecuted) {
//
//		if (!fastPostActivities.isEmpty()) {
//			
//			// get a copy of the activities (so won't modify them)
//			ActivityList list = justExecuted.getActivities();
//
//			// Do not insert if last event was only an NBBO update
//			if (list.size() == 1 && list.get(0) instanceof UpdateNBBO) {
//				return false;
//			}
//			// Do not insert if just executed activities are same as the fast activities
//			if (list.equals(fastPostActivities)) {
//				return false;
//			}
//			// Check if the just executed list contains activities in the fast activities list
//			boolean found = true;
//			for (Iterator<Activity> it = fastPostActivities.iterator(); it.hasNext(); ) {
//				Activity a = it.next();
//				found = found && list.contains(a);
//			}
//			if (found) return false;
//
//			for (Iterator<Activity> it = list.iterator(); it.hasNext(); ) {
//				Activity act = it.next();
//
//				// Always insert if a clear just happened
//				if (act instanceof Clear) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}

}
