package entity;

import data.EntityProperties;
import data.SystemData;

/**
 * This class is the base for all things that may perform an action/activity 
 * in the simulation (e.g. agents, markets, etc).
 * 
 * Acts as the Receiver class in the Command pattern.
 * 
 * @author ewah
 */
public abstract class Entity {

	public SystemData data;
	protected final int id;
	protected EntityProperties params;		// stores all parameters
	
	public Entity(int agentID) {
		id = agentID;
	}
	
	/**
	 * Gets Entity's ID.
	 * @return ID
	 */
	public final int getID() {
		return this.id;
	}
	
	/**
	 * @return simple class name
	 */
	public String getName() {
		return this.getClass().getSimpleName();
	}
	
}
