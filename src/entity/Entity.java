package entity;

import data.ObjectProperties;
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
	protected ObjectProperties params;		// stores all parameters
	
	public Entity(int agentID) {
		id = agentID;
	}

	/**
	 * Constructor
	 * @param ID
	 * @param l
	 * @param d
	 */
	public Entity(int agentID, SystemData d, ObjectProperties ep) {
		this.id = agentID;
		this.data = d;
		this.params = ep;
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
