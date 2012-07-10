package systemmanager;

import event.*;
import entity.*;
import activity.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

import market.*;

/**
 * This class serves the purpose of the Client in the Command pattern, 
 * in that it instantiates the Activity objects and provides the methods
 * to execute them later.
 * 
 * @author ewah
 */
public class SystemManager {

	private static HashMap<Integer,Entity> entities; 	// Entities hashed by their ID
	private static EventManager eventManager;
	private static SystemData data;
	private static Sequence agentIDSequence;
	private static Sequence marketIDSequence;
	private static TimeStamp duration;

	// Environment parameters (set by config file)
	private static int numAgents = 5;
	private static int numMarkets = 2;

	public SystemManager() {
		// empty constructor
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// this will be set by the config file
		duration = new TimeStamp(50); // hardcoded TODO
		
		initialize();
		runSystemManager();

//		if (args.length != 1) {
//			System.out.println("Usage: java -jar mss.jar <config_file>");
//			System.exit(1);
//		}
//		runSystemManager(args[0]);
	}


	/**
	 * Initialize events and entities based on config file. 
	 * Adds agent arrival events to event queue.
	 */
	public static void initialize() {
		System.out.println("Initialization phase.");

		data = new SystemData();
		data.gameLength = duration;
		eventManager = new EventManager(duration);
		entities = new HashMap<Integer,Entity>();
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);

		// Dynamically create entities
		String[] agentTypes = {"ZI", "ZI", "ZI", "ZI", "ZI"};
		for (int i = 0; i < numMarkets; i++) {
			int marketID = marketIDSequence.decrement();
			entities.put(marketID, MarketFactory.createMarket("CDA",marketID,data));
			data.addMarket((Market) entities.get(marketID));
		}
		for (int i = 0; i < numAgents; i++) {
			int agentID = agentIDSequence.increment();
			entities.put(agentID, AgentFactory.createAgent(agentTypes[i], agentID, data));
			data.addAgent((Agent) entities.get(agentID));
		}

		// Initialize arrival times
		eventManager.createEvent(new AgentArrival(data.getAgent(1), data.getMarket(-1), new TimeStamp(1)));
		eventManager.createEvent(new AgentArrival(data.getAgent(2), data.getMarket(-2), new TimeStamp(2)));
		eventManager.createEvent(new AgentArrival(data.getAgent(3), data.getMarket(-1), new TimeStamp(3)));
		eventManager.createEvent(new AgentArrival(data.getAgent(4), data.getMarket(-2), new TimeStamp(4)));
//		ArrayList<Integer> mkts = new ArrayList<Integer>(data.markets.keySet());
//		for (Iterator<Integer> i = mkts.iterator(); i.hasNext(); ) {
//			eventManager.createEvent(new AgentArrival(data.getAgent(5), data.getMarket(i.next()), new TimeStamp(5)));
//		}

		// Agent departure times
		eventManager.createEvent(new AgentDeparture(data.getAgent(1), data.getMarket(-1), data.gameLength));
		eventManager.createEvent(new AgentDeparture(data.getAgent(2), data.getMarket(-2), data.gameLength));
		eventManager.createEvent(new AgentDeparture(data.getAgent(3), data.getMarket(-1), data.gameLength));
		eventManager.createEvent(new AgentDeparture(data.getAgent(4), data.getMarket(-2), data.gameLength));
//		eventManager.createEvent(new AgentDeparture(data.getAgent(5), data.getMarket(-1), data.gameLength));
//		eventManager.createEvent(new AgentDeparture(data.getAgent(5), data.getMarket(-2), data.gameLength));
	}

	public static void runSystemManager() {

		System.out.println("Running...");

		while (!eventManager.isEventQueueEmpty()) {
			eventManager.executeCurrentEvent();
		}
		System.out.println("Event queue is now empty!");
	}


}
