package systemmanager;

import static logger.Logger.log;
import static logger.Logger.Level.*;
import static utils.Compare.max;

import java.util.Random;

import event.EventQueue;
import event.TimeStamp;

import logger.Logger;
import activity.Activity;

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

	public EventManager(Random rand) {
		eventQueue = new EventQueue(rand);
		currentTime = TimeStamp.ZERO;
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
		return currentTime;
	}

	/**
	 * Executes activities until and including time
	 * @param time
	 */
	public void executeUntil(TimeStamp time) {
		while (!isEmpty() && !eventQueue.peek().getTime().after(time))
			executeNext();
	}

	/**
	 * Executes next activity at head of Q. Removes from Q only when execution
	 * is complete. Adds new events to EventQ after event completion.
	 */
	protected void executeNext() {

		if (Logger.getLevel().ordinal() >= DEBUG.ordinal())
			log(DEBUG, this.getClass().getSimpleName()
					+ "::executeNext: " + eventQueue);

		try {
			
			Activity act = eventQueue.remove();
			currentTime = max(currentTime, act.getTime());
			eventQueue.addAll(act.execute(currentTime));
			log(DEBUG, act.toString());
		} catch (Exception e) {
			log(ERROR, this.getClass().getSimpleName()
					+ "::executeNext:error executing activity.");
			e.printStackTrace();
		}
	}

	public void addActivity(Activity act) {
		eventQueue.add(act);
	}

}
