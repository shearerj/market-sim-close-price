package systemmanager;

import static logger.Logger.log;
import static logger.Logger.Level.*;

import java.util.Random;

import com.google.common.collect.Ordering;

import event.EventQueue;
import event.TimeStamp;

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
	
	protected static final Ordering<TimeStamp> ord = Ordering.natural();

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
	 * Executes activities until (not including) time
	 * @param time
	 */
	public void executeUntil(TimeStamp time) {
		while (!isEmpty() && eventQueue.peek().getTime().before(time))
			executeNext();
	}
	
	/**
	 * Executes next immediate activities
	 */
	public void executeImmediate() {
		executeUntil(TimeStamp.ZERO);
	}

	/**
	 * Executes next activity at head of Q. Removes from Q only when execution
	 * is complete. Adds new events to EventQ after event completion.
	 */
	protected void executeNext() {
		try {
			Activity act = eventQueue.remove();
			currentTime = ord.max(currentTime, act.getTime());
			log(DEBUG, act + " then " + eventQueue);
			eventQueue.addAll(act.execute(currentTime));
			
		} catch (Exception e) {
			log(ERROR, "Error executing activity");
			e.printStackTrace();
		}
	}

	public void addActivity(Activity act) {
		eventQueue.add(act);
	}

}
