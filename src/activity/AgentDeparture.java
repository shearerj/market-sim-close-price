package activity;

import entity.*;
import event.*;

/**
 * Class for activity of agents leaving a market or market(s).
 * 
 * @author ewah
 */
public class AgentDeparture extends Activity {

	private Agent ag;
	private Market mkt;
	
	public AgentDeparture(Agent ag, Market mkt, TimeStamp t) {
		this.ag = ag;
		this.mkt = mkt;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.agentDeparture(this.mkt);
	}
	
}
