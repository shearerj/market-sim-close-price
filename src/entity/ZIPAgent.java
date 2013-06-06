package entity;

import event.*;
import market.*;
import activity.Activity;
import activity.AgentStrategy;
import activity.UpdateAllQuotes;
import systemmanager.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;


/**
 * Use kappa to set 'c' for the ZIP agent
 * 
 * Use \mu from Cliff's paper
 * 
 * Ensure that transaction price != \lambda
 * 
 * 
 * 
 * @author ewah, sgchako, kunshao, marzuq, gshiva
 *
 */
public class ZIPAgent extends BackgroundAgent {

	@SuppressWarnings("unused")
	private int bidRange;				// range for limit order
	
	public ZIPAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		
		rand = new Random(Long.parseLong(params.get(Agent.RANDSEED_KEY)));
		arrivalTime = new TimeStamp(Long.parseLong(params.get(Agent.ARRIVAL_KEY)));
		bidRange = Integer.parseInt(params.get(ZIAgent.BIDRANGE_KEY));
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
	public Collection<Activity> agentStrategy(TimeStamp ts) {
		Collection<Activity> actMap = new ArrayList<Activity>();
		int q = 1;
		// 0.50% chance of being either long or short
		if (rand.nextDouble() < 0.5) q = -q;
		@SuppressWarnings("unused")
		int val = Math.max(0, data.getFundamentalAt(ts).sum(getPrivateValueAt(q)).getPrice());

		// Insert events for the agent to sleep, then wake up again at timestamp tsNew
		int sleepTime = Integer.parseInt(params.get(SLEEPTIME_KEY));
		double sleepVar = Double.parseDouble(params.get(SLEEPVAR_KEY));
		TimeStamp tsNew = ts.sum(new TimeStamp(getRandSleepTime(sleepTime, sleepVar)));
		actMap.add(new UpdateAllQuotes(this, tsNew));
		actMap.add(new AgentStrategy(this, tsNew));
		return actMap;
	}
}
