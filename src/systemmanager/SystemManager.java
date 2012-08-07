package systemmanager;

import event.*;
import entity.*;
import activity.*;

import java.util.*;
import java.io.*;
import java.text.DateFormat;

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

	// Environment parameters
	private Properties envProps;
	private AgentProperties params;
	
	private Log log;

	
	/**
	 * Constructor
	 */
	public SystemManager() {
		data = new SystemData();
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
		envProps = new Properties();
		params = new AgentProperties();	
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
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		
		while (!eventManager.isEventQueueEmpty()) {
			eventManager.executeCurrentEvent();
		}
		String s = "STATUS: Event queue is now empty.";
		log.log(Log.DEBUG, s);
		System.out.println(s);
	}
	
	
	/**
	 * Initialize parameters based on configuration file.
	 * 
	 * @param configFile
	 */
	public void setup(String configFile) {
		
		// Read environment parameters
		loadConfig(envProps, configFile);
		data.simLength = new TimeStamp(Long.parseLong(envProps.getProperty("simLength")));
		data.nbboLatency = new TimeStamp(Long.parseLong(envProps.getProperty("nbboLatency")));
		data.tickSize = Integer.parseInt(envProps.getProperty("tickSize"));
		
		// Create log file
		String logDir = envProps.getProperty("logDir");
		int logLevel = Integer.parseInt(envProps.getProperty("logLevel"));
		Date now = new Date();
		String logFilename = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(now);
		logFilename = logFilename.replace("/", "-");
		logFilename = logFilename.replace(" ", "_");
		logFilename = logFilename.replace(":", "");
		try {
			log = new Log(logLevel, ".", logDir + logFilename + ".txt", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Log properties
		log.log(Log.DEBUG, envProps.toString());
		log.log(Log.INFO, params.toString());
		
		// Create event manager
		eventManager = new EventManager(data.simLength, log);
		
		// Check which types of agents to create
		for (int i = 0; i < SystemConsts.agentTypes.length; i++) {
			String num = envProps.getProperty(SystemConsts.agentTypes[i]);
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numAgents += n;
				data.numAgentType.put(SystemConsts.agentTypes[i], n);
			}
		}
		// Check which types of markets to create
		for (int i = 0; i < SystemConsts.marketTypes.length; i++) {
			String num = envProps.getProperty(SystemConsts.marketTypes[i]);
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numMarkets += n;
				data.numMarketType.put(SystemConsts.marketTypes[i], n);
			}
		}
		
		// Create Quoter entity, which enters the system at time 0
		Quoter iu = new Quoter(0, data, log);
		data.quoter = iu;
		eventManager.createEvent(new UpdateNBBO(iu, new TimeStamp(0)));
		
		// Create markets first since agent creation references markets
		for (Map.Entry<String, Integer> mkt: data.numMarketType.entrySet()) {
			for (int i = 0; i < mkt.getValue(); i++) {
				int marketID = marketIDSequence.decrement();
				Market market = MarketFactory.createMarket(mkt.getKey(), marketID, data, log);
				data.addMarket(market);
			}
			log.log(Log.INFO, "Markets: " + mkt.getValue() + " " + mkt.getKey());
		}
		
		// Create agents, initialize parameters, and compute arrival times (if needed)
		for (Map.Entry<String, Integer> ag : data.numAgentType.entrySet()) {
			setupSystemParams(ag.getKey());
			
			for (int i = 0; i < ag.getValue(); i++) {
				setupAgent(agentIDSequence.increment(), ag.getKey());
			}
			log.log(Log.INFO, "Agents: " + ag.getValue() + " " + ag.getKey());
		}
		
		// Log agent information
		logAgentInfo();
	}
	
	/**
	 * Method for loading any needed system parameters from SystemProperties file.
	 * 
	 * @param key
	 */
	private void setupSystemParams(String key) {
		if (key.equals("NBBO")) {
			// set up arrival times/private values generator
			HashMap<String,String> nbbo = params.get(key);
			data.nbboArrivalTimes(Double.parseDouble(nbbo.get("arrivalRate")));
			data.nbboPrivateValues(Double.parseDouble(nbbo.get("kappa")),
					Integer.parseInt(nbbo.get("meanPV")),
					Double.parseDouble(nbbo.get("shockVar")));	
		}
	}
	
	
	/**
	 * Creates agent and initializes all agent settings/parameters.
	 * Inserts AgentArrival/Departure activities into the eventQueue.
	 * 
	 * @param agentID
	 * @param agentType
	 */
	public void setupAgent(int agentID, String agentType) {
		Agent agent = AgentFactory.createAgent(agentType, agentID, data, params, log);
		data.addAgent(agent);
		
		TimeStamp ts = agent.getArrivalTime();
		Activity arrival = null;
		Activity departure = null;
		
		// Agent is in single market
		if (data.getAgent(agentID) instanceof SMAgent) {
			for (int i = 1; i <= data.numMarkets; i++) {
				arrival = new AgentArrival(data.getAgent(agentID), data.getMarket(-i), ts);
				departure = new AgentDeparture(data.getAgent(agentID), data.getMarket(-i), data.simLength);
				eventManager.createEvent(arrival);
				eventManager.createEvent(departure);
			}
			
		} else if (data.getAgent(agentID) instanceof MMAgent) {
			// Agent is in multiple markets
			arrival = new AgentArrival(data.getAgent(agentID), ts);
			departure = new AgentDeparture(data.getAgent(agentID), data.simLength);
			eventManager.createEvent(arrival);
			eventManager.createEvent(departure);
		}

		// Check if agent is infinitely fast and updates event manager if needed
		if (Integer.parseInt(params.get(agentType).get("sleepTime")) == 0) {
			eventManager.setInfiniteFastActs(data.getAgent(agentID).getInfinitelyFastActs());
		} 
	}
	
	
	/**
	 * Logs agent information.
	 */
	public void logAgentInfo() {
		for (Map.Entry<Integer,Agent> entry : data.agents.entrySet()) {
			Agent ag = entry.getValue();
			
			// print arrival times
			String s = ag.toString() + "::" + ag.getType() + "::";
			s += "arrivalTime=" + ag.getArrivalTime().toString();
			
			// print private value if exists 
			if (ag.getType().equals("NBBO")) {
				s += ", pv=" + ((NBBOAgent) ag).privateValue;
			}
			log.log(Log.INFO, s);
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
			p.load(config);
		} catch (IOException e) {
			log.log(Log.INFO, "loadConfig(InputStream): error opening/processing config file: " + config + "/" + e);
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
			log.log(Log.INFO, "loadConfig(String): error opening/processing config file: " + config + "/" + e);
			System.exit(0);
		}
	}
	
}
