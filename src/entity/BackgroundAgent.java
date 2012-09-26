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
 * The background agent is always only active in one market, but it needs a minimum
 * of two markets (for latency arbitrage scenarios).
 * 
 * BID SUBMISSION:
 * 
 * The agent will submit to the alternate market ONLY if both the NBBO quote is better
 * than the main market's quote and the bid to submit will transact immediately 
 * given the price in the alternate market. The only difference in outcome occurs
 * when the NBBO is out-of-date and the agent submits a bid to the main market
 * although the alternate market is actually better.
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
	
	private int mainMarketID;			// assigned at initialization
	private int altMarketID;
	
	public Consts.SubmittedBidMarket submittedBidType;
	
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
		valueVar = this.data.privateValueVar;
		expiration = (int) (100 * getExponentialRV(expireRate));
		arrivalTime = this.data.nextArrival();
		privateValue = Math.max(0, this.data.nextPrivateValue() + (int) Math.round(getNormalRV(0,valueVar)));
		
		if (this.data.numMarkets != 2) {
			log.log(Log.ERROR, "NBBOAgent: NBBO agents need 2 markets!");
		}
		
		// Choose market indices based on whether agentID is even or odd
		// Ensures close to 50% even distribution in each market
		if (agentID % 2 == 0) {
			mainMarketID = -1;
			altMarketID = -2;
		} else {
			mainMarketID = -2;
			altMarketID = -1;
		}
		
		submittedBidType = Consts.SubmittedBidMarket.NOBID;
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		return null;
	}
	
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		
		ActivityHashMap actMap = new ActivityHashMap();

		// identify best buy and sell offers (for all markets)
		Quote mainMarketQuote = data.getMarket(mainMarketID).quote(ts);

		int p = 0;
		int q = 1;
		// 0.50% chance of being either long or short
		if (rand.nextDouble() < 0.5)
			q = -q; 

		// basic ZI behavior
		if (q > 0) {
			p = (int) Math.max(0, ((this.privateValue - 2*bidRange) + rand.nextDouble()*2*bidRange));
		} else {
			p = (int) Math.max(0, (this.privateValue + rand.nextDouble()*2*bidRange));
		}

		// Check if NBBO indicates that other market better
		// - Want to buy for as low a price as possible, so find market with the lowest ask
		// - Want to sell for as high a price as possible, so find market with the highest bid
		boolean nbboBetter = false;
		if (q > 0) {
			if (lastNBBOQuote.bestAsk < mainMarketQuote.lastAskPrice.getPrice() &&
					lastNBBOQuote.bestAsk != -1) { 
				nbboBetter = true;
			}
		} else {
			if (lastNBBOQuote.bestBid > mainMarketQuote.lastBidPrice.getPrice() &&
					mainMarketQuote.lastBidPrice.getPrice() != -1) {
				nbboBetter = true;
			}
		}
		
		submittedBidType = Consts.SubmittedBidMarket.MAIN;	// default is submitting to main market
		if (nbboBetter) {
			// nbboBetter = true indicates that the alternative market has a better quote
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "NBBO(" + lastNBBOQuote.bestBid + ", " + 
					lastNBBOQuote.bestAsk + ") better than " + data.getMarket(mainMarketID) + 
					" Quote(" + mainMarketQuote.lastBidPrice.getPrice() + 
					", " + mainMarketQuote.lastAskPrice.getPrice() + ")");
			
			int bestMarketID = mainMarketID;
			int bestPrice = -1;
			if (q > 0) {
				if (p > lastNBBOQuote.bestAsk) {
					bestMarketID = altMarketID;
					bestPrice = lastNBBOQuote.bestAsk;
				}
			} else {
				if (p < lastNBBOQuote.bestBid) {
					bestMarketID = altMarketID;
					bestPrice = lastNBBOQuote.bestBid;
				}
			}
			
			if (bestMarketID == altMarketID) {
				// specify that submitting to the alternate market
				submittedBidType = Consts.SubmittedBidMarket.ALTERNATE;
				
				log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
						"::agentStrategy: " + "Bid +(" + p + "," + q + ") will transact" +
						" immediately in " + data.getMarket(altMarketID) +
						" given best price " + bestPrice);
			}
			
			// submit bid to the best market
			actMap.appendActivityHashMap(addBid(data.markets.get(bestMarketID), p, q, ts));
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "+(" + p + "," + q + ") to " + 
					data.getMarket(bestMarketID) + ", duration=" + expiration);
			
		} else {
			// main market is better than the alternate market (according to NBBO)
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "NBBO(" + lastNBBOQuote.bestBid + ", " + 
					lastNBBOQuote.bestAsk + ") worse than/same as " + data.getMarket(mainMarketID) + 
					" Quote(" + mainMarketQuote.lastBidPrice.getPrice() + 
					", " + mainMarketQuote.lastAskPrice.getPrice() + ")");
			
			// submit bid to the main market
			actMap.appendActivityHashMap(addBid(data.markets.get(mainMarketID), p, q, ts));
			log.log(Log.INFO, ts.toString() + " | " + this.toString() + " " + agentType + 
					"::agentStrategy: " + "+(" + p + "," + q + ") to " + 
					data.getMarket(mainMarketID) + ", duration=" + expiration);
			
		}
			    
		// Bid expires after a given duration
		TimeStamp expireTime = ts.sum(new TimeStamp(expiration));
		actMap.insertActivity(new WithdrawBid(this, data.markets.get(mainMarketID), expireTime));
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
