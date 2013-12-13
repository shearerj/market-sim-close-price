package systemmanager;

import java.util.Random;

import event.TimeStamp;

import activity.Activity;

public class MockEventManager extends EventManager {

	public MockEventManager() {
		super(new Random());
	}
	
	public static void executeAll(Iterable<? extends Activity> acts) {
		MockEventManager m = new MockEventManager();
		m.eventQueue.addAll(acts);
		m.executeUntil(new TimeStamp(Integer.MAX_VALUE));
	}

}
