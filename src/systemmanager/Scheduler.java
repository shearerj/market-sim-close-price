package systemmanager;

import static com.google.common.base.Preconditions.checkArgument;
import static logger.Log.log;
import static logger.Log.Level.*;

import java.util.Random;

import activity.Activity;

import com.google.common.collect.Ordering;

import event.EventQueue;
import event.TimeStamp;
import event.TimedActivity;

/**
 * 
 * SCHEDULER
 * 
 * This class is responsible for managing the event queue and removes them when
 * execution is complete. Entities have a reference to the scheduler to allow
 * them to schedule new activities. See the public methods in this class for how
 * entities can schedule new activities.
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
	 * 
	 * TODO Change back to including. Makes more sense with testing
	 * 
	 * @param time
	 */
	protected void executeUntil(TimeStamp time) {
		while (!isEmpty() && eventQueue.peek().getTime().before(time))
			executeNext();
	}

	/**
	 * Executes any activity listed as immediate. With the current
	 * implementation, these should never get scheduled.
	 */
	protected void executeImmediate() {
		executeUntil(TimeStamp.ZERO);
	}

	/**
	 * Executes next activity at head of Q. The activities execute activity
	 * should schedule upcoming activities.
	 */
	protected void executeNext() {
		try {
			TimedActivity act = eventQueue.remove();
			currentTime = ord.max(currentTime, act.getTime());
			log.log(DEBUG, "%s then %s", act, eventQueue);
			act.getActivity().execute(currentTime);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Schedule an activity to happen as some point in the future. Any
	 * activities that are scheduled at the same point in the future will have a
	 * non deterministic ordering. To ensure a specific ordering for activities
	 * scheduled at the same time in the future, use the method
	 * <code>scheduleActivities</code>
	 * 
	 * @param scheduledTime can't be before the current time
	 * @param act
	 */
	public void scheduleActivity(TimeStamp scheduledTime, Activity act) {
		checkArgument(!scheduledTime.before(currentTime));
		eventQueue.add(scheduledTime, act);
	}
	
	/**
	 * Schedule several activities to happen at some point in the future with a
	 * deterministic ordering. The first activitiy listed will occur first,
	 * followed by the next activity, etc. There may still be activities that
	 * happen between the first and secon scheduled activity. The only guarantee
	 * is that activities that occur in the beginning of the list will be
	 * executed before any that occur after them in the list.
	 * 
	 * @param scheduledTime can't be before the current time
	 * @param acts
	 */
	public void scheduleActivities(TimeStamp scheduledTime, Activity... acts) {
		checkArgument(!scheduledTime.isImmediate());
		eventQueue.addAllOrdered(scheduledTime, acts);
	}

	/**
	 * Executes the activity immediately. This execution happens instantaniously
	 * and takes prescedence over any other event in the event queue. This is
	 * different than scheduling an activity to happen at the current time,
	 * because an activity scheduled at the current time could still be executed
	 * after other activities that are also scheduled for the current time. This
	 * will happen immediatly.
	 * 
	 * @param act
	 */
	public void executeActivity(Activity act) {
		// XXX The commendted out way is more "proper" but this way is likely (untested) more efficient
		act.execute(currentTime);
//		eventQueue.add(TimeStamp.IMMEDIATE, act);
//		executeImmediate(); // Execute all Immediate activities
	}

}
