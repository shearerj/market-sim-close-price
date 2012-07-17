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
	
	public AgentArrival(Agent ag, Market mkt, TimeStamp t) {
		this.ag = ag;
		this.mkt = mkt;
		this.time = t;
	}
	
	public ActivityHashMap execute() {
		return this.ag.agentArrival(this.mkt, this.time);
	}
	
	public String toString() {
		return new String("AgentArrival(Agt " + this.ag.getID() + ", Mkt " +
							this.mkt.getID() + ")");
	}
}
