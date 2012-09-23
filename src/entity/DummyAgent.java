package entity;

import event.*;
import activity.*;
import systemmanager.*;

import java.util.HashMap;

/**
 * Dummy agent. Used for experiments (on EGTA) without any HFT agent or any
 * player in a "role."
 * 
 * @author ewah
 */
public class DummyAgent extends MMAgent {
	
	/**
	 * Overloaded constructor
	 * @param agentID
	 */
	public DummyAgent(int agentID, SystemData d, AgentProperties p, Log l) {
		super(agentID, d, p, l);
		agentType = Consts.getAgentType(this.getClass().getSimpleName());
		arrivalTime = new TimeStamp(0);
		params = p;
	}
	
	
	@Override
	public HashMap<String, Object> getObservation() {
		return null;
	}
	
	
	@Override
	public ActivityHashMap agentStrategy(TimeStamp ts) {
		return null;
	}
	
}
