package systemmanager;

import java.util.Random;

import event.TimeStamp;

import activity.Activity;

/**
 * Class that provides the ability to "simulate" an even manager. This
 * guarantees that residual effects of actions get propagated (such as
 * information spreading)
 * 
 * @author erik
 * 
 */
public abstract class Executer extends EventManager {

	private static final Random rand = new Random();
	
	private Executer() {
		super(rand);
	}
	
	public static void execute(Iterable<? extends Activity> acts) {
		EventManager m = new EventManager(rand);
		m.eventQueue.addAll(acts);
		m.executeUntil(new TimeStamp(Integer.MAX_VALUE));
	}
	
	public static void executeImmediate(Iterable<? extends Activity> acts) {
		EventManager m = new EventManager(rand);
		m.eventQueue.addAll(acts);
		m.executeUntil(TimeStamp.ZERO);
	}

}
