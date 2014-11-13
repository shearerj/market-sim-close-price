package entity.market;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * Container for Quote data.
 * 
 * @author ewah
 */
public class Quote implements Serializable {

	private static final Quote empty = new Quote(Optional.<Price> absent(), 0, Optional.<Price> absent(), 0, MarketTime.ZERO);
	
	private final Optional<Price> ask, bid;
	private final int askQuantity, bidQuantity;
	private final MarketTime quoteTime;

	private Quote(Optional<Price> bid, int bidQuantity, Optional<Price> ask, int askQuantity, MarketTime currentTime) {
		checkArgument(!ask.isPresent() || !bid.isPresent() || ask.get().greaterThanEqual(bid.get()), "Invalid quote bid > ask");
		this.ask = checkNotNull(ask);
		this.askQuantity = askQuantity;
		this.bid = checkNotNull(bid);
		this.bidQuantity = bidQuantity;
		this.quoteTime = checkNotNull(currentTime);
	}
	
	public static Quote create(Optional<Price> bid, int bidQuantity, Optional<Price> ask, int askQuantity, MarketTime currentTime) {
		return new Quote(bid, bidQuantity, ask, askQuantity, currentTime);
	}
	
	public static Quote empty() {
		return empty;
	}

	public Optional<Price> getAskPrice() {
		return ask;
	}

	public Optional<Price> getBidPrice() {
		return bid;
	}
	
	public int getBidQuantity() {
		return bidQuantity;
	}
	
	public int getAskQuantity() {
		return askQuantity;
	}
	
	public MarketTime getQuoteTime() {
		return quoteTime;
	}
	
	/**
	 * @return true if the quote is defined (has an ask and a bid price)
	 */
	public boolean isDefined() {
		return ask.isPresent() && bid.isPresent();
	}

	/**
	 * @return bid-ask spread of the quote
	 */
	public double getSpread() {
		// XXX Are these the best way to handle these cases?
		if (!ask.isPresent() || !bid.isPresent())
			return Double.POSITIVE_INFINITY;
		return ask.get().doubleValue() - bid.get().doubleValue();
	}
	
	public double getMidquote() {
		// XXX Are these the best way to handle these cases?
		if (!ask.isPresent() || !bid.isPresent())
			return Double.NaN;
		return (ask.get().doubleValue() + bid.get().doubleValue())/ 2;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(ask, askQuantity, bid, bidQuantity, quoteTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Quote))
			return false;
		Quote that = (Quote) obj;
		return Objects.equal(ask, that.ask)
				&& Objects.equal(bid, that.bid)
				&& Objects.equal(quoteTime, that.quoteTime)
				&& askQuantity == that.askQuantity
				&& bidQuantity == that.bidQuantity;
	}

	@Override
	public String toString() {
		return "(Bid: " + (bid.isPresent() ? bidQuantity + " @ " + bid.get() : "- ") +
				", Ask: " + (ask.isPresent() ? askQuantity + " @ " + ask.get() : "- ") + ')';
	}

	private static final long serialVersionUID = 3842989596948215994L;

}
