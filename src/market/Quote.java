package market;

import static logger.Logger.log;
import static logger.Logger.Level.ERROR;

import entity.Market;
import event.TimeStamp;

/**
 * Container for Quote data.
 * 
 * @author ewah
 */
public class Quote {

	protected final Price askPrice, bidPrice;
	protected final Market market;
	protected final int askQuantity, bidQuantity;
	protected final TimeStamp quoteTime;
	protected final int spread;

	public Quote(Market market, Price askPrice, int askQuantity,
			Price bidPrice, int bidQuantity, TimeStamp currentTime) {
		this.market = market;
		this.askPrice = askPrice;
		this.bidPrice = bidPrice;
		this.askQuantity = askQuantity;
		this.bidQuantity = bidQuantity;
		this.quoteTime = currentTime;

		// XXX Are these the best way to handle these cases?
		if (askPrice == null || bidPrice == null) {
			spread = Integer.MAX_VALUE;
		} else if (askPrice.lessThan(bidPrice)) {
			log(ERROR, market.getClass().getSimpleName()
					+ "::quote: ERROR bid > ask");
			spread = 0;
		} else {
			spread = askPrice.getPrice() - bidPrice.getPrice();
		}
	}

	public Price getAskPrice() {
		return askPrice;
	}

	public Price getBidPrice() {
		return bidPrice;
	}

	/**
	 * XXX Should also return if ask < bid?
	 * 
	 * @return true if the quote is defined (has an ask and a bid price)
	 */
	public boolean isDefined() {
		return askPrice != null && bidPrice != null;
	}

	/**
	 * @return bid-ask spread of the quote (integer)
	 */
	public int getSpread() {
		return spread;
	}

	public String toString() {
		return "(Bid: " + bidPrice + ", Ask: " + askPrice + ")";
	}

}
