package systemmanager;

/**
 * System-wide constants and accessor methods. Sets default properties for each
 * type of entity.
 * 
 * @author ewah
 */
public class Consts {
	
	// **********************************************************
	// Agent, market, and model types
	// (must edit this whenever add a new agent, market, or model
	
	public final static String[] agentTypeNames = 
		{ "LA", "ZI", "DUMMY" };
	public final static String[] marketTypeNames = 
		{ "CDA", "CALL"};
	public final static String[] modelTypeNames = 
		{ "TWOMARKET", "CENTRALCDA", "CENTRALCALL" };
		
	// EGTA roles (i.e. players in the game)
	public final static String[] roles = {"LA", "DUMMY"};
	// **********************************************************
	
	// ActivityList priorities
	public final static int DEFAULT_PRIORITY = 0;
	public final static int SUBMIT_BID_PRIORITY = 1;
	public final static int CDA_CLEAR_PRIORITY = 2;
	public final static int CALL_CLEAR_PRIORITY = 2;
	public final static int HFT_PRIORITY = 3;
	public final static int WITHDRAW_BID_PRIORITY = 4;
	public final static int UPDATE_NBBO_PRIORITY = 5;
	
	// TimeStamp
	public final static long INF_TIME = -1;
	
	// Price
	public final static int INF_PRICE = 999999999;
	public final static int SCALING_FACTOR = 100;
	
	
	/**
	 * Returns the market type based on the class name.
	 * @param className
	 * @return String of market type
	 */
	public static String getMarketType(String className) {
		return className.replace("Market","").toUpperCase();
	}
	
	/**
	 * Returns the agent type based on the class name.
	 * @param className
	 * @return
	 */
	public static String getAgentType(String className) {
		return className.replace("Agent","").toUpperCase();
	}
	
	public static String getModelType(String className) {
		return className.toUpperCase();
	}
	
	// Directories
	public final static String configDir = "config/";
	public final static String logDir = "logs/";
	
	// Track type of bid submission for background agents
	// NOBID = no bid submitted yet
	// MAIN = bid submitted to main market
	// ALTERNATE = bid submitted to alternate market, see extra info
	public enum SubmittedBidMarket { NOBID,	MAIN, ALTERNATE	}

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
		
		ObjectProperties ep = new ObjectProperties();
		
		if (type.equals("LA")) {
			ep.put("sleepTime", "0");
			ep.put("sleepVar", "100");
			ep.put("alpha", "0.001");	
		}
		if (type.equals("CALL")) {
			ep.put("pricingPolicy", "0.5");
			ep.put("clearFreq", "100");
		}
		
		return ep;
	}
	
}