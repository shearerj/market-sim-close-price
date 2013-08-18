package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import entity.agent.Agent;
import event.TimeStamp;

/**
 * Class for activity of agents arriving in a market or market(s).
 * 
 * @author ewah
 */
// XXX Is this activity necessary?
public class AgentArrival extends Activity {

	protected final Agent agent;

	public AgentArrival(Agent agent, TimeStamp currentTime) {
		super(currentTime);
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return agent.agentArrival(currentTime);
	}

	@Override
	public String toString() {
		return super.toString() + agent;
	}

}