/**
 *  $Id: TimeStamp.java,v 1.6 2003/11/06 20:00:35 omalley Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 *  
 *  Edited 2012/05/28 by ewah
 */
package edu.umich.srg.marketsim;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;

/**
 * The TimeStamp class is just a wrapper around java.lang.Long. This *must*
 * remain an immutable object
 */
public class TimeStamp implements Comparable<TimeStamp>, Serializable {
	
	public static final TimeStamp ZERO = new TimeStamp(0);
	
	private final long ticks;

	private TimeStamp(long ticks) {
		this.ticks = ticks;
	}
	
	public static TimeStamp of(long ticks) {
		return new TimeStamp(ticks);
	}

	public long get() {
		return ticks;
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
		return ticks + "t";
	}

	private static final long serialVersionUID = -2109498445060507654L;

}
