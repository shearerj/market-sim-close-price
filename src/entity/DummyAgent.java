package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

/**
 * Dummy agent. Payoff is always 0. Strategy can be any string, as it is never parsed.
 * 
 * @author ewah
 */
public class DummyAgent extends MMAgent {
	
	/**
	 * Overloaded constructor
	 * @param agentID
	 */
	public DummyAgent(int agentID, SystemData d, EntityProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = Consts.getAgentType(this.getClass().getSimpleName());
		arrivalTime = new TimeStamp(0);
		params = p;
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		HashMap<String,Object> obs = new HashMap<String,Object>();
		obs.put("role", agentType);
		obs.put("payoff", 0);
		obs.put("strategy", params.get("strategy"));
		return obs;
	}
	
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		return null;
	}
	
}
