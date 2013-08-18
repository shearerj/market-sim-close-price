package entity.market;

import java.io.Serializable;
import static java.math.RoundingMode.HALF_EVEN;

import com.google.common.base.Objects;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Ints;

import utils.MathUtils;

/**
 * Price class is wrapper for long; one unit represents one thousandth of a
 * dollar.
 * 
 * @author ewah
 */
public class Price implements Comparable<Price>, Serializable {

	private static final long serialVersionUID = 772101228717034473L;
	
	// XXX Take infinite price into account for subtraction, addition,
	// toString, etc. Basically, should infinite price be special?
	public static final Price INF = new Price(Integer.MAX_VALUE - 1);
	public static final Price ZERO = new Price(0);

	public static int TICKS_PER_DOLLAR = 1000;

	protected final int ticks; // in ticks

	/**
	 * Constructor taking in a int.
	 * 
	 * @param ticks
	 */
	public Price(int ticks) {
		// XXX Decide if negative numbers should be allowed and how to include
		// this in the "diff" function
		this.ticks = ticks;
	}
	
	public Price(double ticks) {
		this(DoubleMath.roundToInt(ticks, HALF_EVEN));
	}

	public int getInTicks() {
		return ticks;
	}

	/**
	 * @return price in dollars
	 */
	public double getInDollars() {
		return ticks / (double) TICKS_PER_DOLLAR;
	}

	public Price quantize(int quanta) {
		return new Price(MathUtils.quantize(ticks, quanta));
	}

	/**
	 * Add price to this object.
	 */
	public Price plus(Price p) {
		return new Price(this.ticks + p.ticks);
	}

	/**
	 * Subtract price from this object.
	 */
	public Price minus(Price p) {
		return new Price(this.ticks - p.ticks);
	}
	
	public Price times(double x) {
		return new Price((int) (ticks * x));
	}
	
	public Price times(int x) {
		return new Price(ticks * x);
	}

	public Price nonnegative() {
		if (ticks < 0)
			return ZERO;
		return this;
	}

	/**
	 * Any price is greater than null
	 */
	@Override
	public int compareTo(Price o) {
		if (o == null)
			return 1;
		return Ints.compare(ticks, o.ticks);
	}

	/**
	 * True if p is null or this price is strictly greater
	 */
	public boolean greaterThan(Price p) {
		return p == null || compareTo(p) > 0;
	}

	/**
	 * True if p is null or this price is strictly less
	 */
	public boolean lessThan(Price p) {
		return p == null || compareTo(p) < 0;
	}

	/**
	 * True if p is null or this price is greater or equal
	 */
	public boolean greaterThanEquals(Price p) {
		return p == null || compareTo(p) >= 0;
	}

	/**
	 * True if p is null or this price is less or equal
	 */
	public boolean lessThanEqual(Price p) {
		return p == null || compareTo(p) <= 0;
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
		return ticks == other.ticks;
	}
	
	@Override
	public String toString() {
		int dollars = ticks / TICKS_PER_DOLLAR;
		int digits = MathUtils.logn(TICKS_PER_DOLLAR, 10);
		int cents = ticks % TICKS_PER_DOLLAR;
		while (digits > 2 && cents % 10 == 0) {
			cents /= 10;
			digits--;
		}
		
		return String.format("$%d.%0" + digits + "d", dollars, cents);
	}

}
