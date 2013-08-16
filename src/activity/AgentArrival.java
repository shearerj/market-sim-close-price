package activity;

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

	public AgentArrival(Agent ag, TimeStamp currentTime) {
		super(currentTime);
		this.agent = ag;
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