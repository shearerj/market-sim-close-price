package event;

import systemmanager.*;
import activity.*;

/**
 * EVENTMANAGER
 * 
 * This class is responsible for updating and managing the Event priority queue.
 * This involves inserting new events as well as removing them when execution is
 * complete.
 * 
 * @author ewah
 */
public class EventManager {

	protected Log log;
	protected EventQueue eventQueue;
	protected TimeStamp currentTime;
	protected TimeStamp simulationLength;
	
	protected Activity fastActivity;	// infinitely fast activity

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
	 * 
	 * Infinitely fast activities (time -1) execute & re-generate an identical
	 * activity, which will be inserted at the next event time in the Q.
	 * Currently, only infinitely fast AgentStrategy calls are supported.
	 */
	public void executeNext() {

		if (log.getLevel() == Log.DEBUG) {
			log.log(Log.DEBUG, this.getClass().getSimpleName() + "::executeNext: " + 
					eventQueue);
		}

		try {
			Activity act = eventQueue.remove();
			if (act.getTime().after(currentTime)) {
				currentTime = act.getTime();
			}
			// TODO remove later - this stores infinitely fast AgentStrategy activity
			// for execution after any market Clear activity. This activity is only
			// added onto the Q during the MMAgent's arrival method (if it's infinitely
			// fast, the LAAgent does not add a new AgentStrategy method through chaining).
			if (act.getTime().equals(Consts.INF_TIME) && act instanceof AgentStrategy) {
				fastActivity = act;
			}
			if (currentTime.compareTo(simulationLength) <= 0) {
				// (temporary) insert infinitely fast 
				// LA agent strategy after any market clear event
				if (act instanceof Clear && fastActivity != null) {
					eventQueue.add(fastActivity);
				}
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
