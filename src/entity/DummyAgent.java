package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * Dummy agent. Payoff is always 0. Strategy can be any string, as it is never parsed.
 * 
 * @author ewah
 */
public class DummyAgent extends HFTAgent {
	
	/**
	 * Overloaded constructor
	 * @param agentID
	 */
	public DummyAgent(int agentID, int modelID, SystemData d, ObjectProperties p, Log l) {
		super(agentID, modelID, d, p, l);
		arrivalTime = new TimeStamp(0);
		params = p;
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String,Object> obs = new HashMap<String,Object>();
//		obs.put(Observations.ROLES_KEY, getRole());
//		obs.put(Observations.PAYOFF_KEY, 0);
//		obs.put(Observations.STRATEGY_KEY, params.get(Agent.STRATEGY_KEY));
		return obs;
	}
	
	
	@Override
	public Collection<Activity> agentStrategy(TimeStamp ts) {
		return Collections.emptyList();
	}
	
}
