package entity;

import event.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;
import java.util.Random;

/**
 * AAAgent
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
public class AAAgent extends BackgroundAgent {

	private int bidRange;				// range for limit order

	
	/**
	 * Overloaded constructor.
	 */
	public AAAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		
		rand = new Random(Long.parseLong(params.get(Agent.RANDSEED_KEY)));
		arrivalTime = new TimeStamp(Long.parseLong(params.get(Agent.ARRIVAL_KEY)));
		bidRange = Integer.parseInt(params.get(AAAgent.BIDRANGE_KEY));
		int alpha1 = (int) Math.round(getNormalRV(0, this.data.pvVar));
		int alpha2 = (int) Math.round(getNormalRV(0, this.data.pvVar));
		alpha = new PrivateValue(alpha1, alpha2);
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String,Object> obs = new HashMap<String,Object>();
		obs.put(Observations.ROLE_KEY, getRole());
		obs.put(Observations.PAYOFF_KEY, getRealizedProfit());
		obs.put(Observations.STRATEGY_KEY, getFullStrategy());
		return obs;
	}
	
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		ActivityHashMap actMap = new ActivityHashMap();

		int p = 0;
		int q = 1;
		// 0.50% chance of being either long or short
		if (rand.nextDouble() < 0.5) q = -q;
		int val = Math.max(0, data.getFundamentalAt(ts).sum(getPrivateValueAt(q)).getPrice());

		// basic ZI behavior
		if (q > 0) {
			p = (int) Math.max(0, ((val-2*bidRange) + rand.nextDouble()*2*bidRange));
		} else {
			p = (int) Math.max(0, (val + rand.nextDouble()*2*bidRange));
		}

		actMap.appendActivityHashMap(submitNMSBid(p, q, ts));	// bid does not expire
		return actMap;
	}
}
