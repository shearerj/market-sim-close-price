/**
 *  $Id: TimeStamp.java,v 1.6 2003/11/06 20:00:35 omalley Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 *  
 *  Edited 2012/05/28 by ewah
 */
package event;

import java.io.Serializable;
import java.math.RoundingMode;

import com.google.common.base.Objects;
import com.google.common.math.IntMath;
import com.google.common.primitives.Longs;

/**
 * The TimeStamp class is just a wrapper around java.lang.Long. This *must*
 * remain an immutable object
 */
public class TimeStamp implements Comparable<TimeStamp>, Serializable {
	
	private static final long serialVersionUID = -2109498445060507654L;
	
	public static final TimeStamp ZERO = new TimeStamp(0);
	public static final int TICKS_PER_SECOND = 1000000;
	
	protected final long ticks;

	protected TimeStamp(long ticks) {
		this.ticks = ticks;
	}
	
	public static TimeStamp of(long ticks) {
		return new TimeStamp(ticks);
	}

	public double getInSeconds() {
		return ticks / (double) TICKS_PER_SECOND;
	}

	/**
	 * Subtract two TimeStamps
	 */
	public TimeStamp minus(TimeStamp other) {
		return TimeStamp.of(this.ticks - other.ticks);
	}

	/**
	 * Add two TimeStamps together
	 */
	public TimeStamp plus(TimeStamp other) {
		return TimeStamp.of(this.ticks + other.ticks);
	}

	public long getInTicks() {
		return ticks;
	}

	/**
	 * @param other
	 * @return true if other is before or null
	 */
	public boolean before(TimeStamp other) {
		return this.compareTo(other) < 0;
	}
	
	public boolean beforeOrOn(TimeStamp other) {
		return this.compareTo(other) <= 0;
	}

	/**
	 * @param other
	 * @return true if other is after or null
	 */
	public boolean after(TimeStamp other) {
		return this.compareTo(other) > 0;
	}
	
	public boolean afterOrOn(TimeStamp other) {
		return this.compareTo(other) >= 0;
	}

	@Override
	public int compareTo(TimeStamp other) {
		return Longs.compare(ticks, other.ticks);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof TimeStamp)) return false;
		final TimeStamp other = (TimeStamp) obj;
		return ticks == other.ticks;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(ticks);
	}
	
	@Override
	public String toString() {
		return ticks + "ms";
	}
	
	// Fancy formatting
	public String toSecondsString() {
		long seconds = Long.signum(ticks) * Math.abs(ticks / TICKS_PER_SECOND);
		int digits = IntMath.log10(TICKS_PER_SECOND, RoundingMode.HALF_EVEN);
		long microseconds = Math.abs(ticks % TICKS_PER_SECOND);
		while (digits > 3 && microseconds % 10 == 0) {
			microseconds /= 10;
			digits--;
		}
		return String.format("%d.%0" + digits + "ds", seconds, microseconds);
	}

}
