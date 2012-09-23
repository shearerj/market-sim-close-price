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
 * First input argument is folder name, second is sample #.
 * 
 * @author ewah
 */
public class SystemManager {

	private EventManager eventManager;
	private SystemData data;
	private Sequence agentIDSequence;
	private Sequence marketIDSequence;
	private Observations results;
	
	private static int num;						// sample number used for labeling output files
	private static String simFolder;			// simulation folder name
	
	private Properties envProps;
	private Log log;

	/**
	 * Constructor
	 */
	public SystemManager() {
		data = new SystemData();
		agentIDSequence = new Sequence(1);
		marketIDSequence = new Sequence(-1);
		envProps = new Properties();
		results = new Observations(data);
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
	 * Initialize parameters based on configuration file.
	 */
	public void setup() {

		try {
			// =================================================
			// Load parameters

			// Read environment parameters & set up environment
			loadConfig(envProps, Consts.configDir + Consts.configFile);
			data.readEnvProps(envProps);

			// Create log file
			int logLevel = Integer.parseInt(envProps.getProperty("logLevel"));
			Date now = new Date();
			String logFilename = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(now);
			logFilename = logFilename.replace("/", "-");
			logFilename = logFilename.replace(" ", "_");
			logFilename = logFilename.replace(":", "");
			
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
			// Read simulation spec file
			SimulationSpec specs = new SimulationSpec(simFolder + Consts.simSpecFile, log, data);
			specs.setParams();

			data.backgroundArrivalTimes();
			data.backgroundPrivateValues();
			

			// =================================================
			// Create entities

			eventManager = new EventManager(data.simLength, log);

			// Create Quoter entity, which enters the system at time 0
			Quoter iu = new Quoter(0, data, log);
			data.quoter = iu;
			eventManager.createEvent(new UpdateNBBO(iu, new TimeStamp(0)));

			// Create markets first since agent creation references markets
			for (Map.Entry<String, Integer> mkt: data.numMarketType.entrySet()) {
				for (int i = 0; i < mkt.getValue(); i++) {
					int mID = marketIDSequence.decrement();
					// create market
					setupMarket(mID, mkt.getKey());
				}
				log.log(Log.INFO, "Markets: " + mkt.getValue() + " " + mkt.getKey());
			}

			// Create agents, initialize parameters, and compute arrival times (if needed)
			for (Map.Entry<String, Integer> ag : data.numAgentType.entrySet()) {

				for (int i = 0; i < ag.getValue(); i++) {

					AgentProperties ap = specs.setStrategy(ag.getKey(), i);
					int aID = agentIDSequence.increment();

					// create agent & events
					setupAgent(aID, ag.getKey(), ap);
				}
				log.log(Log.INFO, "Agents: " + ag.getValue() + " " + ag.getKey());
			}
			// Log agent information
			logAgentInfo();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Creates market and initializes any Activities as necessary. For example,
	 * for Call Markets, this method inserts the initial Clear activity into the 
	 * eventQueue.
	 * 
	 * @param marketID
	 * @param marketType
	 */
	public void setupMarket(int marketID, String marketType) {
		Market market = MarketFactory.createMarket(marketType, marketID, data, log);
		data.addMarket(market);
		
		// Check if is call market, then initialize clearing sequence
		if (market instanceof CallMarket) {
			Activity clear = new Clear(market, market.getNextClearTime());
			eventManager.createEvent(clear);
		}
	}
	

	/**
	 * Creates agent and initializes all agent settings/parameters.
	 * Inserts AgentArrival/Departure activities into the eventQueue.
	 * 
	 * @param agentID
	 * @param agentType
	 * @param ap AgentProperties object
	 */
	public void setupAgent(int agentID, String agentType, AgentProperties ap) {
		Agent agent = AgentFactory.createAgent(agentType, agentID, data, ap, log);
		data.addAgent(agent);
		log.log(Log.DEBUG, agent.toString() + ": " + ap);
		
		TimeStamp ts = agent.getArrivalTime();
		Activity arrival = null;
		Activity departure = null;
		
		// Agent is in single market
		if (agent instanceof SMAgent) {
			for (int i = 1; i <= data.numMarkets; i++) {
				arrival = new AgentArrival(agent, data.getMarket(-i), ts);
				departure = new AgentDeparture(agent, data.getMarket(-i), data.simLength);
				eventManager.createEvent(arrival);
				eventManager.createEvent(departure);
			}
			
		} else if (agent instanceof MMAgent) {
			// Agent is in multiple markets
			arrival = new AgentArrival(agent, ts);
			departure = new AgentDeparture(agent, data.simLength);
			eventManager.createEvent(arrival);
			eventManager.createEvent(departure);
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
			if (ag instanceof BackgroundAgent) {
				s += ", pv=" + ((BackgroundAgent) ag).getPrivateValue();
			}
			log.log(Log.INFO, s);
		}
	}
	
	
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
				results.addObservation(id);
			}
			
			results.addFeature("bkgrd_surplus", results.getIntegerFeatures(data.getAllSurplus()));
			results.addFeature("bkgrd_profit", results.getIntegerFeatures(data.getAllProfit()));
			results.addFeature("arrival_interval", results.getTimeStampFeatures(data.getIntervals()));
			results.addFeature("pv", results.getPriceFeatures(data.getPrivateValues()));
			results.addFeature("bkgrd_info", results.getBackgroundInfo(data.getAgents()));
			results.addFeature("transactions", results.getTransactionInfo());
			results.addFeature("depths", results.getDepthInfo());
			results.addFeature("spreads", results.getSpreadInfo());
			results.addFeature("exec_speed", results.getExecutionSpeed());
			
			File file = new File(simFolder + Consts.obsFilename + num + ".json");
			FileWriter txt = new FileWriter(file);
			txt.write(results.generateObservationFile());
			txt.close();
			
		} catch (Exception e) {
			String s = "aggregateResults(): error creating observation file";
			e.printStackTrace();
			System.err.print(s);
		}
	}
	
}
