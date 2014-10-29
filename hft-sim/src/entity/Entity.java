package entity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Random;

import logger.Log;
import logger.Log.Level;
import data.Stats;
import event.Activity;
import event.TimeLine;
import event.TimeStamp;

/**
 * This class is the base for all things that may perform an action/activity 
 * in the simulation (e.g. agents, markets, etc).
 * 
 * Acts as the Receiver class in the Command pattern.
 * 
 * @author ewah
 */
public abstract class Entity implements Serializable {

	private final int id;
	private final Stats stats;
	private final TimeLine timeline;
	private final Log log;
	protected final Random rand;
	
	protected Entity(int id, Stats stats, TimeLine timeline, Log log, Random rand) {
		this.id = id;
		this.stats = checkNotNull(stats);
		this.timeline = checkNotNull(timeline);
		this.log = checkNotNull(log);
		this.rand = checkNotNull(rand);
	}
	
	protected final int getID() {
		return id;
	}

	protected String name() {
		return getClass().getSimpleName();
	}
	
	@Override
	public String toString() {
		return "(" + id + ')';
	}

	protected final void log(Level level, String format, Object... parameters) {
		log.log(level, format, parameters);
	}
	
	protected final TimeStamp getCurrentTime() {
		return timeline.getCurrentTime();
	}
	
	protected final void scheduleActivityIn(TimeStamp delay, Activity act) {
		timeline.scheduleActivityIn(delay, act);
	}

	protected final void postStat(String name, double value) {
		stats.post(name, value);
	}
	
	protected final void postStat(String name, double value, long times) {
		stats.post(name, value, times);
	}
	
	protected final void postTimedStat(String name, double value) {
		stats.postTimed(timeline.getCurrentTime(), name, value);
	}
	
	private static final long serialVersionUID = -7406324829959902527L;
	
}
