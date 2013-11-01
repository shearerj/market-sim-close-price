package activity;

import static com.google.common.base.Preconditions.checkNotNull;
import entity.agent.MockBackgroundAgent;
import event.TimeStamp;

public class MockAgentActivity extends Activity {

	protected final MockBackgroundAgent agent;
	
	public MockAgentActivity(MockBackgroundAgent agent, TimeStamp scheduledTime) {
		super(scheduledTime);
		this.agent = checkNotNull(agent, "Agent");
	}

	@Override
	public Iterable<? extends Activity> execute(TimeStamp currentTime) {
		return this.agent.addMockActivity(currentTime);
	}
}
