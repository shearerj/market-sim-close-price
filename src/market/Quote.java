package market;

import static systemmanager.Consts.INF_PRICE;
import entity.Market;
import event.TimeStamp;

/**
 * Container for Quote data.
 * 
 * @author ewah
 */
public class Quote {

	public Price lastAskPrice;
	public Price lastBidPrice;
	public Price lastClearPrice;
	public TimeStamp lastQuoteTime;
	public TimeStamp nextQuoteTime;		// for now, unused
	public TimeStamp nextClearTime;
	public TimeStamp lastClearTime;
	public Integer marketID;
	public Integer lastAskQuantity;
	public Integer lastBidQuantity;
	
	public int depth;
	
	public Quote() {
		lastAskPrice = new Price(-1);
		lastBidPrice = new Price(-1);
		lastAskQuantity = 0;
		lastBidQuantity = 0;
	}
	
	/**
	 * Constructor
	 * 
	 * @param mkt
	 */
	public Quote(Market mkt) {
		marketID = mkt.getID();
		
		PQBid ask = (PQBid) mkt.getAskQuote();
		PQBid bid = (PQBid) mkt.getBidQuote();
		lastAskPrice = ask.bidTreeSet.last().price;
		lastBidPrice = bid.bidTreeSet.first().price;
		lastAskQuantity = ask.bidTreeSet.last().quantity;
		lastBidQuantity = bid.bidTreeSet.first().quantity;
		
		lastClearPrice = mkt.getLastClearPrice();
		nextClearTime = mkt.getNextClearTime();
		lastClearTime = mkt.getLastClearTime();
		nextQuoteTime = mkt.getNextQuoteTime();
		lastQuoteTime = mkt.getLastQuoteTime();
		
		depth = 1;
	}
	
	/**
	 * @return bid-ask spread of the quote (integer)
	 */
	public int getSpread() {
		if (lastAskPrice.compareTo(lastBidPrice) >= 0) {
			if (lastAskPrice.getPrice() == -1 || lastAskPrice.getPrice() == 0) {
				return INF_PRICE.getPrice();
			}
			if (lastBidPrice.getPrice() == -1 || 
					lastBidPrice.equals(INF_PRICE)) {
				return INF_PRICE.getPrice();
			}
			return lastAskPrice.getPrice() - lastBidPrice.getPrice();
		}
		return 0;
	}
	
	
	public String toString() {
		return "(Bid: " + lastBidPrice + ", Ask: " + lastAskPrice + ")";
	}
	
	
	
//	public int getQuantityAtBidPrice (double bidPrice) {
//		int totalQuantityAtPrice = 0;
//		for (int quantity : points.keySet()) {
//			if (points.get(quantity).bidprice.price == bidPrice) {
//				totalQuantityAtPrice += quantity;
//			}
//		}
//		return totalQuantityAtPrice;
//	}
//
//	public int getQuantityAtAskPrice (double askPrice) {
//		int totalQuantityAtPrice = 0;
//		for (int quantity : points.keySet()) {
//			if (points.get(quantity).askprice.price == askPrice) {
//				totalQuantityAtPrice += quantity;
//			}
//		}
//		return totalQuantityAtPrice;
//	}
	
//	public HashMap<Integer,QuotePoint> getPoints () {
//		return points;
//	}
	
	
}


///**
// * Data structure for storing point for quote.
// * @author ewah
// */
//class QuotePoint {
//	
//	public Price bidprice, askprice;
//	
//	QuotePoint() {
//		bidprice = askprice = new Price(-1);
//	}
//	
//	QuotePoint(Price bp, Price ap) {
//		bidprice = bp;
//		askprice = ap;
//	}
//
//	@Override
//	public boolean equals(Object that) {
//		return (that instanceof QuotePoint) && this.equals(that);
//	}
//
//	public boolean equals(QuotePoint that) {
//		return (bidprice.price == that.bidprice.price) 
//				&& (askprice.price == that.askprice.price);
//	}
//
////	public int hashCode () {
////		return (int) (bidprice.price + (askprice.price*askprice.price));
////	}
//}