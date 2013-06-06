package activity;


import java.util.Collection;

import entity.*;
import event.*;

/**
 * Class for activity of agents arriving in a market or market(s).
 * 
 * @author ewah
 */
public class AgentArrival extends Activity {

	private Agent ag;
	
	public AgentArrival(Agent ag, TimeStamp t) {
		super(t);
		this.ag = ag;
	}
	
	public AgentArrival deepCopy() {
		return new AgentArrival(this.ag, this.time);
	}
	
	public Collection<Activity> execute() {
		return ag.agentArrival(time); 
	}
	
	public String toString() {
		return new String("AgentArrival::" + this.ag.toString());
	}
}
