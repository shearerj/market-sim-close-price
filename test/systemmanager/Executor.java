package systemmanager;

import java.util.Random;

import event.TimeStamp;
import event.TimedActivity;

/**
 * Class that provides the ability to "simulate" an even manager. This
 * guarantees that residual effects of actions get propagated (such as
 * information spreading)
 * 
 * @author erik
 * 
 */
public class Executor extends Scheduler {

	private static final Random rand = new Random();
	
	public Executor() {
		super(rand);
	}

	@Override
	public void executeUntil(TimeStamp time) {
		super.executeUntil(time);
	}
	
	public void setTime(TimeStamp time) {
		this.currentTime = time;
	}
	
	public TimedActivity peek() {
		return eventQueue.peek();
	}
	
	public boolean isEmpty() {
		return eventQueue.isEmpty();
	}

}
