package entity;

import data.ObjectProperties;
import data.Observations;
import data.SystemData;
import event.*;
import market.*;
import activity.Activity;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Collection;
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
public class ZIAgent extends BackgroundAgent {

	private int bidRange;				// range for limit order
	
	/**
	 * Overloaded constructor.
	 */
	public ZIAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
	
		rand = new Random(Long.parseLong(params.get(Agent.RANDSEED_KEY)));
		arrivalTime = new TimeStamp(Long.parseLong(params.get(Agent.ARRIVAL_KEY)));
		bidRange = Integer.parseInt(params.get(ZIAgent.BIDRANGE_KEY));
		int alpha1 = (int) Math.round(getNormalRV(0, this.data.pvVar));
		int alpha2 = (int) Math.round(getNormalRV(0, this.data.pvVar));
		alpha = new PrivateValue(alpha1, alpha2);
		// if (rand.nextBoolean()) alpha.reverseValues();
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
	public Collection<Activity> agentStrategy(TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();

		// update quotes
		this.updateAllQuotes(ts);
		
		int p = 0;
		int q = 1;
		if (rand.nextBoolean()) q = -q;	 // 50% chance of being either long or short
		int val = Math.max(0, data.getFundamentalAt(ts).sum(getPrivateValueAt(q)).getPrice());

		// basic ZI behavior
		if (q > 0) {
			p = (int) Math.max(0, ((val-2*bidRange) + rand.nextDouble()*2*bidRange));
		} else {
			p = (int) Math.max(0, (val + rand.nextDouble()*2*bidRange));
		}

//		actMap.appendCollection<Activity>(submitNMSBid(p, q, expiration, ts));
		actMap.addAll(submitNMSBid(p, q, ts));	// bid does not expire
		return actMap;
	}
}
