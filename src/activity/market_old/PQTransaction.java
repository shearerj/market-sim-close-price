package activity.market;

import event.TimeStamp;

/**
 * Price/quantity transaction class.
 * 
 * @author ewah
 */
public class PQTransaction extends Transaction {
	
    /**
	 * generate PQTransaction with given price, quantity, buyerID, sellerID, timestamp
	 * @param q quantity
	 * @param p price
	 * @param bID buyerID
	 * @param sID sellerID
	 * @param ts TimeStamp
	 */
	public PQTransaction(int q, Price p, int bID, int sID, TimeStamp ts)
	{
		quantity = new Long(q);
		price = p;
		buyerID = new Integer(bID);
		sellerID = new Integer(sID);
		timestamp = ts;
	}
	
	/**
	 * generate PQTransaction with given price, quantity, buyerID, sellerID, timestamp, marketID
	 * @param q quantity
	 * @param p price
	 * @param bID buyerID
	 * @param sID sellerID
	 * @param ts timestamp
	 * @param aucID marketID
	 */
	public PQTransaction(int q, Price p, int bID,int sID,TimeStamp ts, int aucID)
	{
		quantity = new Long(q);
		price = p;
		buyerID = new Integer(bID);
		sellerID = new Integer(sID);
		timestamp = ts;
		marketID = new Integer(aucID);
	}


	public String toString() {
		String result = "Transaction(quantity=" + this.quantity 
				+ ", price=" + price    
				+ ", buyerID=" + buyerID.toString()  
				+ ", sellerID=" + sellerID.toString() 
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
				timestamp.toString()+" "+
				marketID.toString()+"\n");
	}
	
	/**
	 * set all class members to null
	 */
	private void setMembersNull()
	{
		quantity = null;
		price = null;
		timestamp = null;
		buyerID = null;
		sellerID = null;
		marketID = null;
		type = null;
	}

}
