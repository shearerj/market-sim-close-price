package market;

import entity.Market;
import systemmanager.Consts;

/**
 * Data structure for holding best bid/ask quote (for updating NBBO)
 * 
 * @author ewah
 */
public class BestBidAsk {

	public final Market bestBidMarket, bestAskMarket;
	public final Price bestBid, bestAsk;
	
	public BestBidAsk(Market bestBidMarket, Price bestBid, Market bestAskMarket, Price bestAsk) {
		this.bestBidMarket = bestBidMarket;
		this.bestBid       = bestBid;
		this.bestAskMarket = bestAskMarket;
		this.bestAsk       = bestAsk;
	}
	
	// TODO make prices null?
	public BestBidAsk() {
		// initialize to -1
		this(null, null, null, null);
	}

	/**
	 * @return bid-ask spread of the quote (integer)
	 */
	public int getSpread() {
		if (bestAsk.compareTo(bestBid) >= 0) {
			if (bestAsk.getPrice() == -1 || bestAsk.equals(Consts.INF_PRICE)) {	// ask undefined
				return -Consts.INF_PRICE;
			}
			if (bestBid.getPrice() == -1 || bestBid.getPrice() == 0) {	// bid undefined
				return Consts.INF_PRICE;
			}
			return bestAsk.getPrice() - bestBid.getPrice();
		}
		// if bid crosses the ask, return a spread of INF
		return Consts.INF_PRICE;
	}
	
	public String toString() {
		return "(Bid: " + bestBid + ", Ask: " + bestAsk + ")";
	}

	@Override
	public boolean equals(Object that) {
		if (that instanceof BestBidAsk)
			return equals((BestBidAsk) that);
		else
			return false;
	}

	public boolean equals(BestBidAsk that) {
		return (bestBidMarket == that.bestBidMarket) && (bestBid == that.bestBid) &&
				(bestAskMarket == that.bestAskMarket) && (bestAsk == that.bestAsk);
	}
}