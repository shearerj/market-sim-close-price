package entity;

import java.util.LinkedList;

import event.*;
import activity.*;
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

	protected int ID;
	public Log log;
	public SystemData data;
	
	public Entity() {
		// empty constructor
	}

	/**
	 * Constructor
	 * @param ID
	 * @param l
	 * @param d
	 */
	public Entity(int ID, SystemData d) {
		this.ID = ID;
		this.data = d;
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
	
	/**
	 * Create Event with empty list of Activities.
	 * @param t
	 * @return Event
	 */
//	public Event createEvent(TimeStamp t) {
//		LinkedList<Activity> activities = new LinkedList<Activity>();
//		Event e = new Event(t, activities);
//		return e;
//	}
	
	/**
	 * Create Event with a given list of Activities.
	 * @param t
	 * @param activities
	 * @return
	 */
//	public Event createEvent(TimeStamp t, LinkedList<Activity> activities) {
//		Event e = new Event(t, activities);
//		return e;
//	}
}
