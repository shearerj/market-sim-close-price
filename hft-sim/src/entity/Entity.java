package entity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import logger.Log.Level;
import systemmanager.Simulation;
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

	private static final long serialVersionUID = -7406324829959902527L;
	
	protected final Simulation sim;
	protected final int id;
	
	protected Entity(int agentID, Simulation sim) {
		this.id = agentID;
		this.sim = checkNotNull(sim);
	}
	
	protected String name() {
		return getClass().getSimpleName();
	}
	
	protected void log(Level level, String format, Object... parameters) {
		sim.log(level, format, parameters);
	}
	
	protected TimeStamp currentTime() {
		return sim.getCurrentTime();
	}
	
	protected void postStat(String name, double value) {
		sim.postStat(name, value);
	}
	
	protected void postStat(String name, double value, long times) {
		sim.postStat(name, value, times);
	}
	
	protected void postTimedStat(String name, double value) {
		sim.postTimedStat(name, value);
	}
	
	protected final int getID() {
		return this.id;
	}

	@Override
	public String toString() {
		return "(" + id + ')';
	}
	
}
