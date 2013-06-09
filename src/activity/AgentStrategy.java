package activity;

import java.util.Collection;

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
		this(ag, null, t);
	}

	public AgentStrategy(Agent ag, Market mkt, TimeStamp t) {
		super(t);
		this.ag = ag;
		this.mkt = mkt;
	}

	public AgentStrategy deepCopy() {
		return new AgentStrategy(this.ag, this.mkt, this.time);
	}

	public Collection<Activity> execute(TimeStamp currentTime) {
		return ag.agentStrategy(currentTime);
	}

	@Override
	public String toString() {
		if (mkt == null) {
			return new String("AgentStrategy::" + this.ag.toString());
		} else {
			return new String("AgentStrategy::" + this.ag.toString() + ","
					+ this.mkt.toString());
		}
	}
}
