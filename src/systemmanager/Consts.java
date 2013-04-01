package systemmanager;

import entity.CallMarket;
import entity.ZIAgent;

/**
 * System-wide constants and accessor methods. Sets default properties for each
 * type of entity.
 * 
 * @author ewah
 */
public class Consts {
	
	public final static double[] rhos = 
		{0, 0.0001, 0.0002, 0.0003, 0.0004, 0.0005, 0.0006, 0.0007, 0.0008, 0.0009};
//	public final static double[] rhos = {0, 0.0001};
	public final static int[] windows = {250, 500, 750, 1000};
	
	// **********************************************************
	// Agent, market, and model types
	// (must edit this whenever add a new agent, market, or model
	
	public final static String ZI = "ZI";
	public final static String ZIP = "ZIP";
	public final static String BASICMARKETMAKER = "BASICMM";
	public final static String LA = "LA";
	public final static String DUMMY = "DUMMY";
	
	public final static String CALL = "CALL";
	public final static String CDA = "CDA";
	
	public final static String TWOMARKET = "TWOMARKET";
	public final static String CENTRALCDA = "CENTRALCDA";
	public final static String CENTRALCALL = "CENTRALCALL";
	
	public final static String ROLE_HFT = "HFT";
	public final static String ROLE_MARKETMAKER = "MARKETMAKER";
	public final static String ROLE_BACKGROUND = "BACKGROUND";
	
	public final static String[] SMAgentTypes = 
		{ ZI, ZIP, BASICMARKETMAKER };
//	public final static String[] marketTypeNames = 
//		{ CDA, CALL };
	public final static String[] modelTypeNames = 
		{ TWOMARKET, CENTRALCDA, CENTRALCALL };
	
	// EGTA roles
	public final static String[] roles =
		{ ROLE_HFT, ROLE_MARKETMAKER, ROLE_BACKGROUND };
	public final static String[] HFT_AGENT_TYPES = 
		{ LA, DUMMY };
	public final static String[] MARKETMAKER_AGENT_TYPES = 
		{ BASICMARKETMAKER };
	public final static String[] BACKGROUND_AGENT_TYPES =
		{ ZI, ZIP };
	
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
		else if (className.contains("MarketMaker"))
			return className.replace("MarketMaker","MM").toUpperCase();
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

	// Constants in simulation_spec file
	public final static String setupSuffix = "_setup";
	
	/**
	 * Get hard-coded default properties for a given entity type.
	 * 
	 * @param type
	 */
	public final static ObjectProperties getProperties(String type) {
		
		ObjectProperties p = new ObjectProperties();
		
		if (type.equals(LA)) {
			p.put("sleepTime", "0");
//			p.put("sleepVar", "100");
			p.put("alpha", "0.001");
		}
		if (type.equals(BASICMARKETMAKER)) {
			p.put("sleepTime", "200");
			// p.put("sleepVar", "100");
			p.put("numRungs", "10");
			p.put("rungSize", "1000");
		}
		if (type.equals(CALL)) {
			p.put(CallMarket.PRICING_POLICY_KEY, "0.5");
			p.put(CallMarket.CLEAR_FREQ_KEY, "100");
		}
		if (type.equals(ZI)) {
			p.put(ZIAgent.BIDRANGE_KEY, "200");
		}
		if (type.equals(ZIP)) {
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
