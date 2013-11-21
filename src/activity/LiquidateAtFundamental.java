package activity;

import static com.google.common.base.Preconditions.checkNotNull;

import entity.agent.Agent;
import event.TimeStamp;

/**
 * Class for Activity to liquidate an agent's position, based on some given price.
 * This price may be based on the value of the global fundamental.
 * 
 * @author ewah
 */
public class LiquidateAtFundamental extends Activity {

	protected final Agent agent;

	public LiquidateAtFundamental(Agent agent, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public Iterable<? extends Activity> execute(TimeStamp currentTime) {
		return this.agent.liquidateAtFundamental(currentTime);
	}
	
	@Override
	public String toString() {
		return super.toString() + agent;
	}
	
}
