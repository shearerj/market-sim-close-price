package entity.sip;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

import entity.market.Market;
import entity.market.Price;


/**
 * Data structure for holding best bid/ask quote (for updating NBBO)
 * 
 * @author ewah
 */
public class BestBidAsk implements Serializable {
	
	private static final BestBidAsk empty = new BestBidAsk(
			Optional.<Market> absent(), Optional.<Price> absent(), 0,
			Optional.<Market> absent(), Optional.<Price> absent(), 0);
	
	protected final Optional<Market> bestBidMarket, bestAskMarket;
	protected final Optional<Price> bestBid, bestAsk;
	protected final int bestBidQuantity, bestAskQuantity;

	protected BestBidAsk(Optional<Market> bestBidMarket, Optional<Price> bestBid, int bestBidQuantity,
			Optional<Market> bestAskMarket, Optional<Price> bestAsk, int bestAskQuantity) {
		this.bestBidMarket = checkNotNull(bestBidMarket);
		this.bestBid = checkNotNull(bestBid);
		this.bestBidQuantity = bestBidQuantity;
		this.bestAskMarket = checkNotNull(bestAskMarket);
		this.bestAsk = checkNotNull(bestAsk);
		this.bestAskQuantity = bestAskQuantity;
	}
	
	public static BestBidAsk create(Optional<Market> bestBidMarket, Optional<Price> bestBid, int bestBidQuantity,
			Optional<Market> bestAskMarket, Optional<Price> bestAsk, int bestAskQuantity) {
		return new BestBidAsk(bestBidMarket, bestBid, bestBidQuantity, bestAskMarket, bestAsk, bestAskQuantity);
	}
	
	public static BestBidAsk empty() {
		return empty;
	}

	/**
	 * @return bid-ask spread of the quote (double)
	 */
	public double getSpread() {
		if (bestAsk.isPresent() && bestBid.isPresent() && bestAsk.get().greaterThanEqual(bestBid.get())) {
			return bestAsk.get().doubleValue() - bestBid.get().doubleValue();
		}

		return Double.POSITIVE_INFINITY;
	}

	public Optional<Market> getBestBidMarket() {
		return bestBidMarket;
	}

	public Optional<Market> getBestAskMarket() {
		return bestAskMarket;
	}

	public Optional<Price> getBestBid() {
		return bestBid;
	}

	public Optional<Price> getBestAsk() {
		return bestAsk;
	}
	
	public int getBestBidQuantity() {
		return bestBidQuantity;
	}

	public int getBestAskQuantity() {
		return bestAskQuantity;
	}
	

	@Override
	public int hashCode() {
		return Objects.hashCode(bestBidMarket, bestBid, bestBidQuantity, 
				bestAskMarket, bestAsk, bestAskQuantity);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof BestBidAsk))
			return false;
		BestBidAsk that = (BestBidAsk) obj;
		return Objects.equal(bestBidMarket, that.bestBidMarket)
				&& Objects.equal(bestBid, that.bestBid)
				&& Objects.equal(bestBidQuantity, that.bestBidQuantity)
				&& Objects.equal(bestAskMarket, that.bestAskMarket)
				&& Objects.equal(bestAsk, that.bestAsk)
				&& Objects.equal(bestAskQuantity, that.bestAskQuantity);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("(BestBid: ");
		
		if (bestBid.isPresent()) { 
			sb.append(bestBidQuantity).append(" @ ").append(bestBid.get());
			sb.append(" from ").append(bestBidMarket.get());
		} else {
			sb.append("- ");
		}
		
		sb.append(", BestAsk: ");
		
		if (bestAsk.isPresent()) {
			sb.append(bestAskQuantity).append(" @ ").append(bestAsk.get());
			sb.append(" from ").append(bestAskMarket.get());
		} else {
			sb.append("- ");
		}
		
		return sb.append(')').toString();
	}

	private static final long serialVersionUID = -7312167969610706296L;

}