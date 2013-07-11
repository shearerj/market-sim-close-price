package event;

import static logger.Logger.log;
import static logger.Logger.Level.*;
import static systemmanager.Consts.START_TIME;
import static systemmanager.Consts.INF_TIME;

import java.util.Random;

import logger.Logger;
import activity.Activity;
import activity.AgentStrategy;
import activity.Clear;

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

	protected EventQueue eventQueue;
	protected TimeStamp currentTime;
	protected TimeStamp simulationLength;

	protected Activity fastActivity; // infinitely fast activity

	public EventManager(TimeStamp simulationLength, Random rand) {
		eventQueue = new EventQueue(rand);
		currentTime = START_TIME;
		this.simulationLength = simulationLength;
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

	public void execute() {
		while (!isEmpty() && currentTime.before(simulationLength)) {
			executeNext();
		}
	}

	/**
	 * Executes next activity at head of Q. Removes from Q only when execution
	 * is complete. Adds new events to EventQ after event completion.
	 * 
	 * Infinitely fast activities (time -1) execute & re-generate an identical
	 * activity, which will be inserted at the next event time in the Q.
	 * Currently, only infinitely fast AgentStrategy calls are supported.
	 */
	protected void executeNext() {

		if (Logger.getLevel().ordinal() >= DEBUG.ordinal())
			log(DEBUG, this.getClass().getSimpleName()
					+ "::executeNext: " + eventQueue);

		try {
			Activity act = eventQueue.remove();
			if (act.getTime().after(currentTime)) {
				currentTime = act.getTime();
			}
			// TODO remove later - this stores infinitely fast AgentStrategy
			// activity for execution after any market Clear activity. This
			// activity is only added onto the Q during the MMAgent's arrival
			// method (if it's infinitely fast, the LAAgent does not add a new
			// AgentStrategy method through chaining).
			//
			// FIXME I'm not certain this will work as intended
			if (act.getTime().equals(INF_TIME)
					&& act instanceof AgentStrategy) {
				fastActivity = act;
			}
			if (currentTime.after(simulationLength)) return;

			// (temporary) insert infinitely fast
			// LA agent strategy after any market clear event
			if (act instanceof Clear && fastActivity != null) {
				eventQueue.add(fastActivity);
			}
			eventQueue.addAll(act.execute(currentTime));
				
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName()
					+ "::executeNext:" + "error executing activity.");
			e.printStackTrace();
		}
	}

	public void addActivity(Activity act) {
		eventQueue.add(act);
	}

}
