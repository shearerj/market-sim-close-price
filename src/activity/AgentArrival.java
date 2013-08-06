package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.agent.*;
import event.*;

/**
 * Class for activity of agents arriving in a market or market(s).
 * 
 * @author ewah
 */
public class AgentArrival extends Activity {

	protected final Agent agent;

	public AgentArrival(Agent ag, TimeStamp currentTime) {
		super(currentTime);
		this.agent = ag;
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return agent.agentArrival(currentTime);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(
				super.hashCode()).toHashCode();
	}

	public String toString() {
		return getName() + " :: " + agent;
	}

}