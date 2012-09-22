package market;

/**
 * BestQuote is a container to store the best quote across all markets.
 * 
 * User: Shih-Fen Cheng
 * Date: Oct 27, 2010
 * Time: 6:58:57 AM
 */
public class BestQuote {
	public int bestBuyMarket = 1;
	public int bestBuy = -1;
	public int bestSellMarket = 1;
	public int bestSell = -1;

	public String toString() {
		return "BestQuote(BestBuy: " + bestBuy + ", BestSell: " + bestSell + ")";
	}

	@Override
	public boolean equals(Object that) {
		if (that instanceof BestQuote)
			return equals((BestQuote) that);
		else
			return false;
	}

	public boolean equals(BestQuote that) {
		return (bestBuyMarket == that.bestBuyMarket) && (bestBuy == that.bestBuy) &&
				(bestSellMarket == that.bestSellMarket) && (bestSell == that.bestSell);
	}
}
