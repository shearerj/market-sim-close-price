package systemmanager;

import event.*;
import entity.*;
import activity.*;
import activity.market.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.io.IOException;

/**
 * This class serves the purpose of the Client in the Command pattern, 
 * in that it instantiates the Activity objects and provides the methods
 * to execute them later.
 * 
 * @author ewah
 */
public class SystemManager {

	private static EventManager eventManager;
	private static HashMap<Integer,Entity> entities; 	// Entities hashed by their ID
	
	private static SystemData data;
	private static Sequence agentIDSequence;
	private static Sequence marketIDSequence;
	
	// Environment parameters
	private static int numAgents = 5;
	private static int numMarkets = 2;
	
	public SystemManager() {
		// empty constructor
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int gameID = 1;
		
		initialize();
		runSystemManager();
		
		if (args.length != 1) {
			System.out.println("Usage: java -jar mss.jar <config_file>");
//			System.exit(1);
		}
//		runSystemManager(args[0]);
	}
	
	
	/**
	 * Initialize events and entities based on config file. 
	 * Adds agent arrival events to event queue.
	 */
	public static void initialize() {
		System.out.println("Initialization phase.");
		
		eventManager = new EventManager();
		entities = new HashMap<Integer,Entity>();
		data = new SystemData();
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
		
		// set by config file
//		numAgents = 5;
//		numMarkets = 2;

		// Dynamically create entities
		for (int i = 0; i < numMarkets; i++) {
			int marketID = marketIDSequence.decrement();
			entities.put(marketID, MarketFactory.createMarket("CDA",marketID,data));
			data.addMarket((Market) entities.get(marketID));
		}
		for (int i = 0; i < numAgents; i++) {
			int agentID = agentIDSequence.increment();
			entities.put(agentID, AgentFactory.createAgent("ZI",agentID,data));
			data.addAgent((Agent) entities.get(agentID));
		}
		
		// Initialize arrival times
		Activity arr1 = new AgentArrival(data.getAgent(1), data.getMarket(-1), new TimeStamp(1));
		Activity arr2 = new AgentArrival(data.getAgent(2), data.getMarket(-2), new TimeStamp(2));
		Activity arr3 = new AgentArrival((Agent) entities.get(3), (Market) entities.get(-1), new TimeStamp(3));
		Activity arr4 = new AgentArrival((Agent) entities.get(4), (Market) entities.get(-2), new TimeStamp(4));
		Activity arr5 = new AgentArrival((Agent) entities.get(5), (Market) entities.get(-1), new TimeStamp(5));
		
		eventManager.createEvent(arr1);
		eventManager.createEvent(arr2);
		eventManager.createEvent(arr3);
		eventManager.createEvent(arr4);
		eventManager.createEvent(arr5);
		
	}

	public static void runSystemManager() {
		
		System.out.println("Running...");
//		Activity act1 = new AgentStrategy((Agent) entities.get(2), new TimeStamp(20));
		// create event
//		Event e = createEmptyEvent(new TimeStamp(20));
//		e.addActivity(subBid1);
//		e.addActivity(subBid2);
//		e.addActivity(act1);
		// add event to Event Queue
//		eventManager.addEvent(e);
//		System.out.println("Event with time " + e.getTime().toString() + " added!");
		
		while (!eventManager.isEventQueueEmpty()) {
			eventManager.executeCurrentEvent();
		}
		System.out.println("Event queue is now empty!");
	}

	
//	/**
//	 * Creates a new Event with an empty list of Activities. TESTING ONLY
//	 */
//	public static Event createEmptyEvent(TimeStamp t) {
//		LinkedList<Activity> activities = new LinkedList<Activity>();
//		Event e = new Event(t, activities);
//		return e;
//	}
	
	
}
