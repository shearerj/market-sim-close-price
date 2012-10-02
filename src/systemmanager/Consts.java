package systemmanager;

/**
 * System-wide constants and accessor methods. Sets default properties for each
 * type of entity.
 * 
 * @author ewah
 */
public class Consts {
	
	// ActivityList priority
	public final static int DEFAULT_PRIORITY = 0;
	public final static int NBBO_PRIORITY = 2;
	public final static int HFT_PRIORITY = 1;
	
	// TimeStamp
	public final static long INF_TIME = -1;
	
	// Price
	public final static int INF_PRICE = 999999999;
	
	// Entity types
	public final static String[] agentTypeNames = {"LA", "ZI", "DUMMY"};
	public final static String[] marketTypeNames = {"CDA", "CALL"};
	public final static String CENTRAL = "CENTRAL";
	
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
	
	
	// Directories
	public final static String configDir = "config/";
	public final static String logDir = "logs/";
	
	// Track type of bid submission for background agents
	// NOBID = no bid submitted yet
	// MAIN = bid submitted to main market
	// ALTERNATE = bid submitted to alternate market, see extra info
	public enum SubmittedBidMarket {
		NOBID,
		MAIN,
		ALTERNATE
	}

	// ----- SIMULATION -----
	
	// Config/spec file names
	public final static String simSpecFile = "simulation_spec.json";
	public final static String configFile = "env.properties";
	public final static String obsFilename = "observation";

	// EGTA roles (i.e. players in the game)
	public final static String[] roles = {"LA", "DUMMY"};

	/**
	 * Get hard-coded default properties for a given agent type.
	 * 
	 * @param type
	 */
	public final static AgentProperties getProperties(String type) {
		
		AgentProperties ap = new AgentProperties();
		
		if (type.equals("LA")) {
			ap.put("sleepTime", "0");
			ap.put("sleepVar", "100");
			ap.put("alpha", "0.001");
		} else if (type.equals("BACKGROUND")) {
			
		} else if (type.equals("DUMMY")) {
			
		}
		return ap;
	}
	
	
}