package entity;

import java.util.ArrayList;
import java.util.HashMap;

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
 * NOTE: limit order price is based on uniform dist 2 std devs away from the PV.
 * The stdev is given as an input parameter.
 *
 * @author ewah
 */
public class NBBOAgent extends MMAgent {

	private int meanPV;
	private double expireRate;
	private int expiration;			// time until limit order expiration
	private int privateValue;
	private int bidSD;				// std dev for bids (above/below PV)
	
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
		params = p;
		
		meanPV = this.data.meanPV;
		expireRate = this.data.expireRate;
		bidSD = this.data.bidSD;
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
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		return null;
	}
	
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		
		ActivityHashMap actMap = new ActivityHashMap();

		// identify best buy and sell offers
		BestQuote bestQuote = findBestBuySell();

		int p = 0;
		int q = 1;
		if (rand.nextDouble() < 0.5) q = -q; // 0.50% chance of being either long or short

		// basic ZI behavior
		if (q > 0) {
			p = (int) ((this.privateValue - 2*bidSD) + rand.nextDouble()*2*bidSD);
		} else {
			p = (int) (this.privateValue + rand.nextDouble()*2*bidSD);
		}
		
		int ask = lastNBBOQuote.bestAsk;
		int bid = lastNBBOQuote.bestBid;
		int bestPrice = -1;

		// if NBBO better check other market for matching quote (exact match)
		boolean nbboBetter = false;
		if (q > 0) {
			if (bestQuote.bestBuy > bid) {
				nbboBetter = true;
				bestPrice = bestQuote.bestBuy;
			}
		} else {
			if (bestQuote.bestSell < ask) { 
				nbboBetter = true;
				bestPrice = bestQuote.bestSell;
			}
		}

		boolean bidSubmitted = false;
		if (nbboBetter) {
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "NBBO (" + lastNBBOQuote.bestBid + ", " + 
					lastNBBOQuote.bestAsk + ") better than " + data.getMarket(tradeMarketID) + 
					" (" + bestQuote.bestSell +	", " + bestQuote.bestBuy + ")");

			// since NBBO is better, check other market for a matching quote
			Quote altQuote = getLatestQuote(altMarketID);
			
			// submit bid to whichever market has the best quote
			if (altQuote != null) {
				
				int bestMarketID = tradeMarketID;
				
				if (q > 0) {
					if (altQuote.lastBidPrice.getPrice() > bid) {
						bestMarketID = altMarketID;
						bestPrice = altQuote.lastBidPrice.getPrice();
					}
				} else {
					if (altQuote.lastAskPrice.getPrice() < ask) {
						bestMarketID = altMarketID;
						bestPrice = altQuote.lastAskPrice.getPrice();
					}
				}
				// track that trading in the original market
				log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
						"::agentStrategy: " + "Best market is " + data.getMarket(bestMarketID) +
						" with order to submit: (" + q + ", " + bestPrice + ")");
				
				actMap.appendActivityHashMap(addBid(data.markets.get(bestMarketID), p, q, ts));
				bidSubmitted = true;
				log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
							"::agentStrategy: " + "+(" + p + "," + q + ") to " + altMarketID +
							" expiration="	+ expiration);

//					
			}

		} else {
			// current market's quote is better
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "NBBO (" + lastNBBOQuote.bestBid + ", " + 
					lastNBBOQuote.bestAsk + ") worse than " + data.getMarket(tradeMarketID) + 
					" (" + bestQuote.bestSell +	", " + bestQuote.bestBuy + ")");
			
			actMap.appendActivityHashMap(addBid(data.markets.get(tradeMarketID), p, q, ts));
			bidSubmitted = true;
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "+(" + p + "," + q + ") to " + data.getMarket(tradeMarketID) + 
					" expiration="	+ expiration);
		}
			    
		// Bid expires after a given point
		if (bidSubmitted) {
			TimeStamp expireTime = ts.sum(new TimeStamp(expiration));
			actMap.insertActivity(new WithdrawBid(this, data.markets.get(tradeMarketID), expireTime));
		}
		return actMap;
	}
	
	
	/**
	 * @return agent's private value
	 */
	public int getPrivateValue() {
		return privateValue;
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
