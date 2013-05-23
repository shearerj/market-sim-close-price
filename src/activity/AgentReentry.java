package activity;

import event.*;
import entity.*;

/**
 * Class for activity of agents that re-enter a market or market(s).
 * 
 * @author ewah
 */
public class AgentReentry extends Activity {
	
	private int priority;
	private Agent ag;
	
	public AgentReentry(Agent ag, int priority, TimeStamp t) {
		this.ag = ag;
		this.priority = priority;
		this.time = t;
	}
	
	public AgentReentry deepCopy() {
		return new AgentReentry(this.ag, this.priority, this.time);
	}
	
	public ActivityHashMap execute() {
		return ag.agentReentry(priority, time); 
	}
	
	public String toString() {
		return new String("AgentReentry::" + this.ag.toString() +
							" {" + priority + "}");
	}
}
