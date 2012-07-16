package activity;

import event.*;

/**
 * Base class for any method that Entities may invoke. Includes abstract
 * method execute and a timestamp. 
 * 
 * Based on the Command Pattern.
 * 
 * @author ewah
 */
public abstract class Activity {
	
	protected TimeStamp time;
	
	/**
	 * Executes the activity on the given Entity.
	 * @return hash table of generated Activity vectors, hashed by TimeStamp
	 */
	public abstract ActivityHashMap execute();
	
	/**
	 * @return TimeStamp of time variable
	 */
	public TimeStamp getTime() {
		return this.time;
	}
}
