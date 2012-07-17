package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.Iterator;

/**
 * High-frequency trader employing latency arbitrage strategy.
 * 
 * @author ewah
 */
public class HFTAgent extends Agent {
	
	private Random m_random;
	
	private double alpha;
	private int tickSize;
	private double delta; // in percentage
	private int orderSize;
	private double sleepTimeLB;
	private double sleepTimeUB;
	private int timeLimit;
	private int lossLimit;
	private Bid clearPositionBid;
	
//    tickSize = Double.parseDouble(m_StrategyProps.getProperty("tickSize", "0.1"));
//    alpha = Double.parseDouble(m_StrategyProps.getProperty("alpha", "0.01"));
//    delta = Double.parseDouble(m_StrategyProps.getProperty("delta", "0.05"));
//    orderSize = Integer.parseInt(m_StrategyProps.getProperty("orderSize", "200"));
//    timeLimit = Integer.parseInt(m_StrategyProps.getProperty("timeLimit", "30"));
//    lossLimit = Double.parseDouble(m_StrategyProps.getProperty("lossLimit", "0.05"));
//    sleepTimeLB = Double.parseDouble(m_StrategyProps.getProperty("sleepTimeLB", "5"));
//    sleepTimeUB = Double.parseDouble(m_StrategyProps.getProperty("sleepTimeUB", "10"));
	
	/**
	 * Overloaded constructor
	 * @param agentID
	 */
	public HFTAgent(int agentID, SystemData d) {
		super(agentID, d);
		agentType = "HFT";
	}
	

	public ActivityHashMap agentStrategy(TimeStamp ts) {

//		System.out.println("HFTAgentStrategy...");

		ActivityHashMap actMap = new ActivityHashMap();
		
		BestQuote bestQuote = findBestBuySell();
		if ((bestQuote.bestSell > (1+alpha)*bestQuote.bestBuy) && (bestQuote.bestBuy >= 0) ) {
//			log(Log.INFO, "found arb op!"); // TODO -log
			System.out.println("found arb opp!");
			
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
//				addMessage("Arb opportunity exists BestQuote: (" +
//						bestQuote.bestBuy + ", " + bestQuote.bestSell + ") in markets "
//						+ bestQuote.bestBuyMarket + " and " + bestQuote.bestSellMarket);
				// TODO - logging
			}
		}
		return actMap;
	}
	
	
	/**
	 * Get the quantity for a bid between the begin and end prices
	 * @param beginPrice
	 * @param endPrice
	 * @param auctionID
	 * @param buyOrSell true if buy, false if sell
	 * @return
	 */
	private int getBidQuantity(int beginPrice, int endPrice, int auctionID, boolean buyOrSell) {
		//  System.out.println("GET QUANTITY FOR " + beginPrice + "\tTO: " + endPrice);
		int quantity = 0;
		
		HashMap<Integer,PQBid> bids = new HashMap<Integer,PQBid>(data.bidData);
		for (Map.Entry<Integer,PQBid> entry : bids.entrySet()) {
			PQBid b = entry.getValue();
			
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
