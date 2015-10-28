package edu.umich.srg.marketsim.event;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import edu.umich.srg.collect.RandomKeyedQueue;
import edu.umich.srg.marketsim.TimeStamp;

import java.util.Map.Entry;
import java.util.Random;

/**
 * EventQueue holds the ordering of activities. Scheduled activities that occur at the same time are
 * randomly ordered, with the constraint that activities scheduled at the future time, and between
 * pops happen in the same order they were scheduled in. If the sequence of scheduled activities
 * were queued "pop A pop B C pop" then they could happen in order "A B C", "B A C", or "B C A".
 */
public class EventQueue {

  // FIXME Decide how time table for simulation is going to work. Is it closed
  // open, closed closed, or open closed; (open closed?)

  private TimeStamp currentTime;

  private final RandomKeyedQueue<TimeStamp, Runnable> scheduledActivities;
  private final ListMultimap<TimeStamp, Runnable> pendingScheduledActivities;

  public EventQueue(Random rand) {
    this.scheduledActivities = RandomKeyedQueue.create(rand);
    this.pendingScheduledActivities = ArrayListMultimap.create();
    this.currentTime = TimeStamp.ZERO;
  }

  // TODO Make this more efficient by using a sorted Multimap
  private boolean moreScheduledActivities(TimeStamp time) {
    if (!scheduledActivities.isEmpty() && scheduledActivities.peek().getKey().compareTo(time) <= 0)
      return true;
    for (TimeStamp scheduledTime : pendingScheduledActivities.keySet())
      if (scheduledTime.compareTo(time) <= 0)
        return true;
    return false;
  }

  public void executeUntil(TimeStamp time) {
    while (moreScheduledActivities(time)) {
      Entry<TimeStamp, Runnable> act = pop();
      currentTime = act.getKey();
      // FIXME These don't have nice representations...
      // log.debug("Executing {%s} then {%s}", act.getValue(), scheduledActivities);
      act.getValue().run();
    }
    if (time.compareTo(currentTime) > 0)
      currentTime = time;
  }

  private Entry<TimeStamp, Runnable> pop() {
    scheduledActivities.addAll(pendingScheduledActivities);
    pendingScheduledActivities.clear();

    Entry<TimeStamp, Runnable> scheduledAct = scheduledActivities.remove();
    assert scheduledAct.getKey().compareTo(currentTime) >= 0 : "Activities aren't in proper order";
    return scheduledAct;
  }

  /**
   * Schedule an activity to happen as some point in the future. Any activities that are scheduled
   * at the same point in the future will have a non deterministic ordering. To ensure a specific
   * ordering for activities scheduled at the same time in the future, use the method
   * <code>scheduleActivities</code>
   */
  public void scheduleActivityIn(TimeStamp delay, Runnable act) {
    checkArgument(delay.compareTo(TimeStamp.ZERO) > 0);
    pendingScheduledActivities.put(TimeStamp.of(currentTime.get() + delay.get()), act);
  }

  public TimeStamp getCurrentTime() {
    return currentTime;
  }

}
