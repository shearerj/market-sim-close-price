/**
 *  $Id: TimeStamp.java,v 1.6 2003/11/06 20:00:35 omalley Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 *  
 *  Edited 2012/05/28 by ewah
 */
package event;

import java.util.*;

import systemmanager.Consts;

import activity.Activity;

/**
 * The TimeStamp class is just a wrapper around java.lang.Long. This *must*
 * remain an immutable object
 */
public class TimeStamp implements Comparable<TimeStamp> {

	public static final TimeStamp startTime = new TimeStamp(0);
	
	protected final long ts;

	public TimeStamp(Date d) {
		ts = d.getTime();
	}

	public TimeStamp(Long l) {
		ts = l;
	}

	public TimeStamp(Integer i) {
		ts = (long) i;
	}

	public TimeStamp(long l) {
		ts = l;
	}

	public TimeStamp(int i) {
		ts = (long) i;
	}

	public TimeStamp(String s) {
		ts = Long.parseLong(s);
	}

	public TimeStamp(TimeStamp ts) {
		this.ts = new Long(ts.longValue());
	}

	/**
	 * @return true if TimeStamp is infinitely fast.
	 */
	public boolean isInfinitelyFast() {
		return this.equals(Consts.INF_TIME); // TODO change to reference check?
	}

	/**
	 * Get the timestamp.
	 * 
	 * @return the TimeStamp's receipt timestamp in microseconds
	 */
	public Long getLongValue() {
		return ts;
	}

	/**
	 * Convert the timestamp to seconds
	 * 
	 * @return the TimeStamp's receipt timestamp in seconds
	 */
	public long getTimeStampInSecs() {
		return ts / 1000000;
	}

	/**
	 * Diff the object's timestamp with the specified value.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return the difference
	 */
	public TimeStamp diff(TimeStamp other) {
		return diff(this, other);
	}

	/**
	 * Diff the timestamp.
	 * 
	 * @param t1
	 *            the comparison timestamp
	 * @param t2
	 *            the comparison timestamp
	 * @return the difference
	 */
	public static TimeStamp diff(TimeStamp t1, TimeStamp t2) {
		return new TimeStamp(t1.ts - t2.ts);
	}

	/**
	 * Sum the timestamp.
	 * 
	 * @param t1
	 *            the comparison timestamp
	 * @param t2
	 *            the comparison timestamp
	 * @return the sum
	 */
	public static TimeStamp sum(TimeStamp t1, TimeStamp t2) {
		return new TimeStamp(t1.ts + t2.ts);
	}

	/**
	 * Sum the object's timestamp with the specified value.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return the sum
	 */
	public TimeStamp sum(TimeStamp other) {
		return sum(this, other);
	}

	/**
	 * Convert the timestamp to a long.
	 * 
	 * @return the converted timestamp
	 */
	public long longValue() {
		return ts;
	}

	/**
	 * Convert the timestamp to a string.
	 * 
	 * @return the converted timestamp
	 */
	public String toString() {
		return Long.toString(ts);
	}

	/**
	 * Determines if the timestamp is before the specified timestamp.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return true if other is before, otherwise false
	 */
	public boolean before(TimeStamp other) {
		return ts < other.ts;
	}

	/**
	 * Determines if the timestamp is after the specified timestamp.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return true if other is after, otherwise false
	 */
	public boolean after(TimeStamp other) {
		return ts > other.ts;
	}

	/**
	 * Compares two timestamps.
	 * 
	 * @param other
	 *            the comparison timestamp
	 * @return 0 if equal, <0 if invoking timestamp is less, >0 if greater
	 */
	public int compareTo(TimeStamp other) {
		return (int) Long.signum(ts - other.ts);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TimeStamp other = (TimeStamp) obj;
		return ts == other.ts;
	}

	@Override
	public int hashCode() {
		return (int) ts;
	}

	/**
	 * Verifies whether or not the Activity's TimeStamp matches the given one.
	 * 
	 * @param act
	 * @return true if matches, false otherwise.
	 */
	public boolean checkActivityTimeStamp(Activity act) {
		return equals(act.getTime());
	}

}
