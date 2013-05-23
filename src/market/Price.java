package market;

/**
 * Price class is wrapper for long; one unit represents one thousandth
 * of a dollar.
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
	
	/**
	 * Add price to this object.
	 * @param p
	 * @return
	 */
	public Price sum(Price p) {
		return new Price(this.price + p.price);
	}
	
	/**
	 * Subtract price from this object.
	 * @param p
	 * @return
	 */
	public Price diff(Price p) {
		return new Price(this.price - p.price);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
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
	

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
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
