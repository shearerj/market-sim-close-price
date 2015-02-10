package event;

import static logger.Log.Level.DEBUG;

import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;

import logger.Log;
import utils.Collections3;
import utils.RandomKeyedQueue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * EventQueue hold the ordering of activities. There are immediate and scheduled
 * activities, and they are bundled between successive pops. Immediate
 * activities always happen before scheduled activities. Between pops, immediate
 * activities are scheduled in the order they are queued, but between pops, they
 * are in the reverse of that order. If the sequence of immediate activities
 * queued "pop A pop B C pop", they would happen in order "B C A". Scheduled
 * activities that occur at the same time are randomly ordered, with the
 * constraint that activities scheduled at the future time, and between pops
 * happen in the same order they were scheduled in. If the sequence of scheduled
 * activities were queued "pop A pop B C pop" then they could happen in order
 * "A B C", "B A C", or "B C A".
 * 
 * @author erik
 * 
 */
public class EventQueue implements Timeline {
	
	private final Log log;
	private TimeStamp currentTime;
	
	private final RandomKeyedQueue<TimeStamp, Activity> scheduledActivities;
	private final Queue<Activity> immediateActivities;
	
	private final ListMultimap<TimeStamp, Activity> pendingScheduledActivities; 
	private final List<Activity> pendingImmediateActivities;
	
	protected EventQueue(Log log, Random rand) {
		this.log = log;
		this.scheduledActivities = RandomKeyedQueue.create(rand);
		this.immediateActivities = Collections3.asLifoQueue(Lists.<Activity> newArrayList());
		this.pendingScheduledActivities = ArrayListMultimap.create();
		this.pendingImmediateActivities = Lists.newArrayList();
		this.currentTime = TimeStamp.ZERO;
	}
	
	public static EventQueue create(Log log, Random rand) {
		return new EventQueue(log, rand);
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
		while (moreImmediateActivities() || moreScheduledActivities(time)) {
			Entry<TimeStamp, Activity> act = pop();
			currentTime = act.getKey();
			log.log(DEBUG, "Executing {%s} then immediately {%s} then {%s}", act.getValue(), immediateActivities, scheduledActivities);
			act.getValue().execute();
		}
		if (time.after(currentTime))
			currentTime = time;
	}
	
	private Entry<TimeStamp, Activity> pop() {
		scheduledActivities.addAll(pendingScheduledActivities);
		immediateActivities.addAll(Lists.reverse(pendingImmediateActivities));
		pendingScheduledActivities.clear();
		pendingImmediateActivities.clear();
		
		if (!immediateActivities.isEmpty()) {
			return Maps.immutableEntry(currentTime, immediateActivities.remove());
		}
		
		Entry<TimeStamp, Activity> scheduledAct = scheduledActivities.remove();
		assert scheduledAct.getKey().afterOrOn(currentTime) : "Activities aren't in proper order";
		return scheduledAct;
	}
	
	public void propagateInformation() {
		while (!immediateActivities.isEmpty() || !pendingImmediateActivities.isEmpty() ||
				!scheduledActivities.isEmpty() || !pendingScheduledActivities.isEmpty()) {
			Activity act = pop().getValue(); // Don't update time
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
	@Override
	public void scheduleActivityIn(TimeStamp delay, Activity act) {
		if (delay.after(TimeStamp.ZERO))
			pendingScheduledActivities.put(currentTime.plus(delay), act);
		else
			pendingImmediateActivities.add(act);
	}
	
	@Override
	public TimeStamp getCurrentTime() {
		return currentTime;
	}
	
}
