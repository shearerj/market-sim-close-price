package market;

import utils.MathUtils;

/**
 * Price class is wrapper for long; one unit represents one thousandth of a
 * dollar.
 * 
 * @author ewah
 */
public class Price implements Comparable<Price> {

	public static int PRICE_PER_DOLLAR = 1000;

	protected final int price;

	/**
	 * Constructor initializes price to zero.
	 */
	public Price() {
		price = 0;
	}

	/**
	 * Constructor taking in a int.
	 * 
	 * @param p
	 */
	public Price(int p) {
		// TODO Decide if negative numbers should be allowed and how to include
		// this in the "diff" function
		price = p;
	}

	public int getPrice() {
		return price;
	}

	/**
	 * @return price in dollars (3 decimal places)
	 */
	public double getDollarPrice() {
		return price / (double) PRICE_PER_DOLLAR;
	}
	
	public Price quantize(int quanta) {
		return new Price(MathUtils.quantize(price, quanta));
	}

	/**
	 * Add price to this object.
	 */
	public Price plus(Price p) {
		return new Price(this.price + p.price);
	}

	/**
	 * Subtract price from this object.
	 */
	public Price minus(Price p) {
		return new Price(this.price - p.price);
	}

	@Override
	public int compareTo(Price o) {
		if (o == null) return 1;
		return this.price - o.price;
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
		return price;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Price)) return false;
		Price other = (Price) obj;
		return price == other.price;
	}

	@Override
	public String toString() {
		return Integer.toString(price);
	}

}
