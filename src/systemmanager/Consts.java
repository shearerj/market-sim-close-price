package systemmanager;

import entity.*;
import event.TimeStamp;
import data.ObjectProperties;

import java.util.Arrays;
import java.util.List;

/**
 * System-wide constants and accessor methods. Sets default properties for each
 * type of entity.
 * 
 * @author ewah
 */
public class Consts {
	
	// 0 indicates no surplus discounting
	public final static double[] rhos = {0, 0.0006};
	//	{0, 0.0001, 0.0002, 0.0003, 0.0004, 0.0005, 0.0006, 0.0007, 0.0008, 0.0009};
	
	// 0 means sampling every time step
	public final static int[] periods = {0, 1, 250};
	
	public final static long upToTime = 3000;	// compute statistics up to this time
	
	// **********************************************************
	// Agent, market, and model types
	// UPDATE WHEN ADD NEW AGENT, MARKET, OR MODEL
	public final static String AA = "AA";
	public final static String ZI = "ZI";
	public final static String ZIP = "ZIP";
	public final static String ZIR = "ZIR";
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
	
	public final static String TYPE_ENVIRONMENT = "ENV";
	public final static String TYPE_PLAYER = "PLAYER";
	
	// **********************************************************
	// Lists
	// UPDATE WHEN ADD NEW AGENT, MARKET, OR MODEL
	public final static List<String> roles = 
			Arrays.asList(ROLE_HFT, ROLE_MARKETMAKER, ROLE_BACKGROUND);
	
	public final static List<String> MARKETMODEL_TYPES =
			Arrays.asList(TWOMARKET, CENTRALCDA, CENTRALCALL);
	
	public final static List<String> SM_AGENT_TYPES = 
			Arrays.asList(ZI, ZIR,  ZIP, BASICMARKETMAKER, AA);
	public final static List<String> HFT_AGENT_TYPES =
			Arrays.asList(LA, DUMMY);
	public final static List<String> MARKETMAKER_AGENT_TYPES = 
			Arrays.asList(BASICMARKETMAKER);
	public final static List<String> BACKGROUND_AGENT_TYPES =
			Arrays.asList(ZI, ZIR, ZIP);
	
	// **********************************************************
	// Setting up models
	public final static String MODEL_CONFIG_KEY = "config";
	public final static String MODEL_CONFIG_NONE = "NONE";
	
	// ActivityList priorities (lower the number, higher the priority)
//	public final static int HIGHEST_PRIORITY = -999;
//	public final static int ARRIVAL_PRIORITY = -10; 	// inserted with high priority
//	public final static int HFT_ARRIVAL_PRIORITY = -10;
//	public final static int DEFAULT_PRIORITY = 0;
//	public final static int SUBMIT_BID_PRIORITY = 0;
//	public final static int CDA_CLEAR_PRIORITY = 1;
//	public final static int WITHDRAW_BID_PRIORITY = 2; // always happen after the bid is submitted
//	public final static int THRESHOLD_PRE_PRIORITY = 2;
//	public final static int SEND_TO_SIP_PRIORITY = 3;
//	public final static int UPDATE_NBBO_PRIORITY = 3;
//	public final static int CALL_CLEAR_PRIORITY = 4;
//	public final static int THRESHOLD_POST_PRIORITY = 5;
//	public final static int HFT_AGENT_PRIORITY = 7;
//	public final static int MARKETMAKER_PRIORITY = 8;
//	public final static int BACKGROUND_ARRIVAL_PRIORITY = 9;
	// public final static int BACKGROUND_REENTRY_PRIORITY = 10;
//	public final static int BACKGROUND_AGENT_PRIORITY = 12;
//	public final static int LOWEST_PRIORITY = 999;
	
	// TimeStamp
	public final static TimeStamp INF_TIME = new TimeStamp(-1);
	
	// Price
	public final static int INF_PRICE = Integer.MAX_VALUE;
	
	// Other
	public final static String NAN = "NaN";
	public final static double DOUBLE_NAN = Double.NaN;
	
	// **********************************************************
	// FILENAMES
	// Directories
	public final static String configDir = "config/";
	public final static String logDir = "logs/";
	
	// Config/spec file names
	public final static String simSpecFile = "simulation_spec.json";
	public final static String configFile = "env.properties";
	public final static String obsFile = "observation";

	// Constants in simulation_spec file
	public final static String setupSuffix = "_setup";
	
	

	// **********************************************************
	// METHODS
	
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

	/**
	 * Get hard-coded default properties for a given entity type.
	 * 
	 * @param type
	 */
	public final static ObjectProperties getProperties(String type) {
		
		// UPDATE WHEN ADD NEW AGENT OR MARKET
		ObjectProperties p = new ObjectProperties();
		
		if (type.equals(LA)) {
			p.put(Agent.SLEEPTIME_KEY, "0");
			p.put(Agent.SLEEPVAR_KEY, "100");
			p.put(LAAgent.ALPHA_KEY, "0.001");
		}
		if (type.equals(BASICMARKETMAKER)) {
			p.put(Agent.SLEEPTIME_KEY, "200");
			p.put(Agent.SLEEPVAR_KEY, "100");
			p.put(BasicMarketMaker.NUMRUNGS_KEY, "10");
			p.put(BasicMarketMaker.RUNGSIZE_KEY, "1000");
		}
		if (type.equals(CALL)) {
			p.put(CallMarket.PRICING_POLICY_KEY, "0.5");
			p.put(CallMarket.CLEAR_FREQ_KEY, "100");
		}
		if (type.equals(ZI)) {
			p.put(Agent.BIDRANGE_KEY, "2000");
		}
		if (type.equals(ZIR)) {
			p.put(Agent.BIDRANGE_KEY, "5000");
			p.put(Agent.MAXQUANTITY_KEY, "10");
		}
		if (type.equals(ZIP)) {
			p.put(Agent.SLEEPTIME_KEY, "50");
			p.put(Agent.SLEEPVAR_KEY, "100");
			p.put("c_R","0.05");
			p.put("c_A","0.05");
			p.put("beta","0.03");
			p.put("betaVar", "0.005");
			p.put("gamma","0.5");
		}
		if (type.equals(AA)) {
			// FILL IN
			p.put(AAAgent.BIDRANGE_KEY, "200"); // example only
		}
		return p;
	}
	
}
