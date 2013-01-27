package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;
import java.util.ArrayList;

/**
 * MarketMakerAgent
 * 
 * Basic market maker. See description in (Chakraborty & Kearns, 2011).
 * Participates in only a single market at a time. Submits a ladder of bids
 * based on BID = Y_t < ASK = X_t, where C_t = numRungs * stepSize:
 * 
 * buy orders:  [Y_t - C_t, ..., Y_t - 1, Y_t]
 * sell orders: [X_t, X_t + 1, ..., X_t + C_t]
 * 
 * NOTE: The MarketMakerAgent will truncate the ladder when the price will cross
 * the NBBO, i.e., whenever one of the points in the bid would be routed to
 * the alternate market otherwise. This happens when:
 * 
 * buy orders:  If ASK_N < X_t, then [ASK_N, ..., Y_t] (ascending)
 * sell orders: If BID_N > Y_t, then [X_t, ..., BID_N] (ascending)
 * 
 * @author ewah, gshiva
 */
public class MarketMakerAgent extends SMAgent {     

	private int stepSize;
	private int rungSize;
	private int numRungs;	// # of ladder rungs on one side (e.g., number of buy orders)
	private int xt, yt;		// stores the ask/bid, respectively

	private int sleepTime;
//	private double sleepVar;
	
	public MarketMakerAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l, int mktID) {
		super(agentID, modelID, d, p, l, mktID);
		arrivalTime = new TimeStamp(0);
		sleepTime = Integer.parseInt(params.get("sleepTime"));
//		sleepVar = Double.parseDouble(params.get("sleepVar"));
		numRungs = Integer.parseInt(params.get("numRungs"));
		rungSize = Integer.parseInt(params.get("rungSize"));
		stepSize = Market.quantize(rungSize, data.tickSize);
		
		xt = -1;	// ask
		yt = -1;	// bid
	}

	@Override
	public HashMap<String, Object> getObservation() {
		return null;
	}

	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();

		int bid = getBidPrice(getMarketID()).getPrice();
		int ask = getAskPrice(getMarketID()).getPrice();

		// TODO - what if just one of the bid and ask is -1? just submit one side?
		if (bid == -1 || ask == -1) {
			log.log(Log.INFO, ts + " | " + this + " " + agentType +
					"::agentStrategy: undefined quote in market " + getMarket());
			
		} else {
			// check if bid/ask has changed; if so, submit fresh orders
			if (bid != yt || ask != xt) { 
				ArrayList<Integer> prices = new ArrayList<Integer>();
				ArrayList<Integer> quantities = new ArrayList<Integer>();
				
				int ct = numRungs * stepSize;
				int buyMinPrice = bid - ct;		// min price for buy order in the ladder
				int sellMaxPrice = ask + ct;	// max price for sell order in the ladder
				
				// check if the bid or ask crosses the NBBO 
				if (lastNBBOQuote.bestAsk < ask) {
					// buy orders:  If ASK_N < X_t, then [ASK_N, ..., Y_t]
					buyMinPrice = lastNBBOQuote.bestAsk;
				}
				if (lastNBBOQuote.bestBid > bid) {
					// sell orders: If BID_N > Y_t, then [X_t, ..., BID_N]
					sellMaxPrice = lastNBBOQuote.bestBid;
				}
				
				// build descending list of buy orders (yt, ..., yt - ct) or stops at NBBO ask
				for (int p = bid; p >= buyMinPrice; p -= stepSize) {
					if (p > 0) {
						prices.add(p);
						quantities.add(1);
					}
				}
				// build ascending list of sell orders (xt, ..., xt + ct) or stops at NBBO bid
				for (int p = ask; p <= sellMaxPrice; p += stepSize) {
					prices.add(p);
					quantities.add(-1);
				}
				
				log.log(Log.INFO, ts + " | " + getMarket() + " " + this + " " + agentType + 
						"::agentStrategy: ladder numRungs=" + numRungs + ", stepSize=" + stepSize + 
						": buys [" + buyMinPrice + ", " + bid + "] &" + 
						" sells [" + ask + ", " + sellMaxPrice + "]");
				actMap.appendActivityHashMap(submitMultipleBid(getMarket(), prices, quantities, ts));
			} else {
				log.log(Log.INFO, ts + " | " + getMarket() + " " + this + " " + agentType + 
						"::agentStrategy: no change in submitted ladder.");
			}
		}
		// update latest bid/ask prices
		xt = ask;
		yt = bid;
		
		// insert activities for next time the agent wakes up
//		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		TimeStamp tsNew = ts.sum(new TimeStamp(sleepTime));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new UpdateAllQuotes(this, tsNew));
		actMap.insertActivity(Consts.MARKETMAKER_PRIORITY, new AgentStrategy(this, market, tsNew));
		return actMap;
	}
	
}
