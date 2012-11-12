package systemmanager;

import event.*;
import entity.*;
import activity.*;
import models.*;

import java.util.*;
import java.io.*;
import java.text.DateFormat;

/**
 * This class serves the purpose of the Client in the Command pattern, 
 * in that it instantiates the Activity objects and provides the methods
 * to execute them later.
 * 
 * Usage: java -jar hft.jar [simulation folder name] [sample #]
 * 
 * @author ewah
 */
public class SystemManager {

	private EventManager eventManager;
	private SystemData data;
	private Observations obs;
	
	private static int num;						// sample number used for labeling output files
	private static String simFolder;			// simulation folder name
	
	private Properties envProps;
	private Log log;
	private int logLevel;
	private String logFilename;

	/**
	 * Constructor
	 */
	public SystemManager() {
		data = new SystemData();
		envProps = new Properties();
		obs = new Observations(data);
	}
	
	/**
	 * Only one argument, which is the sample number, is processed
	 * 
	 * Two input arguments: first is simulation folder, second is sample number
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		SystemManager manager = new SystemManager();

		if (args.length == 2) {
			simFolder = args[0] + "/";
			num = Integer.parseInt(args[1]);
		} else {
			simFolder = "";
			num = 1;
		}
		
		manager.setup();
		manager.executeEvents();
		manager.aggregateResults();
		manager.close();
	}
	
	
	/**
	 * Method to execute all events in the Event Queue.
	 */
	public void executeEvents() {
		try {
			while (!eventManager.isEventQueueEmpty()) {
				eventManager.executeCurrentEvent();
			}
			String s = "STATUS: Event queue is now empty.";
			log.log(Log.INFO, s);
		} catch (Exception e) {
			System.err.print(e);
		}
	}

	/**
	 * Shuts down simulation. Removes empty log file if log level is 0.
	 */
	public void close() {
		File f = new File(simFolder + Consts.logDir);
		if (f.exists() && logLevel == 0) {
			// remove the empty log file
			f.delete();
		}
	}
	
