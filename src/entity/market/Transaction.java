package entity.market;

import java.io.Serializable;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import entity.agent.Agent;
import event.TimeStamp;

/**
 * Base class for Transactions. Contains information on buyer/seller, market, quantity, price, and
 * time.
 * 
 * @author ewah
 */
public class Transaction implements Serializable {

	private static final long serialVersionUID = 8420827805792281642L;
	
	protected final Agent buyer;
	protected final Agent seller;
	protected final Market market;
	protected final Order buyOrder;
	protected final Order sellOrder;

	// Transaction Info
	protected final int quantity;
	protected final Price price;
	protected final TimeStamp execTime;

	public Transaction(Agent buyer, Agent seller, Market market, Order buyOrder,
			Order sellOrder, int quantity, Price price, TimeStamp execTime) {
		this.buyer = buyer;
		this.seller = seller;
		this.market = market;
		this.buyOrder = buyOrder;
		this.sellOrder = sellOrder;
		this.quantity = quantity;
		this.price = price;
		this.execTime = execTime;
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

	public final Order getBuyBid() {
		return buyOrder;
	}

	public final Order getSellBid() {
		return sellOrder;
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
		return price.equals(other.price) && quantity == other.quantity
				&& buyOrder.equals(other.buyOrder) && sellOrder.equals(other.sellOrder)
				&& buyer.equals(other.buyer) && seller.equals(other.seller)
				&& market.equals(other.market) && execTime.equals(other.execTime);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(price).append(quantity).append(
				buyer).append(seller).append(buyOrder).append(sellOrder).append(
				market).append(execTime).toHashCode();
	}

	@Override
	public String toString() {
		return "(mkt=" + market + ", buyer="
				+ buyer + ", seller=" + seller + ", price=" + price
				+ ", quantity=" + quantity + ", timeStamp=" + execTime + ")";
	}

}
