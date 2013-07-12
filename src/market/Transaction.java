package market;

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

	protected final int transID;
	protected final Agent buyer;
	protected final Agent seller;
	protected final Market market;
	protected final Bid buyBid;
	protected final Bid sellBid;

	// Transaction Info
	protected final int quantity;
	protected final Price price;
	protected final TimeStamp execTime;

	public Transaction(int transID, Agent buyer, Agent seller, Market market,
			Bid buyBid, Bid sellBid, int quantity, Price price,
			TimeStamp execTime) {
		super();
		this.transID = transID;
		this.buyer = buyer;
		this.seller = seller;
		this.market = market;
		this.buyBid = buyBid;
		this.sellBid = sellBid;
		this.quantity = quantity;
		this.price = price;
		this.execTime = execTime;
	}

	public final int getTransID() {
		return transID;
	}

	public final Agent getBuyer() {
		return buyer;
	}

	public final Agent getSeller() {
		return seller;
	}

	public final Market getMarket() {
		return market;
	}

	public final Bid getBuyBid() {
		return buyBid;
	}

	public final Bid getSellBid() {
		return sellBid;
	}

	public final int getQuantity() {
		return quantity;
	}

	public final Price getPrice() {
		return price;
	}

	public final TimeStamp getExecTime() {
		return execTime;
	}

	@Override
	public int hashCode() {
		return transID ^ buyBid.hashCode() ^ sellBid.hashCode()
				^ buyer.hashCode() ^ seller.hashCode() ^ market.hashCode()
				^ price.hashCode() ^ execTime.hashCode() ^ quantity;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Transaction))
			return false;
		Transaction other = (Transaction) obj;
		return this.transID == other.transID
				&& this.buyBid.equals(other.buyBid)
				&& this.sellBid.equals(other.sellBid)
				&& this.buyer.equals(other.buyer)
				&& this.seller.equals(other.seller)
				&& this.market.equals(other.market)
				&& this.price.equals(other.price)
				&& this.execTime.equals(other.execTime)
				&& this.quantity == other.quantity;
	}

	@Override
	public String toString() {
		return "(" + "transID=" + transID + ", mkt=" + market + ", buyer="
				+ buyer + ", seller=" + seller + ", price=" + price
				+ ", quantity=" + quantity + ", timeStamp=" + execTime + ")";
	}

}
