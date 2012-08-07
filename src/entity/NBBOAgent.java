package entity;

import java.util.ArrayList;

import event.*;
import activity.*;
import systemmanager.*;
import market.*;


/**
 * NBBOAgent
 *
 * A zero-intelligence (ZI) agent operating in two-market setting with an NBBO market.
 *
 * The NBBO agent trades with only one market, and will not require the execution
 * speed of "market order." It will look at both the one market and the NBBO, and if
 * the NBBO is better, it will check if there is a match in the other market. 
 * If there is no match for the bid/ask, it will not make the trade.
 *
 * This NBBO agent bases its private value on a stochastic process, the parameters
 * of which are specified at the beginning of the game by the Game Creator.
 * The agent's private valuation is determined by value of the random process at the
 * time it enters the game. The private value is used to calculate the agent's
 * surplus (and thus the market's allocative efficiency).
 *
 * This ZI agent submits only ONE limit order with an expiration that is determined
 * when the agent is initialized. The parameters determining the distribution from
 * which the expiration is drawn are given by the strategy configuration.
 *
 * The NBBO agent is always only active in one market, and it is used in two-market
 * scenarios (for latency arbitrage simulations).
 *
 * @author ewah
 */
public class NBBOAgent extends MMAgent {

	private int meanPV;
	private double expireRate;
	private int expiration;			// time until limit order expiration
	public int privateValue;
	
	private int tradeMarketID;		// assigned at initialization
	private int altMarketID;
	
	
	/**
	 * Overloaded constructor.
	 * @param agentID
	 * @param d SystemData object
	 */
	public NBBOAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = "NBBO";
		
		sleepTime = Integer.parseInt(p.get(agentType).get("sleepTime"));
		sleepVar = Double.parseDouble(p.get(agentType).get("sleepVar"));
		meanPV = Integer.parseInt(p.get(agentType).get("meanPV"));
		expireRate = Double.parseDouble(p.get(agentType).get("expireRate"));
		
		expiration = (int) (100 * getExponentialRV(expireRate));
		privateValue = this.data.nextPrivateValue();
		arrivalTime = this.data.nextArrival();
		
		if (this.data.numMarkets != 2) {
			log.log(Log.ERROR, "NBBOAgent: NBBO agents need 2 markets!");
		}
		
		// Choose market indices based on whether agentID is even or odd
		// Ensures close to 50% even distribution in each market
		if (agentID % 2 == 0) {
			tradeMarketID = -1;
			altMarketID = -2;
		} else {
			tradeMarketID = -2;
			altMarketID = -1;
		}
	}
	
	
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		
		ActivityHashMap actMap = new ActivityHashMap();

		// identify best buy and sell offers
		BestQuote bestQuote = findBestBuySell();

		int p = 0;
		int q = 1;
		if (rand.nextDouble() < 0.5) q = -q; // 0.50% chance of being either long or short

		// basic ZI behavior - price is based on uniform dist 2 SDs away from PV
		int bidSD = 5000; // arbitrary for now, TODO
		if (q > 0) {
			p = (int) ((this.privateValue - 2*bidSD) + rand.nextDouble()*2*bidSD);
		} else {
			p = (int) (this.privateValue + rand.nextDouble()*2*bidSD);
		}
		
		int ask = lastNBBOQuote.bestAsk;
		int bid = lastNBBOQuote.bestBid;

		// if NBBO better check other market for matching quote (exact match)
		boolean nbboWorse = false;
		if (q > 0) {
			if (bestQuote.bestBuy > bid) nbboWorse = true;
		} else {
			if (bestQuote.bestSell < ask) nbboWorse = true;
		}

		boolean bidSubmitted = false;
		if (nbboWorse) {
			log.log(Log.INFO, ts.toString() + " | " + agentType + "::agentStrategy: " + ": NBBO (" + lastNBBOQuote.bestAsk + 
					", " + lastNBBOQuote.bestBid + " ) better than [[" + tradeMarketID + "]] (" + bestQuote.bestSell +
					", " + bestQuote.bestBuy + ")");

			// since NBBO is better, check other market for a matching quote
			Quote altQuote = getLatestQuote(altMarketID);
			
			if (altQuote != null) {
				if (altQuote.lastAskPrice.getPrice() == ask && altQuote.lastBidPrice.getPrice() == bid) {	
					// there is a match! so trade in the other market
					actMap.appendActivityHashMap(addBid(data.markets.get(altMarketID), p, q, ts));
					bidSubmitted = true;
					log.log(Log.INFO, ts.toString() + " | " + agentType + "::agentStrategy: " +
							"bid (" + p + "," + q + ") submitted to [[" + altMarketID + "]] expiring in "
							+ expiration);

				} else {
					// no match, so no trade!
					log.log(Log.INFO, ts.toString() + " | " + agentType + "::agentStrategy: " + "No bid submitted -- no match to NBBO (" +
							lastNBBOQuote.bestAsk +	", " + lastNBBOQuote.bestBid + ") in Market " + altMarketID +
							" (" + altQuote.lastAskPrice.getPrice() + ", " + altQuote.lastBidPrice.getPrice() + ")");
				}
			}

		} else { // current market's quote is better
			actMap.appendActivityHashMap(addBid(data.markets.get(tradeMarketID), p, q, ts));
			bidSubmitted = true;
			log.log(Log.INFO, ts.toString() + " | " + agentType + "::agentStrategy: " +
					"bid (" + p + "," + q + ") submitted to [[" + tradeMarketID + "]] expiring in "
					+ expiration);
		}
		updateTransactions(ts);
		logTransactions(ts);
			    
		// Bid expires after a given point
		if (bidSubmitted) {
			TimeStamp expireTime = ts.sum(new TimeStamp(expiration));
			actMap.insertActivity(new WithdrawBid(this, data.markets.get(tradeMarketID), expireTime));
		}
		return actMap;
	}
	
	
	/**
	 * Generate exponential random variate, with rate parameter.
	 * @param rateParam
	 * @return
	 */
	public double getExponentialRV(double rateParam) {
		double r = rand.nextDouble();
		return -Math.log(r) / rateParam;
	}

}
