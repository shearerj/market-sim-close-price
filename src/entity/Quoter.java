package entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import event.*;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;


/**
 * Class that updates pertinent information for the system. 
 * Generally used for creating NBBO update events. Always has ID = 0.
 * 
 * @author ewah
 */
public class Quoter extends Entity {

	private int tickSize;
	private TimeStamp latency;
//	public BestBidAsk lastQuote;
	public HashMap<Integer, BestBidAsk> lastQuotes;		// hashed by model ID
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public Quoter(int ID, SystemData d, Log l) {
		super(ID, d, new ObjectProperties(), l);
		latency = d.nbboLatency;
		tickSize = d.tickSize;
		lastQuotes = new HashMap<Integer,BestBidAsk>();
	}
	
	/**
	 * Get BestBidAsk quote for the given model.
	 * 
	 * @param modelID
	 * @return
	 */
	public BestBidAsk getNBBOQuote(int modelID) {
		if (!lastQuotes.containsKey(modelID)) {
			BestBidAsk b = new BestBidAsk();
			lastQuotes.put(modelID, b);
			return b;
		} else {
			return lastQuotes.get(modelID);
		}
	}
	
	/**
	 * Method to update the NBBO values.
	 * 
	 * @param ts
	 * @return
	 */
	public ActivityHashMap updateNBBO(TimeStamp ts) {
		
		ActivityHashMap actMap = new ActivityHashMap();
		
		for (Map.Entry<Integer,MarketModel> entry : data.getModels().entrySet()) {
			int modelID = entry.getKey();
			MarketModel model = entry.getValue();
			ArrayList<Integer> ids = model.getMarketIDs();
		
			BestBidAsk lastQuote = findBestBidOffer(ids);
			String s = ts + " | " + ids + " UpdateNBBO" + lastQuote;
			
			int bestBid = lastQuote.bestBid;
			int bestAsk = lastQuote.bestAsk;
			if ((bestBid != -1) && (bestAsk != -1)) {
				// check for inconsistency in buy/sell prices & fix if found
				if (lastQuote.bestBid > lastQuote.bestAsk) {
					int mid = (lastQuote.bestBid + lastQuote.bestAsk) / 2;
					bestBid = mid - this.tickSize;
					bestAsk = mid + this.tickSize;
					
					// Add spread of INF if inconsistent NBBO quote
					this.data.addNBBOSpread(modelID, ts, Consts.INF_PRICE);
				} else {
					// if bid-ask consistent, store the spread
					this.data.addNBBOSpread(modelID, ts, lastQuote.getSpread());
				}
			} else {
				// store spread of INF since no bid-ask spread
				this.data.addSpread(0, ts, Consts.INF_PRICE);
			}
			
			lastQuote.bestBid = bestBid;
			lastQuote.bestAsk = bestAsk;
			log.log(Log.INFO, s + " --> NBBO" + lastQuote);
			
			lastQuotes.put(modelID, lastQuote);
		}
		
		if (latency.getTimeStamp() > 0) {
			TimeStamp tsNew = ts.sum(latency);
			actMap.insertActivity(Consts.UPDATE_NBBO_PRIORITY,	new UpdateNBBO(this, tsNew));
			
		} else if (latency.getTimeStamp() == 0) {
			// infinitely fast NBBO updates
			TimeStamp tsNew = new TimeStamp(Consts.INF_TIME);
			actMap.insertActivity(Consts.UPDATE_NBBO_PRIORITY,	new UpdateNBBO(this, tsNew));
		}
		return actMap;
	}
	
	
	/**
	 * Find best quote across given markets (lowest ask & highest bid).
	 * 
	 * @param marketIDs
	 * @return
	 */
	protected BestBidAsk findBestBidOffer(ArrayList<Integer> marketIDs) {
		
	    int bestBid = -1;
	    int bestBidMkt = 0;
	    int bestAsk = -1;
	    int bestAskMkt = 0;
	    
	    for (Iterator<Integer> i = marketIDs.iterator(); i.hasNext(); ) {
			Market mkt = data.markets.get(i.next());
			
			Price bid = mkt.getBidPrice();
			Price ask = mkt.getAskPrice();

			// in case the bid/ask disappears
			Vector<Price> price = new Vector<Price>();
			price.add(bid);
			price.add(ask);

			// Best bid quote is highest BID
			if (bestBid == -1 || bestBid < bid.getPrice()) {
				if (bid.getPrice() != -1) bestBid = bid.getPrice();
				bestBidMkt = mkt.ID;
			}
			// Best ask quote is lowest ASK
			if (bestAsk == -1 || bestAsk > ask.getPrice()) {
				if (ask.getPrice() != -1) bestAsk = ask.getPrice();
				bestAskMkt = mkt.ID;
			}
	    }
	    BestBidAsk q = new BestBidAsk();
	    q.bestBidMarket = bestBidMkt;
	    q.bestBid = bestBid;
	    q.bestAskMarket = bestAskMkt;
	    q.bestAsk = bestAsk;
	    return q;
	}
}
