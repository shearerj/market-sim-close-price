package entity;

import systemmanager.SystemData;
import market.*;

public class CallMarket extends Market {

	public PQOrderBook orderbook;
	
	/**
	 * Overloaded constructor.
	 * @param marketID
	 */
	public CallMarket(int marketID, SystemData d) {
		super(marketID, d);
		orderbook = new PQOrderBook(this.ID);
		marketType = "CALL";
	}
	
	public Bid getBidQuote() {
		return this.orderbook.getBidQuote();
	}
	
	public Bid getAskQuote() {
		return this.orderbook.getAskQuote();
	}
	
	
	public void addBid(Bid b) {
		orderbook.insertBid((PQBid) b);
		this.data.addBid(b.getBidID(), (PQBid) b);
	}
	
	
	public void removeBid(Bid b) {
		orderbook.removeBid(b.getAgentID());
		// replace with empty bid
		PQBid emptyBid = new PQBid(b.getAgentID(), this.ID);
		emptyBid.addPoint(0, new Price(0));	
		this.data.bidData.put(b.getBidID(), emptyBid);
	}
}
