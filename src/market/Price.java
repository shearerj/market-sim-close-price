package market;

import java.text.DecimalFormat;

/**
 * Price class is wrapper for int; one unit represents one thousandth
 * of a collar.
 * 
 * @author ewah
 */
public class Price implements Comparable<Object> {

	protected int price;
	
	/**
	 * Constructor initializes price to zero.
	 */
	public Price() {
		price = 0;
	}

	/**
	 * Constructor taking in a int.
	 * @param p
	 */
	public Price(int p) {
		price = p;
	}
	
	/**
	 * @return price as int
	 */
	public int getPrice() {
		return new Integer(price).intValue();
	}
	
	/**
	 * @return price in dollars (3 decimal places)
	 */
	public double getDollarPrice() {
		return price / 1000;
	}
	
	/* 
	 * Compares price to another object's price.
	 */
	public int compareTo(Object o) {
		return Double.compare(price, ((Price) o).getPrice());
	}
	
//	/**
//	 * @param numDecPlaces
//	 * @return string representation with the specified # of decimal places
//	 */
//	public String toString(int numDecPlaces)
//	{
//		String str = "#.";
//
//		for (int i = 0; i < numDecPlaces; i++)
//			str = str.concat("0");
//
//		return new DecimalFormat(str).format(price);
//	}
	
	/* 
	 * Converts price to String.
	 */
	public String toString() {
		return new Integer(price).toString();
	}
	
	/**
	 * Returns maximum of two Prices.
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static Price max(Price p1, Price p2) {
		if (p1 == null) return p2;
		if (p2 == null) return p1;
		
		if (p1.compareTo(p2) > 0)
			return p1;
		else
			return p2;
	}

	/**
	 * Returns minimum of two Prices.
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static Price min(Price p1, Price p2) {
		if (p1 == null) return p2;
		if (p2 == null) return p1;
		
		if (p1.compareTo(p2) < 0)
			return p1;
		else
			return p2;
	}
}
