package activity;

import java.util.Collection;

import event.*;

/**
 * Base class for any method that Entities may invoke. Includes abstract
 * method execute and a timestamp.
 * 
 * Based on the Command Pattern.
 * 
 * NOTE: If Activity has a negative TimeStamp, then it an "infinitely fast"
 * activity which has a variable TimeStamp.
 * 
 * @author ewah
 */
public abstract class Activity {

	protected final TimeStamp scheduledTime;
	
	public Activity(TimeStamp scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	/**
	 * Executes the activity on the given Entity.
	 * 
	 * @return hash table of generated Activity vectors, hashed by TimeStamp
	 */
	public abstract Collection<? extends Activity> execute(TimeStamp currentTime);

	/**
	 * @return time of activity
	 */
	public final TimeStamp getTime() {
		return scheduledTime;
	}

	public final String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int hashCode() {
		return scheduledTime.hashCode();
	}

	@Override
	public String toString() {
		return getName() + " :: " + scheduledTime;
	}
	
}
