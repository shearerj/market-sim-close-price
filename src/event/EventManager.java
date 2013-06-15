package event;


import systemmanager.Log;
import activity.Activity;


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
	protected EventQueue eventQueue;
	protected TimeStamp currentTime;

	private TimeStamp simulationLength; // simulation length

	/**
	 * Constructor
	 */
	public EventManager(TimeStamp ts, Log l) {
		eventQueue = new EventQueue();
		log = l;
		currentTime = new TimeStamp(0);
		simulationLength = ts;
	}

	public boolean isEmpty() {
		return eventQueue.isEmpty();
	}

	/**
	 * Returns time (i.e. priority) of event at head of event queue
	 * 
	 * @return
	 */
	public TimeStamp getCurrentTime() {
		return eventQueue.element().getTime();
	}

	/**
	 * Executes next activity at head of Q. Removes from Q only when execution is
	 * complete. Adds new events to EventQ after event completion.
	 */
	public void executeNext() {

		// FIXME This toString is slow, and probably shouldn't be called if the
		// logs aren't being used at debug level
		log.log(Log.DEBUG, "executeNext: " + eventQueue);

		try {
			// This way infinitely fast activities can still schedule events x
			// time in the future.
			Activity act = eventQueue.remove();
			if (act.getTime().after(currentTime)) {
				currentTime = act.getTime();
			}
			if (currentTime.compareTo(simulationLength) <= 0) {
				// only execute if current time is within simulation length
				eventQueue.addAll(act.execute(currentTime));
			}
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName() + "::executeNext:"
					+ "error executing activity.");
			e.printStackTrace();
		}
	}

	public void addActivity(Activity act) {
		eventQueue.add(act);
	}

}
