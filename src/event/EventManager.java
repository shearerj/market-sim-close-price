package event;

import java.util.Map.Entry;

import systemmanager.Log;
import activity.Activity;
import activity.ActivityHashMap;
import activity.ActivityQueue;
import activity.PriorityActivityList;

/**
 * EVENTMANAGER
 * 
 * This class is responsible for updating and managing the Event priority queue.
 * This involves inserting new events as well as removing them when execution is
 * complete.
 * 
 * The event manager can also store lists of infinitely fast activities, which
 * are stored as PriorityActivityList.
 * 
 * @author ewah
 */
public class EventManager {

	protected Log log;
	protected ActivityQueue activityQueue;
	// private TimeStamp duration; // simulation length

	/**
	 * Constructor
	 */
	public EventManager(TimeStamp ts, Log l) {
		activityQueue = new ActivityQueue();
		// duration = ts;
		log = l;
	}
	
	public boolean isEmpty() {
		return activityQueue.isEmpty();
	}

	/**
	 * Returns time (i.e. priority) of event at head of event queue
	 * 
	 * @return
	 */
	public TimeStamp getCurrentTime() {
		return activityQueue.element().getTime();
	}

	/**
	 * Executes event at head of Q. Removes from Q only when execution is
	 * complete. Adds new events to EventQ after event completion.
	 */
	public void executeCurrentEvent() {

		// FIXME This toString is slow, and probably shouldn't be called if the
		// logs aren't being used at debug level
		log.log(Log.DEBUG, "executeCurrentEvent: " + activityQueue);

		try {
			ActivityHashMap acts = activityQueue.remove().execute();
			if (acts == null)
				// FIXME This should be reported, and not happening...
				return;

			for (Entry<TimeStamp, PriorityActivityList> e : acts.entrySet())
				activityQueue.addAll(e.getValue().getActivities().getList());

		} catch (Exception e) {
			// TODO This should be logged at the least.
			e.printStackTrace();
		}
	}
	
	public void addActivity(Activity act) {
		activityQueue.add(act);
	}

}
