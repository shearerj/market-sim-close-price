package activity;

import java.util.Collection;

import event.*;

/**
 * Base class for any method that Entities may invoke. Includes abstract method
 * execute and a timestamp.
 * 
 * Based on the Command Pattern.
 * 
 * NOTE: If Activity has a negative TimeStamp, then it an "infinitely fast"
 * activity which has a variable TimeStamp.
 * 
 * @author ewah
 */
public abstract class Activity {

	protected final TimeStamp time;
	
	public Activity(TimeStamp t) {
		time = t;
	}

	/**
	 * Executes the activity on the given Entity.
	 * 
	 * @return hash table of generated Activity vectors, hashed by TimeStamp
	 */
	public abstract Collection<Activity> execute(TimeStamp currentTime);

	/**
	 * @return deep copy of the Activity.
	 */
	public abstract Activity deepCopy();

	/**
	 * @return TimeStamp of time variable
	 */
	public TimeStamp getTime() {
		return this.time;
	}

	/**
	 * @param obj
	 *            Object
	 * @return true if equal, false if not
	 */
	@Override
	public boolean equals(Object obj) {
		// FIXME It's probably better to have every activity implement its own
		// instead of using twoString
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Activity other = (Activity) obj;
		if (!other.toString().equals(this.toString()))
			return false;
		return true;
	}
	
}
