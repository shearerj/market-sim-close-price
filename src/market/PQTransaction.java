package market;

import entity.Agent;
import entity.Market;
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

	/**
	 * Generate PQTransaction with given price, quantity, buyerID, sellerID,
	 * buyBidID, sellBidID, TimeStamp, marketID.
	 * 
	 * @param q
	 *            quantity
	 * @param p
	 *            price
	 * @param buyer
	 * @param seller
	 * @param buyBid
	 * @param sellBid
	 * @param ts
	 *            TimeStamp
	 * @param mktID
	 *            marketID
	 */
	public PQTransaction(int q, Price p, Agent buyer, Agent seller, Bid buyBid,
			Bid sellBid, TimeStamp ts, Market market) {
		quantity = new Integer(q);
		price = p;
		timestamp = ts;

		this.buyer = buyer;
		this.seller = seller;
		this.buyBid = buyBid;
		this.sellBid = sellBid;
		this.market = market;
	}

	/**
	 * NOTE: does not compare transaction IDs
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PQTransaction other = (PQTransaction) obj;
		return new EqualsBuilder().append(price.price, other.price.price).append(
				quantity.intValue(), other.quantity.intValue()).append(
				buyer.getID(), other.buyer.getID()).append(seller.getID(),
				other.seller.getID()).append(buyBid.getBidID(),
				other.buyBid.getBidID()).append(sellBid.getBidID(),
				other.sellBid.getBidID()).append(market.getID(),
				other.market.getID()).append(timestamp.longValue(),
				other.timestamp.longValue()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31).append(price.price).append(
				quantity.intValue()).append(buyer.getID()).append(
				seller.getID()).append(buyBid.getBidID()).append(
				sellBid.getBidID()).append(market.getID()).append(
				timestamp.longValue()).toHashCode();
	}

	@Override
	public String toString() {
		String result = "Transaction(ID=" + transID.toString() + ", quantity="
				+ quantity.toString() + ", price=" + price + ", buyerID="
				+ buyer.getID() + ", sellerID=" + seller.getID()
				+ ", buyBidID=" + buyBid.getBidID() + ", sellBidID="
				+ sellBid.getBidID() + ", timestamp=" + timestamp.toString()
				+ ", marketID=" + market.getID() + ")";
		return result;
	}

	/**
	 * print the Transaction to stdout
	 */
	public void print() {
		System.out.println(quantity + " " + price + " " + buyer.getID() + " "
				+ seller.getID() + " " + buyBid.getBidID() + " "
				+ sellBid.getBidID() + " " + timestamp.toString() + " "
				+ market.getID());
	}

}
