package activity;

import event.*;

/**
 * Interface for any method that Entities may invoke. Based on the Command Pattern.
 * 
 * @author ewah
 */
public interface Activity {
	
	/**
	 * Executes the activity on the given Entity.
	 * @return		Event to be added to EventQueue. If no new event, returns null.
	 */
	public Event execute();
	
}
