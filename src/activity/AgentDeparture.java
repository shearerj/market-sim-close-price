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
	
	public AgentDeparture(Agent ag, TimeStamp t) {
		this.ag = ag;
		this.time = t;
		this.mkt = null;
	}
	
	public AgentDeparture deepCopy() {
		if (mkt == null) {
			return new AgentDeparture(this.ag, this.time);
		} else {
			return new AgentDeparture(this.ag, this.mkt, this.time);
		}
	}
	
	public ActivityHashMap execute() {
		return ag.agentDeparture();
	}
	
	public String toString() {
		if (mkt == null) {
			return new String("AgentDeparture::" + this.ag.toString());
		} else {
			return new String("AgentDeparture::" + this.ag.toString() + "," +
							this.mkt.toString());
		}
	}
}