	/**
	 * Initialize parameters based on configuration file.
	 */
	public void setup() {

		try {
			// =================================================
			// Load parameters

			// Read environment parameters & set up environment
			loadConfig(envProps, Consts.configDir + Consts.configFile);
			data.readEnvProps(envProps);
			data.obsNum = num;

			// Create log file
			logLevel = Integer.parseInt(envProps.getProperty("logLevel"));
			Date now = new Date();
			logFilename = simFolder + num;
			logFilename += "_" + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(now);
			logFilename = logFilename.replace("/", "-");
			logFilename = logFilename.replace(" ", "_");
			logFilename = logFilename.replace(":","");
			
			try {
				// Check first if directory exists
				File f = new File(simFolder + Consts.logDir);
				if (!f.exists()) {
					// Create directory
					new File(simFolder + Consts.logDir).mkdir();
				}
				log = new Log(logLevel, ".", simFolder + Consts.logDir + logFilename + ".txt", true);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.print("setup(String): error creating log file");
			}

			// Log properties
			log.log(Log.DEBUG, envProps.toString());
			
			// Read simulation specification file
			SimulationSpec specs = new SimulationSpec(simFolder + Consts.simSpecFile, log, data);

			// Create event manager
			eventManager = new EventManager(data.simLength, log);

			// Set up / create entities
			SystemSetup s = new SystemSetup(specs, eventManager, data, log);
			s.setupAll();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
//	/**
//<<<<<<< HEAD
//=======
//	 * Creates market and initializes any Activities as necessary. For example,
//	 * for Call Markets, this method inserts the initial Clear activity into the 
//	 * eventQueue.
//	 * 
//	 * @param marketID
//	 * @param marketType
//	 */
//	public void setupMarket(int marketID, String marketType) {
//		
//		Market market;
//		if (marketType.startsWith(Consts.CENTRAL)) {
//			market = MarketFactory.createMarket(marketType.substring(Consts.CENTRAL.length()+1), 
//					marketID, data, log);
//			data.centralMarkets.put(marketID, market);
//		} else {
//			// Only add market to the general list if it's not the central market
//			market = MarketFactory.createMarket(marketType, marketID, data, log);
//			data.addMarket(market);
//		}
//		
//		// Check if is call market, then initialize clearing sequence
//		if (market instanceof CallMarket) {
//			Activity clear = new Clear(market, market.getNextClearTime());
//			eventManager.createEvent(Consts.CALL_CLEAR_PRIORITY, clear);
//		}
//	}
//	
//
//	/**
//	 * Creates agent and initializes all agent settings/parameters.
//	 * Inserts AgentArrival/Departure activities into the eventQueue.
//	 * 
//	 * @param agentID
//	 * @param agentType
//	 * @param ap AgentProperties object
//	 */
//	public void setupAgent(int agentID, String agentType, AgentProperties ap) {
//		
//		if (!Arrays.asList(Consts.SMAgentTypes).contains(agentType)) {
//			// Multimarket agent
//			Agent agent = AgentFactory.createMMAgent(agentType, agentID, data, ap, log);
//			data.addAgent(agent);
//			log.log(Log.DEBUG, agent.toString() + ": " + ap);
//			
//			TimeStamp ts = agent.getArrivalTime();
//			if (agent instanceof MMAgent) {
//				// Agent is in multiple markets
//				eventManager.createEvent(new AgentArrival(agent, ts));
//				eventManager.createEvent(new AgentDeparture(agent, data.simLength));
//			}
//			
//		} else {
//			// Single market agent - create for each market
//			int n = 0;
//			for (Iterator<Integer> it = data.getMarketIDs().iterator(); it.hasNext(); ) {
//				
//				// Increment agent ID to create after first agent created
//				int id;
//				if (n == 0) {
//					id = agentID;
//				} else {
//					id = agentIDSequence.increment();
//				}
//				
//				int mktID = it.next();
//				Agent agent = AgentFactory.createSMAgent(agentType, id, data, ap, log, mktID);
//				data.addAgent(agent);
//				log.log(Log.DEBUG, agent.toString() + ": " + ap);
//				
//				TimeStamp ts = agent.getArrivalTime();
//				if (agent instanceof SMAgent) {
//					// Agent is in single market
//					Market mkt = ((SMAgent) agent).getMainMarket();
//					eventManager.createEvent(new AgentArrival(agent, mkt, ts));
//					eventManager.createEvent(new AgentDeparture(agent, mkt, data.simLength));		
//				}
//				
//				n++;
//			}
//		}
//		
////		TimeStamp ts = agent.getArrivalTime();
////		if (agent instanceof SMAgent) {
////			// Agent is in single market
////			Market mkt = ((SMAgent) agent).getMainMarket();
////			eventManager.createEvent(new AgentArrival(agent, mkt, ts));
////			eventManager.createEvent(new AgentDeparture(agent, mkt, data.simLength));
////			
////		} else if (agent instanceof MMAgent) {
////			// Agent is in multiple markets
////			eventManager.createEvent(new AgentArrival(agent, ts));
////			eventManager.createEvent(new AgentDeparture(agent, data.simLength));
////		}
//	}
//	
//	
//	/**
//	 * Logs agent information.
//	 */
//	public void logAgentInfo() {
//		for (Map.Entry<Integer,Agent> entry : data.agents.entrySet()) {
//			Agent ag = entry.getValue();
//			
//			// print arrival times
//			String s = ag.toString() + "::" + ag.getType() + "::";
//			s += "arrivalTime=" + ag.getArrivalTime().toString();
//			
//			// print private value if exists 
//			if (ag instanceof ZIAgent) {
//				s += ", pv=" + ((ZIAgent) ag).getPrivateValue();
//			}
//			log.log(Log.INFO, s);
//		}
//	}
//	
	
	/**
	 * Load a configuration file InputStream into a Properties object.
	 * @param p
	 * @param config
	 */
	public void loadInputStream(Properties p, InputStream config) {
		try {
			if (p == null)
				return;
			p.load(config);
		} catch (IOException e) {
			String s = "loadConfig(InputStream): error opening/processing config file: " + config + "/" + e;
			log.log(Log.ERROR, s);
			System.err.print(s);
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
			loadInputStream(p, new FileInputStream(config));
		} catch (FileNotFoundException e) {
			String s = "loadConfig(String): error opening/processing config file: " + config + "/" + e;
			log.log(Log.ERROR, s);
			System.err.print(s);
			System.exit(0);
		}
	}
	
	
	/**
	 * Generate results report (payoff data, feature data logging). Iterators through all agents
	 * and adds observation for each one.
	 */
	public void aggregateResults() {
		try {
			for (Iterator<Integer> it = data.getAgents().keySet().iterator(); it.hasNext(); ) {
				int id = it.next();
				obs.addObservation(id);
			}
			
			obs.addFeature("interval", obs.getTimeStampFeatures(data.getIntervals()));
			obs.addFeature("pv", obs.getPriceFeatures(data.getPrivateValues()));
			obs.addFeature("expire", obs.getTimeStampFeatures(data.getExpirations()));
			obs.addFeature("bkgrd_info", obs.getBackgroundInfo(data.getAgents()));
			getMarketResults();
			obs.addFeature("", obs.getConfiguration());
			
			File file = new File(simFolder + Consts.obsFilename + num + ".json");
			FileWriter txt = new FileWriter(file);
			txt.write(obs.generateObservationFile());
			txt.close();
			
		} catch (Exception e) {
			String s = "aggregateResults(): error creating observation file";
			e.printStackTrace();
			System.err.print(s);
		}
	}
	
	/**
	 * Gets market results by model
	 */
	private void getMarketResults() {
		// TODO create the prefix
		for (Map.Entry<Integer, MarketModel> entry : data.getModels().entrySet()) {
			MarketModel model = entry.getValue();
			ArrayList<Integer> ids = model.getMarketIDs();
			
			String prefix = model.getClass().getSimpleName().toLowerCase() + "_";
			obs.addFeature(prefix + "bkgrd_surplus", obs.getSurplusFeatures(data.getSurplus(ids), false));
			obs.addFeature("depths", obs.getDepthInfo(ids));
			obs.addFeature("transactions", obs.getTransactionInfo(ids));
			obs.addFeature("spreads", obs.getSpreadInfo(ids));
			obs.addFeature("exec_speed", obs.getExecutionSpeed(ids));
		}
	}
	
//	/**
//	 * Gets central market results or results for all markets (excluding centralized).
//	 * @param central true if central market
//	 * @param prefix string to add to key name
//	 */
//	private void getMarketResults(boolean central, String prefix) {
//		if (prefix != null && prefix != "") {
//			prefix = prefix + "_";
//		}
//		
//		if (central) {
//			ArrayList<Integer> ids = data.getCentralMarketIDs();
//			for (Iterator<Integer> i = ids.iterator(); i.hasNext(); ) {
//				ArrayList<Integer> id = new ArrayList<Integer>();
//				int mktID = i.next();
//				id.add(mktID);
//				obs.addFeature(prefix + data.getCentralMarketType(mktID).toLowerCase() + 
//						"_bkgrd_surplus", obs.getSurplusFeatures(data.getSurplus(id), true));
//			}
//		} else {
//			ArrayList<Integer> ids = data.getMarketIDs();
//			obs.addFeature(prefix + "bkgrd_surplus", obs.getSurplusFeatures(data.getSurplus(ids), false));
//		}
//		obs.addFeature(prefix + "transactions", obs.getTransactionInfo(central));
//		obs.addFeature(prefix + "depths", obs.getDepthInfo(central));
//		obs.addFeature(prefix + "spreads", obs.getSpreadInfo(central));
//		obs.addFeature(prefix + "exec_speed", obs.getExecutionSpeed(central));
//	}
	
	
}
