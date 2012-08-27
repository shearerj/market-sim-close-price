package systemmanager;

/**
 * System-wide constants.
 * 
 * @author ewah
 */
public class SystemConsts {
	
	// Entity types
	public final static String[] agentTypes = {"HFT", "NBBO", "ZI", "M"};
	public final static String[] marketTypes = {"CDA", "CALL"}; 

	// Directories
	public final static String configDir = "config/";
	public final static String logDir = "logs/";
	
	// Track type of bid submission for background agents
	// NOBID = no bid submitted yet
	// CURRENT = bid submitted to current market
	// ALTERNATE = bid submitted to alternate market, see extra info
	public enum NBBOBidType {
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
	public final static String[] roles = {"HFT"};

	/**
	 * Get hard-coded default properties for a given agent type.
	 * 
	 * @param type
	 */
	public final static AgentProperties getProperties(String type) {
		
		AgentProperties ap = new AgentProperties();
		
		if (type.equals("HFT")) {
			ap.put("sleepTime", "0");
			ap.put("sleepVar", "100");
			ap.put("alpha", "0.001");
			
		} else if (type.equals("NBBO")) {
			
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