/**
 *  $Id: TimeStamp.java,v 1.6 2003/11/06 20:00:35 omalley Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 *  
 *  Edited 2012/05/28 by ewah
 */
package event;

import java.util.*;

import activity.*;

/**
 * The TimeStamp class is just a wrapper around java.lang.Long.
 * This *must* remain an immutable object
 */
public class TimeStamp implements Comparable<TimeStamp>
{
	/* should these be made private?  */
	private Long ts;
	private static long preTS = -1;		// TimeStamp assigned to PRE activities
	private static long postTS = -2;	// TimeStamp assigned to POST activities
	
	public TimeStamp()          { ts = new Long(new Date().getTime());}
	public TimeStamp(Date d)    { ts = new Long(d.getTime());}
	public TimeStamp(Long l)    { ts = l;}
	public TimeStamp(Integer i) { ts = new Long(i.longValue());}
	public TimeStamp(long l)    { ts = new Long(l);}
	public TimeStamp(int i)     { ts = new Long((new Integer(i)).longValue());}
	public TimeStamp(String s)  { ts = new Long(s);}

	/**
	 * Constructor to handle infinitely fast activity times.
	 * @param t
	 */
	public TimeStamp(EventManager.FastActivityType t) {
		switch (t) {
		case POST:
			ts = new Long(postTS);
			break;
			
		case PRE:
			ts = new Long(preTS);
			break;
			
		default:
			ts = new Long(0);
			break;
		}
	}
	
	/**
	 * @return true if TimeStamp is a PRE activity.
	 */
	public boolean isFastPreActivity() {
		if (ts.longValue() == preTS) return true;
		return false;
	}
	
	/**
	 * @return true if TimeStamp is a POST activity.
	 */
	public boolean isFastPostActivity() {
		if (ts.longValue() == postTS) return true;
		return false;
	}
	
	/**
	 * Get the timestamp.
	 * @return the TimeStamp's receipt timestamp in microseconds
	 */
	public Long getTimeStamp() {
		return ts;
	}

	/**
	 * Convert the timestamp to seconds
	 * @return the TimeStamp's receipt timestamp in seconds
	 */
	public long getTimeStampInSecs() {
		return ts.longValue()/1000000;
	}

	/**
	 * Diff the object's timestamp with the specified value.
	 * @param other the comparison timestamp
	 * @return the difference
	 */
	public TimeStamp diff(TimeStamp other) {
		return new TimeStamp(ts.longValue() - other.longValue());
	}

	/**
	 * Diff the timestamp.
	 * @param t1 the comparison timestamp
	 * @param t2 the comparison timestamp
	 * @return the difference
	 */
	public static TimeStamp diff(TimeStamp t1, TimeStamp t2) {
		return new TimeStamp(t1.longValue() - t2.longValue());
	}

	/**
	 * Sum the timestamp.
	 * @param t1 the comparison timestamp
	 * @param t2 the comparison timestamp
	 * @return the sum
	 */
	public static TimeStamp sum(TimeStamp t1, TimeStamp t2) {
		return new TimeStamp(t1.longValue() + t2.longValue());
	}

	/**
	 * Sum the object's timestamp with the specified value.
	 * @param other the comparison timestamp
	 * @return the sum
	 */
	public TimeStamp sum(TimeStamp other) {
		return new TimeStamp(ts.longValue() + other.longValue());
	}

	/**
	 * Convert the timestamp to a long.
	 * @return the converted timestamp
	 */
	public long longValue() {
		return ts.longValue();
	}

	/**
	 * Convert the timestamp to a string.
	 * @return the converted timestamp
	 */
	public String toString() {
		return ts.toString();
	}

	/**
	 * Determines if the timestamp is before the specified timestamp.
	 * @param other the comparison timestamp
	 * @return true if other is before, otherwise false
	 */
	public boolean before(TimeStamp other) {
		return (ts.compareTo(other.ts) < 0);
	}

	/**
	 * Determines if the timestamp is after the specified timestamp.
	 * @param other the comparison timestamp
	 * @return true if other is after, otherwise false
	 */
	public boolean after(TimeStamp other) {
		return (ts.compareTo(other.ts) > 0);
	}

	/**
	 * Compares two timestamps.
	 * @param other the comparison timestamp
	 * @return 0 if equal, <0 if invoking timestamp is less, >0 if greater
	 */
	public int compareTo(TimeStamp other) {
		return ts.compareTo(other.ts);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
	        return true;
	    if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
	    final TimeStamp other = (TimeStamp) obj;
	    if (ts.compareTo(other.ts) != 0)
	        return false;
	    return true;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return ts.intValue();
	}
	
	/**
	 * Verifies whether or not the Activity's TimeStamp matches the given one.
	 * 
	 * @param act
	 * @return true if matches, false otherwise.
	 */
	public boolean checkActivityTimeStamp(Activity act) {
		if (act.getTime().compareTo(this) == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Verifies whether or not the Activities in the list match the given TimeStamp.
	 * 
	 * @param acts
	 * @return true if matches, false otherwise.
	 */
	public boolean checkActivityTimeStamp(LinkedList<Activity> acts) {
		for (Iterator<Activity> it = acts.iterator(); it.hasNext(); ) {
			if (!checkActivityTimeStamp(it.next())) {
				System.out.println("TimeStamp::checkActivityTimeStamp::ERROR: activities do not match the timestamp");
				return false;
			}
		}
		return true;
	}
}
