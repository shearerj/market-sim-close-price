package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.*;
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

	public String toString() {
		return new String(getName() + "::" + agent);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof AgentArrival))
			return false;
		AgentArrival other = (AgentArrival) obj;
		return new EqualsBuilder().append(agent, other.agent).append(scheduledTime,
				other.scheduledTime).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(scheduledTime).toHashCode();
	}
}
