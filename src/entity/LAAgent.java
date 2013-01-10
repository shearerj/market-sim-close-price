package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * Can act infinitely fast (i.e. sleep time = 0). Note that all Activities
 * with negative TimeStamps are considered to be infinitely fast.
 * 
 * @author ewah
 */
public class LAAgent extends HFTAgent {
	
	private double alpha;
	private int sleepTime;
	private double sleepVar;
	
	/**
	 * Overloaded constructor
	 * @param agentID
	 */
	public LAAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		agentType = Consts.getAgentType(this.getName());
		arrivalTime = new TimeStamp(0);

		sleepTime = Integer.parseInt(params.get("sleepTime"));
		sleepVar = Double.parseDouble(params.get("sleepVar"));
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String,Object> obs = new HashMap<String,Object>();
		obs.put("role", agentType);
		obs.put("payoff", getRealizedProfit());
		obs.put("strategy", params.get("strategy"));
		
//		HashMap<String,String> features = new HashMap<String,String>();
//		obs.put("features", features);
		
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
							"::agentStrategy: Arb opportunity exists: " + bestQuote + 
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
			}
			if (sleepTime > 0) {
				TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
				actMap.insertActivity(Consts.HFT_PRIORITY, new UpdateAllQuotes(this, tsNew));
				actMap.insertActivity(Consts.HFT_PRIORITY, new AgentStrategy(this, tsNew));
				
			} else if (sleepTime == 0) {
				// infinitely fast HFT agent
				TimeStamp tsNew = new TimeStamp(Consts.INF_TIME);
				actMap.insertActivity(Consts.HFT_PRIORITY, new UpdateAllQuotes(this, tsNew));
				actMap.insertActivity(Consts.HFT_PRIORITY, new AgentStrategy(this, tsNew));
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
		
		for (Map.Entry<Integer,Bid> entry : data.getMarket(marketID).getBids().entrySet()) {
			PQBid b = (PQBid) entry.getValue();
			
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
