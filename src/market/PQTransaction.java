package market;

import event.TimeStamp;

import org.apache.commons.lang3.builder.*;

/**
 * Price/quantity transaction class.
 * 
 * Note that transaction quantities are always positive.
 * 
 * @author ewah
 */
public class PQTransaction extends Transaction {
	
//	/**
//	 * generate PQTransaction with given price, quantity, buyerID, sellerID, timestamp
//	 * @param q quantity
//	 * @param p price
//	 * @param bID buyerID
//	 * @param sID sellerID
//	 * @param ts TimeStamp
//	 */
//	public PQTransaction(int q, Price p, int bID, int sID, TimeStamp ts)
//	{
//		quantity = new Integer(q);
//		price = p;
//		buyerID = new Integer(bID);
//		sellerID = new Integer(sID);
//		timestamp = ts;
//	}
	
	/**
	 * Generate PQTransaction with given price, quantity, buyerID, sellerID, buyBidID,
	 * sellBidID, TimeStamp, marketID.
	 * 
	 * @param q quantity
	 * @param p price
	 * @param bID buyerID
	 * @param sID sellerID
	 * @param bBidID buyBidID
	 * @param sBidID sellBidID
	 * @param ts TimeStamp
	 * @param mktID marketID
	 */
	public PQTransaction(int q, Price p, int bID, int sID, int bBidID, int sBidID, TimeStamp ts, int mktID)
	{
		quantity = new Integer(q);
		price = p;
		buyerID = new Integer(bID);
		sellerID = new Integer(sID);
		buyBidID = new Integer(bBidID);
		sellBidID = new Integer(sBidID);
		timestamp = ts;
		marketID = new Integer(mktID);
	}
	
//	/**
//	 * @param pq
//	 * @return
//	 */
//	public boolean equals(PQTransaction pq) {
//		if (pq.price.price == this.price.price &&
//			pq.quantity == this.quantity &&
//			pq.buyerID.equals(this.buyerID) &&
//			pq.sellerID.equals(this.sellerID) &&
//			pq.timestamp.equals(this.timestamp))
//			return true;
//		else
//			return false;
//	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
	        return true;
		if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
	    PQTransaction other = (PQTransaction) obj;
	    return new EqualsBuilder().
	    		append(price.price, other.price.price).
				append(quantity.intValue(), other.quantity.intValue()).
				append(buyerID.intValue(), other.buyerID.intValue()).
				append(sellerID.intValue(), other.sellerID.intValue()).
				append(buyBidID.intValue(), other.buyBidID.intValue()).
				append(sellBidID.intValue(), other.sellBidID.intValue()).
				append(marketID.intValue(), other.marketID.intValue()).
				append(timestamp.longValue(), other.timestamp.longValue()).
				isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).
				append(price.price).
				append(quantity.intValue()).
				append(buyerID.intValue()).
				append(sellerID.intValue()).
				append(buyBidID.intValue()).
				append(sellBidID.intValue()).
				append(marketID.intValue()).
				append(timestamp.longValue()).
				toHashCode();
	}
	
	@Override
	public String toString() {
		String result = "Transaction(quantity=" + quantity.toString()
				+ ", price=" + price    
				+ ", buyerID=" + buyerID.toString()  
				+ ", sellerID=" + sellerID.toString() 
				+ ", buyBidID=" + buyBidID.toString()  
				+ ", sellBidID=" + sellBidID.toString() 
				+ ", timestamp=" + timestamp.toString()
				+ ", marketID=" + marketID.toString()
				+ ")";
		return result;
	}

	/**
	 * print the Transaction to stdout
	 */
	public void print()
	{
		System.out.print(quantity 	+" "+ 
				price    			+" "+
				buyerID.toString()  +" "+
				sellerID.toString() +" "+ 
				buyBidID.toString()  +" "+
				sellBidID.toString() +" "+ 
				timestamp.toString()+" "+
				marketID.toString()+"\n");
	}
	
//	/**
//	 * set all class members to null
//	 */
//	private void setMembersNull()
//	{
//		quantity = null;
//		price = null;
//		timestamp = null;
//		buyerID = null;
//		sellerID = null;
//		buyBidID = null;
//		sellBidID = null;
//		marketID = null;
//	}

}
