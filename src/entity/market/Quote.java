package entity.market;

import static logger.Logger.log;
import static logger.Logger.Level.ERROR;

import event.TimeStamp;

/**
 * Container for Quote data.
 * 
 * @author ewah
 */
public class Quote {

	protected final Price ask, bid;
	protected final int quantityAsk, quantityBid;
	protected final Market market;
	protected final TimeStamp quoteTime;

	public Quote(Market market, Price ask, int quantityAsk, Price bid, int quantityBid, TimeStamp currentTime) {
		this.market = market;
		this.ask = ask;
		this.quantityAsk = quantityAsk;
		this.bid = bid;
		this.quantityBid = quantityBid;
		this.quoteTime = currentTime;
	}

	public Price getAskPrice() {
		return ask;
	}

	public Price getBidPrice() {
		return bid;
	}

	/**
	 * XXX Should also return if ask < bid?
	 * 
	 * @return true if the quote is defined (has an ask and a bid price)
	 */
	public boolean isDefined() {
		return ask != null && bid != null;
	}

	/**
	 * @return bid-ask spread of the quote
	 */
	public double getSpread() {
		// XXX Are these the best way to handle these cases?
		if (ask == null || bid == null) {
			return Double.POSITIVE_INFINITY;
		} else if (ask.lessThan(bid)) {
			log(ERROR, market.getClass().getSimpleName()
					+ "::quote: ERROR bid > ask");
			return 0;
		} else {
			return ask.getInTicks() - bid.getInTicks();
		}
	}
	
	public double getMidquote() {
		// XXX Are these the best way to handle these cases?
		if (ask == null || bid == null)
			return Double.NaN;
		else if (ask.lessThan(bid))
			log(ERROR, market.getClass().getSimpleName()
					+ "::quote: ERROR bid > ask");
		return (ask.getInTicks() + bid.getInTicks())/ 2d;
	}

	public String toString() {
		return "(Bid: " + bid + " @" + quantityBid + ", Ask: " + ask + " @"
				+ quantityAsk + ")";
	}

}
