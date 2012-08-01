package systemmanager;

import event.*;
import entity.*;
import activity.*;

import java.util.*;
import java.io.*;

/**
 * This class serves the purpose of the Client in the Command pattern, 
 * in that it instantiates the Activity objects and provides the methods
 * to execute them later.
 * 
 * @author ewah
 */
public class SystemManager {

	private HashMap<Integer,Entity> entities; 	// Entities hashed by their ID
	private EventManager eventManager;
	private SystemData data;
	private Sequence agentIDSequence;
	private Sequence marketIDSequence;
	
	// Environment parameters (set by config file)
	private Properties props;
	private SystemProperties params;

	/**
	 * Constructor
	 */
	public SystemManager() {
		data = new SystemData();
		entities = new HashMap<Integer,Entity>();
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
		props = new Properties();
		params = new SystemProperties();
	}
	
	public static void main(String[] args) {
		
		SystemManager manager = new SystemManager();

		if (args.length == 1) {
			manager.setup(args[0]);
		} else {
			manager.setup("config/env.properties");
		}
		manager.executeEvents();
	}
	
	
	/**
	 * Initialize environment parameters based on configuration file.
	 * @param configFile
	 */
	public void setup(String configFile) {
		
		loadConfig(props, configFile);	
		data.simLength = new TimeStamp(Long.parseLong(props.getProperty("simLength")));
		eventManager = new EventManager(data.simLength);
		
		// Check which types of agents to create
		for (int i = 0; i < SystemConsts.agentTypes.length; i++) {
			String num = props.getProperty(SystemConsts.agentTypes[i]);
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numAgents += n;
				data.numAgentType.put(SystemConsts.agentTypes[i], n);
			}
		}
		// Check which types of markets to create
		for (int i = 0; i < SystemConsts.marketTypes.length; i++) {
			String num = props.getProperty(SystemConsts.marketTypes[i]);
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numMarkets += n;
				data.numMarketType.put(SystemConsts.marketTypes[i], n);
			}
		}
		
		// Create markets first since agent creation references markets
		for (Map.Entry<String, Integer> mkt: data.numMarketType.entrySet()) {
			for (int i = 0; i < mkt.getValue(); i++) {
				int marketID = marketIDSequence.decrement();
				entities.put(marketID, MarketFactory.createMarket(mkt.getKey(), marketID, data));
				data.addMarket((Market) entities.get(marketID));
			}
		}
		
		// Create agents, initialize parameters, and compute arrival times (if needed)
		for (Map.Entry<String, Integer> ag : data.numAgentType.entrySet()) {
			for (int i = 0; i < ag.getValue(); i++) {
				int agentID = agentIDSequence.increment();
				
				entities.put(agentID, AgentFactory.createAgent(ag.getKey(), agentID, data));
				Agent agent = (Agent) entities.get(agentID);
				data.addAgent(agent);
				agent.initializeParams(params);
				createAgentActivity(agentID, agent.nextArrivalTime());
			}
		}
	}
	
	
	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		System.out.println("Running...");
		while (!eventManager.isEventQueueEmpty()) {
			eventManager.executeCurrentEvent();
		}
		System.out.println("Event queue is now empty!"); // TODO - should be log
	}
	
	/**
	 * Inserts AgentArrival/Departure activities into the eventQueue.
	 * @param agentID
	 * @param marketIDs
	 * @param ts	arrival time
	 */
	public void createAgentActivity(int agentID, TimeStamp ts) {
		for (int i = 1; i <= data.numMarkets; i++) {
			eventManager.createEvent(new AgentArrival(data.getAgent(agentID),
									 data.getMarket(-i), ts));
			eventManager.createEvent(new AgentDeparture(data.getAgent(agentID),
									 data.getMarket(-i), data.simLength));
		}
	}

	/**
	 * Load a configuration file InputStream into a Properties object.
	 * @param p
	 * @param config
	 */
	public void loadConfig(Properties p, InputStream config) {
		try {
			if (p == null)
				return;
//			p = new Properties(); // object passed as reference by value!
			p.load(config);
		} catch (IOException e) {
			System.out.println("loadConfig(InputStream): error opening/processing config file: " + config + "/" + e);
			System.exit(0);
		}
	}

	/**
	 * Load a configuration file into a Properties object.
	 * @param p
	 * @param config	name of configuration file
	 */
	public void loadConfig(Properties p, String config) {
		try {
			loadConfig(p, new FileInputStream(config));
		} catch (FileNotFoundException e) {
			System.out.println("loadConfig(String): error opening/processing config file: " + config + "/" + e);
			System.exit(0);
		}
	}

}
