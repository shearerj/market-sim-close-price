package activity;


import entity.*;
import event.*;

/**
 * Class for activity of agents arriving in a market or market(s).
 * 
 * @author ewah
 */
public class AgentArrival extends Activity {

	private Agent ag;
	private Market mkt;
	
	public AgentArrival(Agent ag, TimeStamp t) {
		this.ag = ag;
		this.time = t;
		this.mkt = null;
	}
	
	public AgentArrival(Agent ag, Market mkt, TimeStamp t) {
		this.ag = ag;
		this.mkt = mkt;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		if (mkt == null) {
			return ((MMAgent)ag).agentArrival(time);
		} else {
			return ((SMAgent)ag).agentArrival(mkt, time);
		}
	}
	
	public String toString() {
		if (mkt == null) {
			return new String("AgentArrival::" + this.ag.toString());
		} else {
			return new String("AgentArrival::" + this.ag.toString() + "," +
							this.mkt.toString());
		}
	}
}
