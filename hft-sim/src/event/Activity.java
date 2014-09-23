package event;


/**
 * Base class for any method that Entities may invoke. Includes abstract
 * method execute and a timestamp.
 * 
 * Based on the Command Pattern.
 * 
 * @author ewah
 */
public interface Activity {

	/** Executes the activity */
	public abstract void execute();
	
}
