package activity;

import java.util.Collection;
import org.apache.commons.lang3.builder.*;

import entity.agent.*;
import event.*;

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
	public int hashCode() {
		return new HashCodeBuilder(19, 37).append(agent).append(super.hashCode()).toHashCode();
	}

	@Override
	public String toString() {
		return getName() + " :: " + agent;
	}

}
