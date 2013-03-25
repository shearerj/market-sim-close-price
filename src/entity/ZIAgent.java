package entity;

import event.*;
import market.*;
import model.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;
import java.util.Random;

/**
 * ZIAgent
 *
 * A zero-intelligence (ZI) agent operating in two-market setting with an NBBO market.
 *
 * The background ZI agent trades with primarily one market, and will not require the 
 * execution speed of "market order." It will look at both the one market and the NBBO, 
 * and if the NBBO is better, it will check if there is a match in the other market. 
 * If there is no match for the bid/ask, it will not make the trade. (NOTE: This is
 * the behavior of all SMAgents.)
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
 * The background ZI agent is always only active in one market, but it needs a minimum
 * of two markets (for latency arbitrage scenarios).
 * 
 * NOTE: The limit order price is uniformly distributed over a range that is twice the
 * size of bidRange in either a positive or negative direction from the agent's
 * private value.
 *
 * @author ewah
 */
public class ZIAgent extends SMAgent {

//	private double expireRate;
//	private long expiration;			// time until limit order expiration
	private int bidRange;				// range for limit order
	private double pvVar;				// variance from private value random process
	
	
	/**
	 * Overloaded constructor.
	 */
	public ZIAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l, int mktID) {
		super(agentID, modelID, d, p, l, mktID);
		
		rand = new Random(Long.parseLong(params.get("seed")));
		// bidRange = this.data.bidRange;
		bidRange = Integer.parseInt(params.get("bidRange"));
		pvVar = this.data.privateValueVar;
//		expireRate = this.data.expireRate;
//		expiration = (long) getExponentialRV(expireRate);
		
		arrivalTime = new TimeStamp(Long.parseLong(params.get("arrivalTime")));
		int pv = Integer.parseInt(params.get("fundamental"));
		privateValue = Math.max(0, pv + (int) Math.round(getNormalRV(0, pvVar)));
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		return null;
	}
	
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();

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

//		actMap.appendActivityHashMap(submitNMSBid(p, q, expiration, ts));
		actMap.appendActivityHashMap(submitNMSBid(p, q, ts));	// bid does not expire
		return actMap;
	}

	
//	/**
//	 * @return expiration
//	 */
//	public long getExpiration() {
//		return expiration;
//	}
	
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
