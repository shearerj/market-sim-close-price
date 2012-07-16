package market;

/**
 * Data structure for holding best bid/ask quote (for updating NBBO)
 * 
 * @author ewah
 */
public class BestBidAsk {

	// initialize to -1
	public int bestBidMarket = -1;
	public int bestBid = -1;
	public int bestAskMarket = -1;
	public int bestAsk = -1;

	public String toString() {
		return "(Bid: "+bestBid+", Ask: "+bestAsk+")";
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