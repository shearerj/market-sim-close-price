package activity.market;

import event.TimeStamp;

/**
 * Base class for Transactions.
 * 
 * @author ewah
 */
public class Transaction {

	public Long quantity;
	public Price price;
	public TimeStamp timestamp;
	public Integer buyerID;
	public Integer sellerID;
	public Integer marketID;
	public String type;			// TODO what is this???
	public Integer transID;

}
