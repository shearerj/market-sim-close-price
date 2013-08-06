package entity.market;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import entity.Agent;
import event.TimeStamp;

/**
 * Base class for Transactions. Contains information on buyer/seller, market, quantity, price, and
 * time.
 * 
 * @author ewah
 */
public class Transaction {

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
		this.buyer = buyer;
		this.seller = seller;
		this.market = market;
		this.buyBid = buyBid;
		this.sellBid = sellBid;
		this.quantity = quantity;
		this.price = price;
		this.execTime = execTime;
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

	public int getQuantity() {
		return quantity;
	}

	public Price getPrice() {
		return price;
	}

	public TimeStamp getExecTime() {
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
		return "(mkt=" + market + ", buyer="
				+ buyer + ", seller=" + seller + ", price=" + price
				+ ", quantity=" + quantity + ", timeStamp=" + execTime + ")";
	}

}
