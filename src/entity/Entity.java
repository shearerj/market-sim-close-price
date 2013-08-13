package entity;

import java.io.Serializable;

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
	
	protected final int id;
	
	public Entity(int agentID) {
		id = agentID;
	}
	
	public final int getID() {
		return this.id;
	}
	
	public final String getName() {
		return getClass().getSimpleName();
	}
	
	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public String toString() {
		return "(" + id + ")";
	}
	
}
