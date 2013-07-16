/**
 *  $Id: TimeStamp.java,v 1.6 2003/11/06 20:00:35 omalley Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 *  
 *  Edited 2012/05/28 by ewah
 */
package event;

/**
 * The TimeStamp class is just a wrapper around java.lang.Long. This *must*
 * remain an immutable object
 */
public class TimeStamp implements Comparable<TimeStamp> {
	
	public static final TimeStamp IMMEDIATE = new TimeStamp(-1);
	public static final TimeStamp ZERO = new TimeStamp(0);
	
	public static final int TICKS_PER_SECOND = 1000000;
	
	protected final long time;

	public TimeStamp(long l) {
		time = l;
	}

	public TimeStamp(int i) {
		time = (long) i;
	}
	
	/**
	 * Convert the timestamp to seconds
	 * 
	 * @return the TimeStamp's receipt timestamp in seconds
	 */
	public double getInSeconds() {
		return time / (double) TICKS_PER_SECOND;
	}

	/**
	 * Diff the object's timestamp with the specified value.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return the difference
	 */
	public TimeStamp minus(TimeStamp other) {
		return new TimeStamp(this.time - other.time);
	}

	/**
	 * Sum the object's timestamp with the specified value.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return the sum
	 */
	public TimeStamp plus(TimeStamp other) {
		return new TimeStamp(this.time + other.time);
	}

	/**
	 * Convert the timestamp to a long.
	 * 
	 * @return the converted timestamp
	 */
	public long longValue() {
		return time;
	}

	/**
	 * Convert the timestamp to a string.
	 * 
	 * @return the converted timestamp
	 */
	public String toString() {
		return Long.toString(time);
	}

	/**
	 * Determines if the timestamp is before the specified timestamp.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return true if other is before, otherwise false
	 */
	public boolean before(TimeStamp other) {
		return time < other.time;
	}

	/**
	 * Determines if the timestamp is after the specified timestamp.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return true if other is after, otherwise false
	 */
	public boolean after(TimeStamp other) {
		return time > other.time;
	}

	/**
	 * Compares two timestamps.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return 0 if equal, <0 if invoking timestamp is less, >0 if greater
	 */
	public int compareTo(TimeStamp other) {
		return (int) Long.signum(time - other.time);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof TimeStamp))
			return false;
		final TimeStamp other = (TimeStamp) obj;
		return time == other.time;
	}

	@Override
	public int hashCode() {
		return (int) time;
	}

}
