package entity;

import data.*;
import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;
import java.util.Iterator;

/**
 * LAAGENT
 * 
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * This agent can act infinitely fast (i.e. sleep time = 0).
 * 
 * @author ewah
 */
public class LAAgent extends HFTAgent {
	
	private double alpha;
	private int sleepTime;
	private double sleepVar;
	
	
	/**
	 * @param agentID
	 */
	public LAAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		arrivalTime = new TimeStamp(0);

		alpha = Double.parseDouble(params.get(LAAgent.ALPHA_KEY));
		sleepTime = Integer.parseInt(params.get(Agent.SLEEPTIME_KEY));
		sleepVar = Double.parseDouble(params.get(Agent.SLEEPVAR_KEY));
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String,Object> obs = new HashMap<String,Object>();
		obs.put(Observations.ROLE_KEY, getRole());
		obs.put(Observations.PAYOFF_KEY, getRealizedProfit());
		obs.put(Observations.STRATEGY_KEY, getFullStrategy());
//		HashMap<String,String> features = new HashMap<String,String>();
//		obs.put(Observations.FEATURES_KEY, features);
		return obs;
	}
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {

		// Ensure that agent has arrived in the market
		if (ts.compareTo(arrivalTime) >= 0) {
			ActivityHashMap actMap = new ActivityHashMap();

			BestQuote bestQuote = findBestBuySell();
			if ((bestQuote.bestSell > (1+alpha)*bestQuote.bestBuy) && (bestQuote.bestBuy >= 0) ) {
				
				log.log(Log.INFO, ts.toString() + " | " + this + " " + agentType + 
						"::agentStrategy: Found possible arb opp!");

				int buyMarketID = bestQuote.bestBuyMarket;
				int sellMarketID = bestQuote.bestSellMarket;
				Market buyMarket = data.getMarket(buyMarketID);
				Market sellMarket = data.getMarket(sellMarketID);

				// check that BID/ASK defined for both markets
				if (buyMarket.defined() && sellMarket.defined()) {
					
					int midPoint = (bestQuote.bestBuy + bestQuote.bestSell) / 2;
					int buySize = getBidQuantity(bestQuote.bestBuy, midPoint-tickSize, 
							buyMarketID, true);
					int sellSize = getBidQuantity(midPoint+tickSize, bestQuote.bestSell, 
							sellMarketID, false);
					int quantity = Math.min(buySize, sellSize);

					if (quantity > 0 && (buyMarketID != sellMarketID)) {
						actMap.appendActivityHashMap(submitBid(buyMarket, midPoint-tickSize, 
								quantity, ts));
						actMap.appendActivityHashMap(submitBid(sellMarket, midPoint+tickSize, 
								-quantity, ts));
						log.log(Log.INFO, ts.toString() + " | " + this + " " + agentType + 
								"::agentStrategy: Exploit existing arb opp: " + bestQuote + 
								" in " + data.getMarket(bestQuote.bestBuyMarket) + " & " 
								+ data.getMarket(bestQuote.bestSellMarket));

					} else if (buyMarketID == sellMarketID) {
						log.log(Log.INFO, ts.toString() + " | " + this + " " + agentType + 
								"::agentStrategy: No arb opp since at least 1 market does not " +
								"have both a bid and an ask");
						// Note that this is due to a market not having both a bid & ask price,
						// causing the buy and sell market IDs to be identical

					} else if (quantity == 0) {
						log.log(Log.INFO, ts.toString() + " | " + this + " " + agentType + 
								"::agentStrategy: No quantity available");
						// Note that if this message appears in a CDA market, then the HFT
						// agent is beating the market's Clear activity, which is incorrect.
					}
					
				} else {
					log.log(Log.INFO, ts.toString() + " | " + this + " " + agentType + 
							"::agentStrategy: Market quote(s) undefined. No bid submitted.");
				}
				
			}
			TimeStamp tsNew = new TimeStamp();
			if (sleepTime > 0) {
				tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
				actMap.insertActivity(Consts.HFT_ARRIVAL_PRIORITY, 
						new AgentReentry(this, Consts.HFT_AGENT_PRIORITY, tsNew));
//				actMap.insertActivity(Consts.HFT_AGENT_PRIORITY, new UpdateAllQuotes(this, tsNew));
//				actMap.insertActivity(Consts.HFT_AGENT_PRIORITY, new AgentStrategy(this, tsNew));
				
			} else if (sleepTime == 0) {
				// infinitely fast HFT agent
				tsNew = new TimeStamp(Consts.INF_TIME);
//				actMap.insertActivity(Consts.DEFAULT_PRIORITY, 
//						new AgentReentry(this, Consts.HFT_ARRIVAL_PRIORITY, tsNew));
				actMap.insertActivity(Consts.HFT_AGENT_PRIORITY, new UpdateAllQuotes(this, tsNew));
				actMap.insertActivity(Consts.HFT_AGENT_PRIORITY, new AgentStrategy(this, tsNew));
			}
			return actMap;
		}
		return null;
	}
	
	
	/**
	 * Get the quantity for a bid between the begin and end prices.
	 * 
	 * @param beginPrice
	 * @param endPrice
	 * @param marketID
	 * @param buy true if buy, false if sell
	 * @return
	 */
	private int getBidQuantity(int beginPrice, int endPrice, int marketID, boolean buy) {
		
		int quantity = 0;
		
		for (Bid bid : data.getMarket(marketID).getBids().values()) {
			PQBid b = (PQBid) bid;
			
			for (Iterator<PQPoint> it = b.bidTreeSet.iterator(); it.hasNext(); ) {
				PQPoint pq = it.next();
				int pqPrice = pq.getPrice().getPrice();
				
				if (pqPrice >= beginPrice && pqPrice <= endPrice) {
					int bidQuantity = pq.getQuantity();
					
					// Buy
					if (buy && (bidQuantity < 0))
						quantity -= bidQuantity;
					// Sell
					if (!buy && (bidQuantity > 0))
						quantity += bidQuantity;
				}
			}
		}
	    return quantity;
	}

}
