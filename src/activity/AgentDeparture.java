package activity;

import java.util.Collection;

import entity.*;
import event.*;

/**
 * Class for activity of agents leaving a market or market(s).
 * 
 * @author ewah
 */
public class AgentDeparture extends Activity {

	private Agent ag;
	
	public AgentDeparture(Agent ag, TimeStamp t) {
		super(t);
		this.ag = ag;
	}
	
	public AgentDeparture deepCopy() {
		return new AgentDeparture(this.ag, this.time);
	}
	
	public Collection<Activity> execute() {
		return ag.agentDeparture(this.time);
	}
	
	public String toString() {
		return new String("AgentDeparture::" + this.ag.toString());
	}
}
