package activity;

import java.util.Collection;

import event.*;
import entity.*;

/**
 * Class for activity of agents that re-enter a market or market(s).
 * 
 * @author ewah
 */
public class AgentReentry extends Activity {
	
	private Agent ag;
	
	public AgentReentry(Agent ag, TimeStamp t) {
		super(t);
		this.ag = ag;
	}
	
	public AgentReentry deepCopy() {
		return new AgentReentry(this.ag, this.time);
	}
	
	public Collection<Activity> execute(TimeStamp currentTime) {
		return ag.agentReentry(currentTime); 
	}
	
	public String toString() {
		return new String("AgentReentry::" + this.ag.toString());
	}
}
