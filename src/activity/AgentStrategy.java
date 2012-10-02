package activity;

import entity.*;
import event.*;

/**
 * Class for executing agent strategies.
 * 
 * @author ewah
 */
public class AgentStrategy extends Activity {

	private Agent ag;
	private Market mkt;

	public AgentStrategy(Agent ag, TimeStamp t) {
		this.ag = ag;
		this.time = t;
		this.mkt = null;
	}
	
	public AgentStrategy(Agent ag, Market mkt, TimeStamp t) {
		this.ag = ag;
		this.mkt = mkt;
		this.time = t;
	}
	
	public AgentStrategy deepCopy() {
		if (mkt == null) {
			return new AgentStrategy(this.ag, this.time);
		} else {
			return new AgentStrategy(this.ag, this.mkt, this.time);
		}
	}
	
	public ActivityHashMap execute() {
		if (mkt == null) {
			return ((MMAgent)ag).agentStrategy(time);
		} else {
			return ((SMAgent)ag).agentStrategy(mkt, time);
		}
	}
	
	public String toString() {
		if (mkt == null) {
			return new String("AgentStrategy::" + this.ag.toString());
		} else {
			return new String("AgentStrategy::" + this.ag.toString() + "," + 
					this.mkt.toString());
		}
	}
}
