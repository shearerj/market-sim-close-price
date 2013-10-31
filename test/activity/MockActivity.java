package activity;

import com.google.common.collect.ImmutableList;

import event.TimeStamp;

public class MockActivity extends Activity {

	public MockActivity(long scheduledTime) {
		this(new TimeStamp(scheduledTime));
	}
	
	public MockActivity(TimeStamp scheduledTime) {
		super(scheduledTime);
	}
	
	@Override
	public Iterable<? extends Activity> execute(TimeStamp currentTime) {
		return ImmutableList.of();
	}

}
