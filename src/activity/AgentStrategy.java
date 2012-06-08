package activity;

import entity.Agent;
import event.*;

/**
 * Class for executing agent strategies.
 * 
 * @author ewah
 */
public class AgentStrategy implements Activity {

	private Agent ag;
	
	public AgentStrategy(Agent ag) {
		this.ag = ag;
	}
	
	public Event execute() {
		return this.ag.agentStrategy();
	}
}
