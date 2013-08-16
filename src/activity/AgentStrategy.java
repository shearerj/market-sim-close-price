package activity;

import java.util.Collection;

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
		this.agent = agent;
	}

	@Override
	public Collection<? extends Activity> execute(TimeStamp currentTime) {
		return agent.agentStrategy(currentTime);
	}

	@Override
	public String toString() {
		return super.toString() + agent;
	}

}
