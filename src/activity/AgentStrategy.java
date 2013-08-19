package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import entity.agent.Agent;
import event.TimeStamp;

/**
 * Class for executing agent strategies.
 * 
 * @author ewah
 */
public class AgentStrategy extends Activity {

	protected final Agent agent;

	public AgentStrategy(Agent agent, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public Iterable<? extends Activity> execute(TimeStamp currentTime) {
		return agent.agentStrategy(currentTime);
	}

	@Override
	public String toString() {
		return super.toString() + agent;
	}

}
