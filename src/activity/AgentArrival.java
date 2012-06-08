package activity;

import entity.Agent;
import event.*;

/**
 * Class for activity of agents arriving in a market or market(s).
 * 
 * @author ewah
 */
public class AgentArrival implements Activity {

	private Agent ag;
	
	public AgentArrival(Agent ag) {
		this.ag = ag;
	}
	
	public Event execute() {
		return this.ag.agentArrival();
	}
	
}
