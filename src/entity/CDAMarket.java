package entity;

import static logger.Logger.log;
import static logger.Logger.Level.ERROR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

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
	
	public CDAMarket(int marketID, MarketModel model) {
		super(marketID, model);
	}

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
		data.addDepth(id, ts, orderbook.getDepth());
		return Collections.singleton(new Clear(this, Consts.INF_TIME));
	}
	
	
	public Collection<Activity> removeBid(int agentID, TimeStamp ts) {
		orderbook.removeBid(agentID);
		data.addDepth(this.id, ts, orderbook.getDepth());
		// return clear(ts);
		Collection<Activity> actMap = new ArrayList<Activity>();
		actMap.add(new Clear(this, Consts.INF_TIME));
		return actMap;
	}
	
	
	public HashMap<Integer,Bid> getBids() {
		return orderbook.getActiveBids();
	}
	
	
	public Quote quote(TimeStamp quoteTime) {
		Quote q = new Quote(this);
		Price bp = q.lastBidPrice;
		Price ap = q.lastAskPrice;
		
		if (bp != null && ap != null) {
			if (bp.getPrice() == -1 || ap.getPrice() == -1) {
				// either bid or ask are undefined
				data.addSpread(id, quoteTime, Consts.INF_PRICE.getPrice());
				data.addMidQuotePrice(id, quoteTime, Consts.INF_PRICE.getPrice(), Consts.INF_PRICE.getPrice());
				
			} else if (bp.compareTo(ap) == 1 && ap.getPrice() > 0) {
				log(ERROR, this.getName() + "::quote: ERROR bid > ask");
				data.addSpread(id, quoteTime, Consts.INF_PRICE.getPrice());
				data.addMidQuotePrice(id, quoteTime, Consts.INF_PRICE.getPrice(), Consts.INF_PRICE.getPrice());
				
			} else {
				// valid bid-ask
				data.addQuote(id, q);
				data.addSpread(id, quoteTime, q.getSpread());
				data.addMidQuotePrice(id, quoteTime, bp.getPrice(), ap.getPrice());
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

