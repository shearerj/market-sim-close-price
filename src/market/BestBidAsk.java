package market;

import systemmanager.Consts;

/**
 * Data structure for holding best bid/ask quote (for updating NBBO)
 * 
 * @author ewah
 */
public class BestBidAsk {

	// initialize to -1
	public int bestBidMarket = 1;
	public int bestBid = -1;
	public int bestAskMarket = 1;
	public int bestAsk = -1;

	/**
	 * @return bid-ask spread of the quote (integer)
	 */
	public int getSpread() {
		if (bestAsk >= bestBid) {
			if (bestAsk == -1 || bestAsk == Consts.INF_PRICE) {	// ask undefined
				return -Consts.INF_PRICE;
			}
			if (bestBid == -1 || bestBid == 0) {	// bid undefined
				return Consts.INF_PRICE;
			}
			return bestAsk - bestBid;
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