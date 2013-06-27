package market;

import data.Sequence;
import entity.Agent;
import entity.Market;
import event.TimeStamp;

/**
 * Base class for Transactions. Contains information on buyer/seller, market,
 * quantity, price, and time.
 * 
 * @author ewah
 */
public class Transaction {
	
	public Integer transID;
	protected Agent buyer;
	protected Agent seller;
	protected Market market;
	protected Bid buyBid;
	protected Bid sellBid;
	
	//Transaction Info
	public Integer quantity;
	public Price price;
	public TimeStamp timestamp;

	public Transaction() {
	}

	public Integer getTransID() {
		return transID;
	}

	public Agent getBuyer() {
		return buyer;
	}

	public Agent getSeller() {
		return seller;
	}

	public Market getMarket() {
		return market;
	}

	public Bid getBuyBid() {
		return buyBid;
	}

	public Bid getSellBid() {
		return sellBid;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public Price getPrice() {
		return price;
	}

	public TimeStamp getTimestamp() {
		return timestamp;
	}
}
