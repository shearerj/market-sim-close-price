package activity.market;

import event.TimeStamp;

import java.util.HashMap;

/**
 * Container for Quote data.
 * 
 * @author ewah
 */
public class Quote {

	public Integer hqw;	// hypothetical quantity won
	public Integer as;	// auction status? TODO???
	public Price lastAskPrice;
	public Price lastBidPrice;
	public Price lastClearPrice;
	public TimeStamp lastQuoteTime;
	public TimeStamp nextQuoteTime;
	public TimeStamp finalClearTime;
	public TimeStamp nextClearTime;
	public TimeStamp lastClearTime;
	public Integer marketID;
	public HashMap<Integer, QuotePoint> points;

	
	public Quote(int mktID, Price lastAsk, Price lastBid, Price lastClear, TimeStamp lastQuoteT, TimeStamp lastClearT) {
		marketID = mktID;
		lastAskPrice = lastAsk;
		lastBidPrice = lastBid;
		lastClearPrice = lastClear;
		lastQuoteTime = lastQuoteT;
		lastClearTime = lastClearT;
	}
	
	public int getQuantityAtBidPrice (double bidPrice) {
		int totalQuantityAtPrice = 0;
		for (int quantity : points.keySet()) {
			if (points.get(quantity).bidprice.price == bidPrice) {
				totalQuantityAtPrice += quantity;
			}
		}
		return totalQuantityAtPrice;
	}

	public int getQuantityAtAskPrice (double askPrice) {
		int totalQuantityAtPrice = 0;
		for (int quantity : points.keySet()) {
			if (points.get(quantity).askprice.price == askPrice) {
				totalQuantityAtPrice += quantity;
			}
		}
		return totalQuantityAtPrice;
	}

	
	public HashMap<Integer,QuotePoint> getPoints () {
		return points;
	}
}


/**
 * Data structure for storing point for quote.
 * @author ewah
 */
class QuotePoint {
	
	public Price bidprice, askprice;
	
	QuotePoint() {
		bidprice = askprice = new Price(-1);
	}
	
	QuotePoint(Price bp, Price ap) {
		bidprice = bp;
		askprice = ap;
	}

	@Override
	public boolean equals(Object that) {
		return (that instanceof QuotePoint) && this.equals(that);
	}

	public boolean equals(QuotePoint that) {
		return (bidprice.price == that.bidprice.price) 
				&& (askprice.price == that.askprice.price);
	}

	public int hashCode () {
		return (int )(bidprice.price + (askprice.price*askprice.price));
	}
}