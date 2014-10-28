package event;

import static logger.Log.Level.DEBUG;

import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;

import logger.Log.LogClock;
import systemmanager.Simulation;
import utils.Collections3;
import utils.RandomKeyedQueue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

public class EventQueue implements LogClock{
	
	private final Simulation sim;
	private TimeStamp currentTime;
	
	private final RandomKeyedQueue<TimeStamp, Activity> scheduledActivities;
	private final Queue<Activity> immediateActivities;
	
	private final ListMultimap<TimeStamp, Activity> pendingScheduledActivities; 
	private final List<Activity> pendingImmediateActivities;
	
	protected EventQueue(Simulation sim, Random rand) {
		this.sim = sim;
		this.scheduledActivities = RandomKeyedQueue.create(rand);
		this.immediateActivities = Collections3.asLifoQueue(Lists.<Activity> newArrayList());
		this.pendingScheduledActivities = ArrayListMultimap.create();
		this.pendingImmediateActivities = Lists.newArrayList();
		this.currentTime = TimeStamp.ZERO;
	}
	
	public static EventQueue create(Simulation sim, Random rand) {
		return new EventQueue(sim, rand);
	}
	
	private boolean moreImmediateActivities() {
		return !immediateActivities.isEmpty() || !pendingImmediateActivities.isEmpty();
	}
	
	// TODO Make this more efficient by using a sorted Multimap
	private boolean moreScheduledActivities(TimeStamp time) {
		if (!scheduledActivities.isEmpty() && !scheduledActivities.peek().getKey().after(time))
			return true;
		for (TimeStamp scheduledTime : pendingScheduledActivities.keySet())
			if (!scheduledTime.after(time))
				return true;
		return false;
	}
	
	public void executeUntil(TimeStamp time) {
		while (moreImmediateActivities() || moreScheduledActivities(time))
			executeNext();
		if (time.after(currentTime))
			currentTime = time;
	}
	
	private void executeNext() {
		Activity act = pop();
		sim.log(DEBUG, "Executing {%s} the immediately {%s} then {%s}", act, immediateActivities, scheduledActivities);
		act.execute();
	}
	
	private Activity pop() {
		scheduledActivities.addAll(pendingScheduledActivities);
		immediateActivities.addAll(Lists.reverse(pendingImmediateActivities));
		pendingScheduledActivities.clear();
		pendingImmediateActivities.clear();
		
		Activity act;
		if (!immediateActivities.isEmpty()) {
			act = immediateActivities.remove();
		} else {
			Entry<TimeStamp, Activity> scheduledAct = scheduledActivities.remove();
			if (scheduledAct.getKey().after(currentTime))
				currentTime = scheduledAct.getKey();
			act = scheduledAct.getValue();
		}
		return act;
	}
	
	public void propogateInformation() {
		while (!immediateActivities.isEmpty() || !pendingImmediateActivities.isEmpty() ||
				!scheduledActivities.isEmpty() || !pendingScheduledActivities.isEmpty()) {
			Activity act = pop();
			if (act instanceof InformationActivity)
				act.execute();
		}
	}

	/**
	 * Schedule an activity to happen as some point in the future. Any
	 * activities that are scheduled at the same point in the future will have a
	 * non deterministic ordering. To ensure a specific ordering for activities
	 * scheduled at the same time in the future, use the method
	 * <code>scheduleActivities</code>
	 */
	public void scheduleActivity(TimeStamp scheduledTime, Activity act) {
		if (scheduledTime.before(currentTime))
			pendingImmediateActivities.add(act);
		else
			pendingScheduledActivities.put(scheduledTime, act);
	}
	
	public TimeStamp getCurrentTime() {
		return currentTime;
	}

	@Override
	public long getLogTime() {
		return currentTime.getInTicks();
	}

	@Override
	public int getLogTimePadding() {
		return 6;
	};
	
}
