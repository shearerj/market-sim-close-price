package entity;

import systemmanager.*;

/**
 * This class is the base for all things that may perform an action/activity 
 * in the simulation (e.g. agents, markets, etc).
 * 
 * Acts as the Receiver class in the Command pattern.
 * 
 * @author ewah
 */
public abstract class Entity {

	public Log log;
	public SystemData data;
	protected int ID;
	protected ObjectProperties params;		// stores all parameters
	
	public Entity() {
		// empty constructor
	}

	/**
	 * Constructor
	 * @param ID
	 * @param l
	 * @param d
	 */
	public Entity(int ID, SystemData d, ObjectProperties ep, Log l) {
		this.ID = ID;
		this.data = d;
		this.params = ep;
		this.log = l;
	}
	
	/**
	 * Gets Entity's ID.
	 * @return ID
	 */
	public final int getID() {
		return this.ID;
	}
	
	/**
	 * Sets Entity's ID.
	 * @param ID
	 */
	public final void setID(int ID) {
		this.ID = ID;
	}
}
