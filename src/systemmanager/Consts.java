package systemmanager;

/**
 * System-wide constants and accessor methods. Sets default properties for each
 * type of entity.
 * 
 * @author ewah
 */
public class Consts {
	
//	public final static double[] rhos = 
//		{0, 0.0001, 0.0002, 0.0003, 0.0004, 0.0005, 0.0006, 0.0007, 0.0008, 0.0009};
	public final static double[] rhos = {0, 0.0006};
//	public final static int[] windows = {250, 500, 750, 1000};
	public final static int[] windows = {250};
	
	// **********************************************************
	// Agent, market, and model types
	// (must edit this whenever add a new agent, market, or model
	
	public final static String[] SMAgentTypes = 
		{ "MARKETMAKER", "ZI", "ZIP", "AA" };
	public final static String[] HFTAgentTypes = 
		{ "LA", "DUMMY" };
	public final static String[] marketTypeNames = 
		{ "CDA", "CALL" };
	public final static String[] modelTypeNames = 
		{ "TWOMARKET", "CENTRALCDA", "CENTRALCALL" };
		
	// EGTA roles (i.e. players in the game)
	public final static String[] roles =
		{"LA", "DUMMY"};
	// **********************************************************
	
	// Setting up models
	public final static String MODEL_CONFIG_KEY = "config";
	public final static String MODEL_CONFIG_NONE = "NONE";
	
	// ActivityList priorities
	public final static int DEFAULT_PRIORITY = 0;
	public final static int SEND_TO_SIP_PRIORITY = 0;
	public final static int UPDATE_NBBO_PRIORITY = 0;
	public final static int SUBMIT_BID_PRIORITY = 1;
	public final static int WITHDRAW_BID_PRIORITY = 2; // always happen after the bid is submitted
	public final static int CDA_CLEAR_PRIORITY = 3;
	public final static int CALL_CLEAR_PRIORITY = 3;
	public final static int HFT_AGENT_PRIORITY = 5;
	public final static int MARKETMAKER_PRIORITY = 6;
	public final static int SM_AGENT_PRIORITY = 7;
	public final static int LOWEST_PRIORITY = 999;
	// AgentArrival/Departure inserted with default priority
	// AgentStrategy inserted with agent-specific priority
	
	// TimeStamp
	public final static long INF_TIME = -1;
	
	// Price
	public final static int INF_PRICE = 999999999;
//	public final static int SCALING_FACTOR = 100;
	
	/**
	 * Returns the market type based on the class name.
	 * @param className
	 * @return String of market type
	 */
	public static String getMarketType(String className) {
		if (className.contains("Market"))
			return className.replace("Market","").toUpperCase();
		else
			return className.toUpperCase();
	}
	
	/**
	 * Returns the agent type based on the class name.
	 * @param className
	 * @return
	 */
	public static String getAgentType(String className) {
		if (className.contains("Agent"))
			return className.replace("Agent","").toUpperCase();
		else
			return className.toUpperCase();
	}
	
	public static String getModelType(String className) {
		return className.toUpperCase();
	}
	
	// Directories
	public final static String configDir = "config/";
	public final static String logDir = "logs/";
	
//	// Track type of bid submission for background agents
//	// NOBID = no bid submitted yet
//	// MAIN = bid submitted to main market
//	// ALTERNATE = bid submitted to alternate market, see extra info
//	public enum SubmittedBidMarket { NOBID,	MAIN, ALTERNATE	}

	// ----- SIMULATION -----
	
	// Config/spec file names
	public final static String simSpecFile = "simulation_spec.json";
	public final static String configFile = "env.properties";
	public final static String obsFilename = "observation";


	/**
	 * Get hard-coded default properties for a given entity type.
	 * 
	 * @param type
	 */
	public final static ObjectProperties getProperties(String type) {
		
		ObjectProperties p = new ObjectProperties();
		
		if (type.equals("LA")) {
			p.put("sleepTime", "0");
			p.put("sleepVar", "100");
			p.put("alpha", "0.001");
		}
		if (type.equals("MARKETMAKER")) {
			p.put("sleepTime", "200");
			p.put("sleepVar", "100");
			p.put("numRungs", "10");
			p.put("rungSize", "1000");
		}
		if (type.equals("CALL")) {
			p.put("pricingPolicy", "0.5");
			p.put("clearFreq", "100");
		}
		if (type.equals("ZIP")) {
			p.put("sleepTime", "50");
			p.put("sleepVar", "100");
			p.put("c_R","0.05");
			p.put("c_A","0.05");
			p.put("beta","0.03");
			p.put("betaVar", "0.005");
			p.put("gamma","0.5");
		}
		return p;
	}
	
}
