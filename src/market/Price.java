package market;

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

	/**
	 * Add price to this object.
	 */
	public Price sum(Price p) {
		return new Price(this.price + p.price);
	}

	/**
	 * Subtract price from this object.
	 */
	public Price diff(Price p) {
		return new Price(this.price - p.price);
	}

	@Override
	public int compareTo(Price o) {
		return this.price - o.price;
	}

	@Override
	public String toString() {
		return Integer.toString(price);
	}

}
