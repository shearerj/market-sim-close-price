package edu.umich.srg.marketsim.market;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import edu.umich.srg.marketsim.Price;

/**
 * Container for Quote data.
 * 
 * @author ewah
 */
public class Quote implements Serializable {

	private static final Quote empty = new Quote(null, null);
	
	private final Optional<Price> ask, bid;

	Quote(Price bid, Price ask) {
		this.ask = Optional.ofNullable(ask);
		this.bid = Optional.ofNullable(bid);
	}
	
	static Quote empty() {
		return empty;
	}

	public Optional<Price> getAskPrice() {
		return ask;
	}

	public Optional<Price> getBidPrice() {
		return bid;
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
		// FIXME Are these the best way to handle these cases?
		if (!ask.isPresent() || !bid.isPresent())
			return Double.POSITIVE_INFINITY;
		return ask.get().doubleValue() - bid.get().doubleValue();
	}
	
	public double getMidquote() {
		// FIXME Are these the best way to handle these cases?
		if (!ask.isPresent() || !bid.isPresent())
			return Double.NaN;
		return (ask.get().doubleValue() + bid.get().doubleValue())/ 2;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ask, bid);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Quote))
			return false;
		Quote that = (Quote) obj;
		return Objects.equals(ask, that.ask)
				&& Objects.equals(bid, that.bid);
	}

	@Override
	public String toString() {
		return "(Bid: " + (bid.isPresent() ? bid.get() : "- ") +
				", Ask: " + (ask.isPresent() ? ask.get() : "- ") + ')';
	}

	private static final long serialVersionUID = 3842989596948215994L;

}