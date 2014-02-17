package systemmanager;

import java.util.Random;

import event.TimeStamp;
import event.TimedActivity;

/**
 * Class that provides the ability to "simulate" an even manager. This
 * guarantees that residual effects of actions get propagated (such as
 * information spreading), as well as the ability to set the time
 * 
 * @author erik
 * 
 */
public class Executor extends Scheduler {

	private static final Random rand = new Random();
	private static final TimeStamp one = TimeStamp.create(1);
	
	public Executor() {
		super(rand);
	}

	/**
	 * Also sets the time to be time - 1
	 */
	@Override
	public void executeUntil(TimeStamp time) {
		super.executeUntil(time);
		this.currentTime = time.minus(one);
	}
	
	public TimedActivity peek() {
		return eventQueue.peek();
	}
	
	public boolean isEmpty() {
		return eventQueue.isEmpty();
	}

}
