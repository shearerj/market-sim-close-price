package systemmanager;

/**
 * System-wide constants and accessor methods.
 * 
 * @author ewah
 */
public class Consts {
	
	// Entity types
	public final static String[] agentTypeNames = {"LA", "BKGRD", "ZI", "M", "DUMMY"};
	public final static String[] marketTypeNames = {"CDA", "CALL"};

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
		if (className.equals("LAAgent")) {
			return agentTypeNames[0];	
		} else if (className.equals("BackgroundAgent")) {
			return agentTypeNames[1];
		} else if (className.equals("ZIAgent")) {
			return agentTypeNames[2];
		} else if (className.equals("MarketMaker")) {
			return agentTypeNames[3];
		} else if (className.equals("DummyAgent")) {
			return agentTypeNames[4];
		} else
			return null;
	}
	
	
	// Directories
	public final static String configDir = "config/";
	public final static String logDir = "logs/";
	
	// Track type of bid submission for background agents
	// NOBID = no bid submitted yet
	// CURRENT = bid submitted to current market
	// ALTERNATE = bid submitted to alternate market, see extra info
	public enum SubmittedBidType {
		NOBID,
		CURRENT,
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
		} else if (type.equals("BKGRD")) {
			
		} else if (type.equals("DUMMY")) {
			
		} else if (type.equals("ZI")) {
			ap.put("sleepTime", "150");
			ap.put("sleepVar", "100");
		} else if (type.equals("M")) {
			ap.put("sleepTime", "150");
			ap.put("sleepVar", "100");
		} 
		return ap;
	}
	
	
}