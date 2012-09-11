package activity;

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
	
	/**
	 * Sets the TimeStamp of the Activity.
	 * @param ts
	 */
	public void setTime(TimeStamp ts) {
		this.time = ts;
	}
	
	/**
	 * @param obj Object
	 * @return true if equal, false if not
	 */
	@Override
	public boolean equals(Object obj) {
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();
}
