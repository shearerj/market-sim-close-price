package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

/**
 * BackgroundAgent
 *
 * A zero-intelligence (ZI) agent operating in two-market setting with an NBBO market.
 *
 * The background agent trades with only one market, and will not require the execution
 * speed of "market order." It will look at both the one market and the NBBO, and if
 * the NBBO is better, it will check if there is a match in the other market. 
 * If there is no match for the bid/ask, it will not make the trade.
 *
 * This agent bases its private value on a stochastic process, the parameters
 * of which are specified at the beginning of the simulation by the spec file.
 * The agent's private valuation is determined by value of the random process at the
 * time it enters the game, with some randomization added by using an individual 
 * variance parameter. The private value is used to calculate the agent's surplus 
 * (and thus the market's allocative efficiency).
 *
 * This agent submits only ONE limit order with an expiration that is determined
 * when the agent is initialized. The parameters determining the distribution from
 * which the expiration is drawn are given by the strategy configuration.
 *
 * The background agent is always only active in one market, and it is used in two-market
 * scenarios (for latency arbitrage simulations).
 * 
 * NOTE: The limit order price is uniformly distributed over a range that is twice the
 * size of bidRange in either a positive or negative direction from the agent's
 * private value.
 *
 * @author ewah
 */
public class BackgroundAgent extends MMAgent {

	private double expireRate;
	private int expiration;				// time until limit order expiration
	private int bidRange;				// range for limit order
	private double valueVar;			// variance from private value random process
	
	private int tradeMarketID;			// assigned at initialization
	private int altMarketID;
	
	public Consts.SubmittedBidType submittedBidType;
	
	/**
	 * Overloaded constructor.
	 * @param agentID
	 * @param d SystemData object
	 */
	public BackgroundAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = Consts.getAgentType(this.getClass().getSimpleName());
		params = p;
		
		expireRate = this.data.expireRate;
		bidRange = this.data.bidRange;
		valueVar = this.data.valueVar;
		expiration = (int) (100 * getExponentialRV(expireRate));
		arrivalTime = this.data.nextArrival();
		privateValue = Math.max(0, this.data.nextPrivateValue() + (int) Math.round(getNormalRV(0,valueVar)));
		
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
		
		submittedBidType = Consts.SubmittedBidType.NOBID;
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
		// 0.50% chance of being either long or short
		if (rand.nextDouble() < 0.5) q = -q; 

		// basic ZI behavior
		if (q > 0) {
			p = (int) Math.max(0, ((this.privateValue - 2*bidRange) + rand.nextDouble()*2*bidRange));
		} else {
			p = (int) Math.max(0, (this.privateValue + rand.nextDouble()*2*bidRange));
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
				log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
						"::agentStrategy: " + "Best market is " + data.getMarket(bestMarketID) +
						" with order to submit: (" + q + ", " + bestPrice + ")");
				
				actMap.appendActivityHashMap(addBid(data.markets.get(bestMarketID), p, q, ts));
				bidSubmitted = true;
				log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
							"::agentStrategy: " + "+(" + p + "," + q + ") to " + 
							data.getMarket(bestMarketID) + ", duration="	+ expiration);
				// track that trading in the other market
				submittedBidType = Consts.SubmittedBidType.ALTERNATE;
			}

		} else {
			// current market's quote is better than NBBO
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "NBBO (" + lastNBBOQuote.bestBid + ", " + 
					lastNBBOQuote.bestAsk + ") worse than " + data.getMarket(tradeMarketID) + 
					" (" + bestQuote.bestSell +	", " + bestQuote.bestBuy + ")");
			
			// potential arb opp when NBBO out of date and should be better
			actMap.appendActivityHashMap(addBid(data.markets.get(tradeMarketID), p, q, ts));
			bidSubmitted = true;
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "+(" + p + "," + q + ") to " + data.getMarket(tradeMarketID) + 
					", duration="	+ expiration);
			submittedBidType = Consts.SubmittedBidType.CURRENT;
		}
			    
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
	private double getExponentialRV(double rateParam) {
		double r = rand.nextDouble();
		return -Math.log(r) / rateParam;
	}

}
