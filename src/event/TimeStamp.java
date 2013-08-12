/**
 *  $Id: TimeStamp.java,v 1.6 2003/11/06 20:00:35 omalley Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 *  
 *  Edited 2012/05/28 by ewah
 */
package event;

import utils.MathUtils;

/**
 * The TimeStamp class is just a wrapper around java.lang.Long. This *must*
 * remain an immutable object
 */
public class TimeStamp implements Comparable<TimeStamp> {
	
	public static final TimeStamp IMMEDIATE = new TimeStamp(-1);
	public static final TimeStamp ZERO = new TimeStamp(0);
	
	public static final int TICKS_PER_SECOND = 1000000;
	
	protected final long ticks;

	public TimeStamp(long ticks) {
		this.ticks = ticks;
	}

	public TimeStamp(int ticks) {
		this((long) ticks);
	}
	
	public double getInSeconds() {
		return ticks / (double) TICKS_PER_SECOND;
	}

	/**
	 * Subtract two TimeStamps
	 */
	public TimeStamp minus(TimeStamp other) {
		return new TimeStamp(this.ticks - other.ticks);
	}

	/**
	 * Add two TimeStamps together
	 */
	public TimeStamp plus(TimeStamp other) {
		return new TimeStamp(this.ticks + other.ticks);
	}

	public long getInTicks() {
		return ticks;
	}

	/**
	 * @param other
	 * @return true if other is before or null
	 */
	public boolean before(TimeStamp other) {
		return other == null || ticks < other.ticks;
	}

	/**
	 * @param other
	 * @return true if other is after or null
	 */
	public boolean after(TimeStamp other) {
		return other == null || ticks > other.ticks;
	}

	public int compareTo(TimeStamp other) {
		// Signum prevents overflow issues
		return (int) Long.signum(ticks - other.ticks);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof TimeStamp))
			return false;
		final TimeStamp other = (TimeStamp) obj;
		return ticks == other.ticks;
	}

	@Override
	public int hashCode() {
		return (int) ticks;
	}
	
	@Override
	public String toString() {
		long seconds = Long.signum(ticks) * Math.abs(ticks / TICKS_PER_SECOND);
		int digits = MathUtils.logn(TICKS_PER_SECOND, 10);
		long microseconds = Math.abs(ticks % TICKS_PER_SECOND);
		while (digits > 3 && microseconds % 10 == 0) {
			microseconds /= 10;
			digits--;
		}
		
		return String.format("%d.%0" + digits + "ds", seconds, microseconds);
	}

}
