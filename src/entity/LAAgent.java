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
public class LAAgent extends MMAgent {
	
	private double alpha;
	
	/**
	 * Overloaded constructor
	 * @param agentID
	 */
	public LAAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = Consts.getAgentType(this.getClass().getSimpleName());
		arrivalTime = new TimeStamp(0);
		params = p;
		
		if (this.data.numMarkets != 2) {
			log.log(Log.ERROR, this.toString() + " " + agentType + 
					": Latency arbitrageurs need 2 markets!");
		}
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
				
				log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
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
					actMap.appendActivityHashMap(addBid(buyMarket, midPoint-tickSize, 
							quantity, ts));
					actMap.appendActivityHashMap(addBid(sellMarket, midPoint+tickSize, 
							-quantity, ts));
					log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
							"::agentStrategy: Arb opportunity exists: " + bestQuote + 
							" in " + data.getMarket(bestQuote.bestBuyMarket) + " & " 
							+ data.getMarket(bestQuote.bestSellMarket));
				} else if (buyMarketID == sellMarketID) {
					log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
							"::agentStrategy: No arb opp since same market");
					// Note that this is due to a market not having both a bid & ask price,
					// causing the buy and sell market IDs to be identical
				} else if (quantity == 0) {
					log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
							"::agentStrategy: No quantity available");
				}
			}
			int sleepTime = Integer.parseInt(params.get("sleepTime"));
			if (sleepTime > 0) {
				double sleepVar = Double.parseDouble(params.get("sleepVar"));
				TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
				actMap.insertActivity(new UpdateAllQuotes(this, tsNew));
				actMap.insertActivity(new AgentStrategy(this, tsNew));
				
			} else if (sleepTime == 0) {
				// infinitely fast HFT agent, occurs after every event
				actMap.insertActivity(new UpdateAllQuotes(this, 
						new TimeStamp(EventManager.FastActivityType.POST)));
				actMap.insertActivity(new AgentStrategy(this, 
						new TimeStamp(EventManager.FastActivityType.POST)));
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
