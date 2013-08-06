package entity.market;


/**
 * Data structure for holding best bid/ask quote (for updating NBBO)
 * 
 * @author ewah
 */
public class BestBidAsk {

	protected final Market bestBidMarket, bestAskMarket;
	protected final Price bestBid, bestAsk;

	public BestBidAsk(Market bestBidMarket, Price bestBid,
			Market bestAskMarket, Price bestAsk) {
		this.bestBidMarket = bestBidMarket;
		this.bestBid = bestBid;
		this.bestAskMarket = bestAskMarket;
		this.bestAsk = bestAsk;
	}

	/**
	 * @return bid-ask spread of the quote (integer)
	 */
	public int getSpread() {
		if (bestAsk.compareTo(bestBid) >= 0) {
			if (bestAsk.getPrice() == -1 || bestAsk.equals(Price.INF)) { // ask
																			// undefined
				return -Price.INF.getPrice();
			}
			if (bestBid.getPrice() == -1 || bestBid.getPrice() == 0) { // bid
																		// undefined
				return Price.INF.getPrice();
			}
			return bestAsk.getPrice() - bestBid.getPrice();
		}
		// if bid crosses the ask, return a spread of INF
		return Price.INF.getPrice();
	}

	public Market getBestBidMarket() {
		return bestBidMarket;
	}

	public Market getBestAskMarket() {
		return bestAskMarket;
	}

	public Price getBestBid() {
		return bestBid;
	}

	public Price getBestAsk() {
		return bestAsk;
	}

	@Override
	public int hashCode() {
		return bestBidMarket.hashCode() ^ bestBid.hashCode()
				^ bestAskMarket.hashCode() ^ bestAsk.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof BestBidAsk))
			return false;
		BestBidAsk that = (BestBidAsk) obj;
		return (bestBidMarket == that.bestBidMarket)
				&& (bestBid == that.bestBid)
				&& (bestAskMarket == that.bestAskMarket)
				&& (bestAsk == that.bestAsk);
	}

	public String toString() {
		return "(Bid: " + bestBid + ", Ask: " + bestAsk + ")";
	}

}