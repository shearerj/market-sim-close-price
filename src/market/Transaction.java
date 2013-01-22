package market;

import event.TimeStamp;

/**
 * Base class for Transactions. Contains information on buyer/seller, market,
 * quantity, price, and time.
 * 
 * @author ewah
 */
public class Transaction {
	
	public Integer transID;
	public Integer buyerID;
	public Integer sellerID;
	public Integer marketID;
	public Integer buyBidID;
	public Integer sellBidID;
	
	public Integer quantity;
	public Price price;
	public TimeStamp timestamp;
}
