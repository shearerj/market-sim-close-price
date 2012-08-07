package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * Can act infinitely fast (i.e. sleep time = 0). Note that all Activities
 * with negative TimeStamps are considered to be infinitely fast.
 * 
 * @author ewah
 */
public class HFTAgent extends MMAgent {
	
	private double alpha;
	private double delta; // in percentage
	private int orderSize;
	private int timeLimit;
	private int lossLimit;
	private Bid clearPositionBid;

	/**
	 * Overloaded constructor
	 * @param agentID
	 */
	public HFTAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = "HFT";
		arrivalTime = new TimeStamp(0);
		
		sleepTime = Integer.parseInt(p.get(agentType).get("sleepTime"));
		sleepVar = Double.parseDouble(p.get(agentType).get("sleepVar"));
		alpha = Double.parseDouble(p.get(agentType).get("alpha"));
		delta = Double.parseDouble(p.get(agentType).get("delta"));
		orderSize = Integer.parseInt(p.get(agentType).get("orderSize"));
		timeLimit = Integer.parseInt(p.get(agentType).get("timeLimit"));
//		lossLimit = Integer.parseInt(p.get(agentType).get("lossLimit"));
		
		// Infinitely fast activities will be inserted (i.e. activities with negative time)
		infiniteActs.add(new UpdateAllQuotes(this, new TimeStamp(-1)));
		infiniteActs.add(new AgentStrategy(this, new TimeStamp(-1)));
		
		if (this.data.numMarkets != 2) {
			log.log(Log.ERROR, "HFTAgent: HFT agents need 2 markets!");
		}
	}
	
	
	public ActivityHashMap agentStrategy(TimeStamp ts) {

		// Ensure that agent has arrived in the market
		if (ts.compareTo(arrivalTime) >= 0) {
			ActivityHashMap actMap = new ActivityHashMap();

			BestQuote bestQuote = findBestBuySell();
			if ((bestQuote.bestSell > (1+alpha)*bestQuote.bestBuy) && (bestQuote.bestBuy >= 0) ) {
				log.log(Log.INFO,"HFTAgent:: found possible arb opp!");

				int buyMarketID = bestQuote.bestBuyMarket;
				int sellMarketID = bestQuote.bestSellMarket;
				Market buyMarket = data.getMarket(buyMarketID);
				Market sellMarket = data.getMarket(sellMarketID);

				int midPoint = (bestQuote.bestBuy + bestQuote.bestSell) / 2;
				int buySize = getBidQuantity(bestQuote.bestBuy, midPoint-tickSize, buyMarketID, true);
				int sellSize = getBidQuantity(midPoint+tickSize, bestQuote.bestSell, sellMarketID, false);
				int quantity = Math.min(buySize, sellSize);

				if (quantity > 0 && (buyMarketID != sellMarketID)) {
					actMap.appendActivityHashMap(addBid(buyMarket, midPoint-tickSize, quantity, ts));
					actMap.appendActivityHashMap(addBid(sellMarket, midPoint+tickSize, -quantity, ts));
					log.log(Log.INFO, "HFTAgent:: Arb opportunity exists: BestQuote(" +
							bestQuote.bestBuy + ", " + bestQuote.bestSell + ") in markets "
							+ bestQuote.bestBuyMarket + " and " + bestQuote.bestSellMarket);
				}
			}
			if (sleepTime > 0) {
				TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime()));
				actMap.insertActivity(new UpdateAllQuotes(this, tsNew));
				actMap.insertActivity(new AgentStrategy(this, tsNew));
			}
			return actMap;
		}
		return null;
	}
	
	
	/**
	 * Get the quantity for a bid between the begin and end prices.
	 * @param beginPrice
	 * @param endPrice
	 * @param marketID
	 * @param buyOrSell true if buy, false if sell
	 * @return
	 */
	private int getBidQuantity(int beginPrice, int endPrice, int marketID, boolean buyOrSell) {
		
		int quantity = 0;
		
		HashMap<Integer,Bid> bids = new HashMap<Integer,Bid>(data.getMarket(marketID).getBids());
		for (Map.Entry<Integer,Bid> entry : bids.entrySet()) {
			PQBid b = (PQBid) entry.getValue();
			
			for (Iterator<PQPoint> it = b.bidTreeSet.iterator(); it.hasNext(); ) {
				PQPoint pq = it.next();
				if (pq.getPrice().getPrice() >= beginPrice && pq.getPrice().getPrice() <= endPrice) {
					int bidQuantity = pq.getQuantity();
					if (buyOrSell && (bidQuantity < 0))
						quantity += -1 * bidQuantity;
					if (!buyOrSell && (bidQuantity > 0))
						quantity += bidQuantity;
				}
			}
		}
	    return quantity;
	}

}
