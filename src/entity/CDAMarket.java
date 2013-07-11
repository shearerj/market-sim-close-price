package entity;

import static logger.Logger.log;
import static logger.Logger.Level.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import market.Bid;
import market.PQBid;
import market.Price;
import market.Quote;
import model.MarketModel;
import systemmanager.Consts;
import activity.Activity;
import activity.Clear;
import event.TimeStamp;

/**
 * Class for a continuous double auction market.
 * 
 * @author ewah
 */
public class CDAMarket extends Market {

	public CDAMarket(int marketID, MarketModel model, int ipID) {
		super(marketID, model, ipID);
	}
	
	
	/**   ----don't think we need this
	 * Overloaded constructor.
	 * @param marketID
	 */
	//public CDAMarket(int marketID, SystemData d, ObjectProperties p, MarketModel model, int ipID) {
		//super(marketID, d, p, model, ipID);
		//marketType = Consts.getMarketType(this.getName());
	//}

	public Bid getBidQuote() {
		return orderbook.getBidQuote();
	}
	
	public Bid getAskQuote() {
		return orderbook.getAskQuote();
	}
	
	public Price getBidPrice() {	
		return ((PQBid) getBidQuote()).bidTreeSet.first().getPrice();
	}
	
	public Price getAskPrice() {
		return ((PQBid) getAskQuote()).bidTreeSet.last().getPrice();
	}
	
	public Collection<? extends Activity> addBid(Bid b, TimeStamp ts) {
		orderbook.insertBid((PQBid) b);
		bids.add(b);
		this.addDepth(ts, orderbook.getDepth());
		return Collections.singleton(new Clear(this, Consts.INF_TIME));
	}
	
	
	public Collection<? extends Activity> removeBid(Agent agent, TimeStamp ts) {
		orderbook.removeBid(agent.getID());
		this.addDepth(ts, orderbook.getDepth());
		return Collections.singleton(new Clear(this, Consts.INF_TIME));
	}
	
	
	public Map<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}
	
	
	public Quote quote(TimeStamp quoteTime) {
		Quote q = new Quote(this);
		Price bp = q.lastBidPrice;
		Price ap = q.lastAskPrice;
		
		if (bp != null && ap != null) {
			if (bp.getPrice() == -1 || ap.getPrice() == -1) {
				// either bid or ask are undefined
				this.addSpread(quoteTime, Price.INF.getPrice());
				this.addMidQuote(quoteTime, Price.INF, Price.INF);
				
			} else if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
				log(ERROR, this.getName() + "::quote: ERROR bid > ask");
				this.addSpread(quoteTime, Price.INF.getPrice());
				this.addMidQuote(quoteTime, Price.INF, Price.INF);
				
			} else {
				// valid bid-ask
				this.addSpread(quoteTime, q.getSpread());
				this.addMidQuote(quoteTime, bp, ap);
			}
		}
		lastQuoteTime = quoteTime;
		
		if (bp != null) {
			lastBidPrice = bp;
			lastBidQuantity = q.lastBidQuantity;
		}
	    if (ap != null) {
	    	lastAskPrice = ap;
	    	lastAskQuantity = q.lastAskQuantity;
	    }
	    
	    return q;
	}

}

