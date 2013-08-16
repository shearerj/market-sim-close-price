package activity;

import java.util.Collection;

import event.*;

/**
 * Base class for any method that Entities may invoke. Includes abstract
 * method execute and a timestamp.
 * 
 * Based on the Command Pattern.
 * 
 * NOTE: If Activity has a negative scheduledTime, then it an "infinitely fast"
 * activity and will be executed immediately
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
	
	@Override
	public final int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " :: ";
	}
	
}
