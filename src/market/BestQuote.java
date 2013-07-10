package market;

import entity.Market;

/**
 * BestQuote is a container to store the best quote across all markets.
 * 
 * User: Shih-Fen Cheng Date: Oct 27, 2010 Time: 6:58:57 AM
 */
// FIXME Different from BestBidAsk?
public class BestQuote {
	protected final Market bestBuyMarket, bestSellMarket;
	protected final Price bestBuy, bestSell;

	public BestQuote(Market bestBuyMarket, Price bestBuy,
			Market bestSellMarket, Price bestSell) {
		this.bestBuyMarket = bestBuyMarket;
		this.bestBuy = bestBuy;
		this.bestSellMarket = bestSellMarket;
		this.bestSell = bestSell;
	}

	public Market getBestBuyMarket() {
		return bestBuyMarket;
	}

	public Market getBestSellMarket() {
		return bestSellMarket;
	}

	public Price getBestBuy() {
		return bestBuy;
	}

	public Price getBestSell() {
		return bestSell;
	}

	@Override
	public int hashCode() {
		return bestBuyMarket.hashCode() ^ bestBuy.hashCode()
				^ bestSellMarket.hashCode() ^ bestSell.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof BestQuote))
			return false;
		BestQuote that = (BestQuote) obj;
		return (bestBuyMarket == that.bestBuyMarket)
				&& (bestBuy == that.bestBuy)
				&& (bestSellMarket == that.bestSellMarket)
				&& (bestSell == that.bestSell);
	}

	public String toString() {
		return "BestQuote(BestBuy: " + bestBuy + ", BestSell: " + bestSell
				+ ")";
	}
}
