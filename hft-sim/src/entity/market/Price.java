package entity.market;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.math.RoundingMode.HALF_EVEN;

import java.math.RoundingMode;

import utils.Maths;

import com.google.common.base.Objects;
import com.google.common.collect.Ordering;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;

/**
 * Price class is wrapper for long; one unit represents one thousandth of a
 * dollar.
 * 
 * @author ewah
 */
public class Price extends Number implements Comparable<Price> {

	private static final long serialVersionUID = 772101228717034473L;
	protected static final Ordering<Price> ord = Ordering.natural();

	public static final Price INF = new Price(Integer.MAX_VALUE) {
		private static final long serialVersionUID = 1849387089333514388L;
		@Override public float floatValue() { return Float.POSITIVE_INFINITY; }
		@Override public double doubleValue() { return Double.POSITIVE_INFINITY; }
		@Override public Price quantize(int quanta) { return this; }
		@Override public int hashCode() { return System.identityHashCode(this); }
		@Override public String toString() { return "$" + doubleValue(); }
		@Override public String toDollarString() { return "$" + doubleValue(); }
	};
	public static final Price NEG_INF = new Price(Integer.MIN_VALUE) {
		private static final long serialVersionUID = -2568290536011656239L;
		@Override public float floatValue() { return Float.NEGATIVE_INFINITY; }
		@Override public double doubleValue() { return Double.NEGATIVE_INFINITY; }
		@Override public Price quantize(int quanta) { return this; }
		@Override public int hashCode() { return System.identityHashCode(this); }
		@Override public String toString() { return "$" + doubleValue(); }
		@Override public String toDollarString() { return "-$" + (-doubleValue()); }
	};
	public static final Price ZERO = new Price(0);

	public static int TICKS_PER_DOLLAR = 1000;

	protected final int ticks; // in ticks

	private Price(int ticks) {
		this.ticks = ticks;
	}
	
	public static Price of(int ticks) {
		return new Price(ticks);
	}
	
	public static Price of(double ticks) {
		checkArgument(!Double.isNaN(ticks));
		if (ticks > Integer.MAX_VALUE) 
			return INF;
		else if (ticks < Integer.MIN_VALUE)
			return NEG_INF;
		else 
			return new Price(DoubleMath.roundToInt(ticks, HALF_EVEN));
	}
	
	@Override
	public int intValue() {
		return ticks;
	}

	@Override
	public long longValue() {
		return ticks;
	}

	@Override
	public float floatValue() {
		return ticks;
	}

	@Override
	public double doubleValue() {
		return ticks;
	}

	/*
	 * FIXME This quantizes by rounding, but for buy or sell orders you may
	 * want to quantize directionally, not to the nearest one.
	 */
	public Price quantize(int quanta) {
		return new Price(Maths.quantize(ticks, quanta));
	}
	
	/**
	 * @return price in dollars
	 */
	public double getInDollars() {
		return doubleValue() / TICKS_PER_DOLLAR;
	}

	/**
	 * Return 0 if price is negative
	 * @return Non-negative version of the price.
	 */
	public Price nonnegative() {
		return ord.max(this, ZERO);
	}
	
	@Override
	public int compareTo(Price price) {
		checkNotNull(price);
		if (this == INF) {
			if (price == INF) {
				return 0;
			}

			return 1;
		} else if (this == NEG_INF) {
			if (price == NEG_INF) {
				return 0;
			}

			return -1;
		} else {
			return Ints.compare(ticks, price.ticks);
		}
	}

	/**
	 * True if p is null or this price is strictly greater
	 */
	public boolean greaterThan(Price p) {
		return ord.compare(this, p) > 0;
	}

	/**
	 * True if p is null or this price is strictly less
	 */
	public boolean lessThan(Price p) {
		return ord.compare(this, p) < 0;
	}

	/**
	 * True if p is null or this price is greater or equal
	 */
	public boolean greaterThanEqual(Price p) {
		return ord.compare(this, p) >= 0;
	}

	/**
	 * True if p is null or this price is less or equal
	 */
	public boolean lessThanEqual(Price p) {
		return ord.compare(this, p) <= 0;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(ticks);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Price))
			return false;
		Price other = (Price) obj;
		if (this == INF)
			return other == INF;
		else if (this == NEG_INF)
			return other == NEG_INF;
		else
			return ticks == other.ticks;
	}
	
	@Override
	public String toString() {
		return '$' + Long.toString(ticks);
	}
	
	public String toDollarString() {
		int absTicks = Math.abs(ticks); 
		int dollars = absTicks / TICKS_PER_DOLLAR;
		int digits = IntMath.log10(TICKS_PER_DOLLAR, RoundingMode.HALF_EVEN);
		int cents = absTicks % TICKS_PER_DOLLAR;
		while (digits > 2 && cents % 10 == 0) {
			cents /= 10;
			digits--;
		}
		
		return String.format("%s$%d.%0" + digits + "d", ticks < 0 ? "-" : "", dollars, cents);
	}

}
