package activity.market;

import java.text.DecimalFormat;

public class Price implements Comparable<Object> {

	protected double price;
	
	/**
	 * Constructor initializes price to zero.
	 */
	public Price() {
		price = 0;
	}
	
	/**
	 * Constructor taking in a float.
	 * @param p
	 */
	public Price(float p) {
		price = (double) p;
	}
	
	/**
	 * Constructor taking in a double.
	 * @param p
	 */
	public Price(double p) {
		price = p;
	}
	
	/**
	 * @return price as double
	 */
	public double getPrice() {
		return new Double(price).doubleValue();
	}
	
	/**
	 * @return price as float
	 */
	public float floatValue() {
		return new Double(price).floatValue();
	}
	
	/**
	 * @return price as int
	 */
	public int intValue() {
		return new Double(price).intValue();
	}
	
	/**
	 * @return price as Double
	 */
	public Double toDouble() {
		return new Double(price);
	}
	
	/* 
	 * Compares price to another object's price.
	 */
	public int compareTo(Object o) {
		return Double.compare(price, ((Price) o).getPrice());
	}
	
	/**
	 * @param numDecPlaces
	 * @return string representation with the specified # of decimal places
	 */
	public String toString(int numDecPlaces)
	{
		String str = "#.";

		for (int i = 0; i < numDecPlaces; i++)
			str = str.concat("0");

		return new DecimalFormat(str).format(price);
	}
	
	/* 
	 * Converts price to String.
	 */
	public String toString() {
		return new Double(price).toString();
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
