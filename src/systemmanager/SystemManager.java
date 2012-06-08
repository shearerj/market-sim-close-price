package systemmanager;

import event.*;
import entity.*;
import activity.*;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * 
 * This class serves the purpose of the Client in the Command pattern, 
 * in that it instantiates the Activity objects and provides the methods
 * to execute them later.
 * 
 * @author ewah
 */
public class SystemManager {

	private static EventManager eventManager;
	private static HashMap<Integer,Entity> entities; 	// Entities hashed by their ID
	
	public SystemManager() {
		// empty constructor
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		runSystemManager();
		
		if (args.length != 1) {
			System.out.println("Usage: java -jar mss.jar <config_file>");
//			System.exit(1);
		}
//		runSystemManager(args[0]);
	}
	
	
	/**
	 * Initialize events and entities based on config file.
	 */
	public static void initialize() {
		
		// how to dynamically assign agentIDs?
		ZIAgent z1 = new ZIAgent(1);
		ZIAgent z2 = new ZIAgent(2);
		entities.put(1,z1);
		entities.put(2,z2);
		
		// create activities
		Bid b = new Bid(0);
		Bid b2 = new Bid(10);
		Activity subBid1 = new SubmitBid(z1, b);
		Activity subBid2 = new SubmitBid(z2, b2);
		Activity act1 = new AgentStrategy(z2);
		
		// create event
		TimeStamp t = new TimeStamp(20);
		LinkedList<Activity> actToAdd = new LinkedList<Activity>();
		actToAdd.add(subBid1);
		actToAdd.add(subBid2);
		Event e_test = new Event(t,actToAdd);
		e_test.storeActivity(act1);
		
		// add event to Event Queue
		eventManager.addEvent(e_test);
		System.out.println("Event with time " + e_test.getTime().toString() + " added!");
	}
	

	public static void runSystemManager() {
		System.out.println("Running...");
		
		eventManager = new EventManager(); 		// create new event manager
		entities = new HashMap<Integer,Entity>();
		
		initialize();
		
		// now check if the event was added correctly
		TimeStamp t2 = eventManager.getCurrentTime();
		System.out.println("time of event is: " + t2.toString());
		
		while (!eventManager.isEventQueueEmpty()) {
			// execute the event now!
			eventManager.executeCurrentEvent();
			
		}
		System.out.println("event queue is now empty!");
		
		
		// to manage events, it has to maintain what the "current time/priority is" based on the current event being executed
		// process of handling events
		// 0) initialize events based on configuration
		// 1) check if event queue is empty
		// 2a) if empty, then don't do anything
		// 2b) if still stuff inside, then get next event
		// 3) execute event and wait to finish
		// 4) when done, remove from the queue and update the current priority
		
		
		// need to also figure out how to dynamically initialize Entities
	}
	
}
