package market;

import event.TimeStamp;

/**
 * Base class for Transactions.
 * 
 * @author ewah
 */
public class Transaction {
	
	public Integer transID;
	public Integer buyerID;
	public Integer sellerID;
	public Integer marketID;
	
	public Integer quantity;
	public Price price;
	public TimeStamp timestamp;
}
