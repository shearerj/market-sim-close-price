package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;
import java.util.Random;

/**
 * ZIAGENT
 *
 * A zero-intelligence (ZI) agent.
 *
 * This agent bases its private value on a stochastic process, the parameters
 * of which are specified at the beginning of the simulation by the spec file.
 * The agent's private valuation is determined by value of the random process at
 * the time it enters, with some randomization added by using an individual 
 * variance parameter. The private value is used to calculate the agent's surplus 
 * (and thus the market's allocative efficiency).
 *
 * This agent submits only ONE limit order during its lifetime.
 * 
 * NOTE: The limit order price is uniformly distributed over a range that is twice
 * the size of bidRange in either a positive or negative direction from the agent's
 * private value.
 *
 * @author ewah
 */
public class ZIAgent extends SMAgent {

	private int bidRange;				// range for limit order
	private double pvVar;				// variance from private value random process
	
	/**
	 * Overloaded constructor.
	 */
	public ZIAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		
		rand = new Random(Long.parseLong(params.get(Agent.RANDSEED_KEY)));
		arrivalTime = new TimeStamp(Long.parseLong(params.get(Agent.ARRIVAL_KEY)));
		bidRange = Integer.parseInt(params.get(ZIAgent.BIDRANGE_KEY));

		pvVar = this.data.privateValueVar;
		int fund = Integer.parseInt(params.get(Agent.FUNDAMENTAL_KEY));
		privateValue = Math.max(0, fund + (int) Math.round(getNormalRV(0, pvVar)));
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String,Object> obs = new HashMap<String,Object>();
		obs.put(Observations.ROLE_KEY, agentType);
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
}
