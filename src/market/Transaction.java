package market;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import entity.Agent;
import entity.Market;
import event.TimeStamp;

/**
 * Base class for Transactions. Contains information on buyer/seller, market, quantity, price, and
 * time.
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

	public Transaction(Agent buyer, Agent seller, Market market, Bid buyBid,
			Bid sellBid, int quantity, Price price, TimeStamp execTime) {
		this.transID = hashCode();
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
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) return false;
		Transaction other = (Transaction) obj;
		return new EqualsBuilder().append(price, other.price).append(quantity,
				other.quantity).append(buyer, other.buyer).append(seller,
				other.seller).append(buyBid, other.buyBid).append(sellBid,
				other.sellBid).append(market, other.market).append(execTime,
				other.execTime).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(price).append(quantity).append(
				buyer).append(seller).append(buyBid).append(sellBid).append(
				market).append(execTime).toHashCode();
	}

	@Override
	public String toString() {
		return "(" + "transID=" + transID + ", mkt=" + market + ", buyer="
				+ buyer + ", seller=" + seller + ", price=" + price
				+ ", quantity=" + quantity + ", timeStamp=" + execTime + ")";
	}

}
