package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.MockAgent;
import event.TimeStamp;

public class MockAgentActivity extends Activity {

	protected final MockAgent agent;
	
	public MockAgentActivity(MockAgent agent, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public Iterable<? extends Activity> execute(TimeStamp currentTime) {
		return this.agent.addMockActivity(currentTime);
	}
}
