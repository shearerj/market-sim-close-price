package event;

import java.util.HashSet;
import java.util.Set;

import systemmanager.Consts;
import systemmanager.Log;
import activity.*;

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
	protected TimeStamp simulationLength;
	
	protected Set<Activity> fastActivitySet;	// infinitely fast activities
	protected boolean alreadyExecutedFast;	// true if fast activities already executed

	/**
	 * Constructor
	 */
	public EventManager(TimeStamp ts, Log l) {
		eventQueue = new EventQueue();
		log = l;
		currentTime = new TimeStamp(0);
		simulationLength = ts;
		fastActivitySet = new HashSet<Activity>();
		alreadyExecutedFast = false;
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
	 * 
	 * Infinitely fast activities (time -1) execute & re-generate an identical
	 * activity, which will be inserted at the next event time in the Q.
	 * Currently, only infinitely fast AgentStrategy calls are supported.
	 */
	public void executeNext() {

		// FIXME This toString is slow, and probably shouldn't be called if the
		// logs aren't being used at debug level
		log.log(Log.DEBUG, this.getClass().getSimpleName() + "::executeNext: " + eventQueue);

		try {
			// Add to set of infinitely fast activities. Only execute
			// the activity if it's not infinitely fast. Execute all the
			// infinitely fast activities after executing a "slow" activity.
			Activity act = eventQueue.remove();
			if (act.getTime().after(currentTime)) {
				currentTime = act.getTime();
			}
			if (act.getTime().equals(Consts.INF_TIME)) {
				fastActivitySet.add(act); // don't execute it
			} else if (currentTime.compareTo(simulationLength) <= 0) {
				eventQueue.addAll(act.execute(currentTime));
				alreadyExecutedFast = false;
			}
			
			// Execute all the infinitely fast activities if haven't done so yet
			if (!alreadyExecutedFast) {
				log.log(Log.DEBUG, this.getClass().getSimpleName() + 
						"::executeNext: INFINITELY FAST: " + fastActivitySet);
				for (Activity a : fastActivitySet) {
					eventQueue.addAll(a.execute(currentTime));
				}
				alreadyExecutedFast = true;
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
