package systemmanager;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Logger.Level.DEBUG;
import static logger.Logger.Level.ERROR;
import static logger.Logger.logger;
import static logger.Logger.Level.*;

import java.util.Random;

import activity.Activity;

import com.google.common.collect.Ordering;

import event.EventQueue;
import event.TimeStamp;
import event.TimedActivity;

/**
 * EVENTMANAGER
 * 
 * This class is responsible for updating and managing the Event priority queue.
 * This involves inserting new events as well as removing them when execution is
 * complete.
 * 
 * @author ewah
 */
public class Scheduler {
	
	protected static final Ordering<TimeStamp> ord = Ordering.natural();

	protected EventQueue eventQueue;
	protected TimeStamp currentTime;

	public Scheduler(Random rand) {
		eventQueue = new EventQueue(rand);
		currentTime = TimeStamp.ZERO;
	}

	protected boolean isEmpty() {
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
	protected void executeUntil(TimeStamp time) {
		while (!isEmpty() && eventQueue.peek().getTime().before(time))
			executeNext();
	}
	
	protected void executeImmediate() {
		executeUntil(TimeStamp.ZERO);
	}

	/**
	 * Executes next activity at head of Q. Removes from Q only when execution
	 * is complete. Adds new events to EventQ after event completion.
	 */
	protected void executeNext() {
		try {
			TimedActivity act = eventQueue.remove();
			currentTime = ord.max(currentTime, act.getTime());
			logger.log(DEBUG, "%s then %s", act, eventQueue);
			act.getActivity().execute(currentTime);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void scheduleActivity(TimeStamp scheduledTime, Activity act) {
		checkArgument(!scheduledTime.isImmediate());
		eventQueue.add(scheduledTime, act);
	}
	
	public void scheduleActivities(TimeStamp scheduledTime, Activity... acts) {
		checkArgument(!scheduledTime.isImmediate());
		eventQueue.addAllOrdered(scheduledTime, acts);
	}
	
	public void executeActivity(Activity act) {
		// XXX The commendted out way is more "proper" but this way is likely (untested) more efficient
		act.execute(currentTime);
//		eventQueue.add(TimeStamp.IMMEDIATE, act);
//		executeImmediate(); // Execute all Immediate activities
	}

}
