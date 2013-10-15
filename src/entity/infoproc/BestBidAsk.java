package entity.infoproc;

import java.io.Serializable;

import com.google.common.base.Objects;

import entity.market.Market;
import entity.market.Price;


/**
 * Data structure for holding best bid/ask quote (for updating NBBO)
 * 
 * @author ewah
 */
public class BestBidAsk implements Serializable {
	
	private static final long serialVersionUID = -7312167969610706296L;
	
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
	 * @return bid-ask spread of the quote (double)
	 */
	public double getSpread() {
		if (bestAsk == null || bestBid == null || bestAsk.lessThan(bestBid))
			return Double.POSITIVE_INFINITY;
		return bestAsk.doubleValue() - bestBid.doubleValue();
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
		return Objects.hashCode(bestBidMarket, bestBid, bestAskMarket, bestAsk);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj.getClass().equals(getClass())))
			return false;
		BestBidAsk that = (BestBidAsk) obj;
		return Objects.equal(bestBidMarket, that.bestBidMarket)
				&& Objects.equal(bestBid, that.bestBid)
				&& Objects.equal(bestAskMarket, that.bestAskMarket)
				&& Objects.equal(bestAsk, that.bestAsk);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("(BestBid: ");
		
		if (bestBid == null) sb.append("- ");
		else sb.append(bestBid).append(" from ").append(bestBidMarket);
		
		sb.append(", BestAsk: ");
		
		if (bestAsk == null) sb.append("- ");
		else sb.append(bestAsk).append(" from ").append(bestAskMarket);
		
		return sb.append(')').toString();
	}

}