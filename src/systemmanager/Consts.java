package systemmanager;

import entity.*;
import event.TimeStamp;
import data.ObjectProperties;

import java.util.Arrays;
import java.util.EnumSet;
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
	public static enum AgentType {
		AA, ZI, ZIP, ZIR, BASICMM, LA, DUMMY;
		
		public static boolean contains(String s) {
			for (AgentType a : values())
				if (a.toString().equals(s))
					return true;
			return false;
		}
	};
	public static enum ModelType {
		TWOMARKET, CENTRALCDA, CENTRALCALL;
		
		public static boolean contains(String s) {
			for (ModelType a : values())
				if (a.toString().equals(s))
					return true;
			return false;
		}
	};
	public static enum MarketType {
		CDA, CALL;
	
		public static boolean contains(String s) {
			for (MarketType a : values())
				if (a.toString().equals(s))
					return true;
			return false;
		}
	}
	public static final EnumSet<AgentType> BACKGROUND_AGENT = EnumSet.of(AgentType.ZI, AgentType.ZIR, AgentType.ZIP, AgentType.AA);
	public static final EnumSet<AgentType> MARKETMAKER_AGENT = EnumSet.of(AgentType.BASICMM);
	public static final EnumSet<AgentType> MM_AGENT = EnumSet.of(AgentType.LA, AgentType.DUMMY);
	public static final EnumSet<AgentType> SM_AGENT = EnumSet.complementOf(MM_AGENT);
	
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
	
	// **********************************************************
	// Setting up models
	public final static String MODEL_CONFIG_KEY = "config";
	public final static String MODEL_CONFIG_NONE = "NONE";
	
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
		if (MarketType.contains(type))
			return getProperties(MarketType.valueOf(type));
		else if (AgentType.contains(type))
			return getProperties(AgentType.valueOf(type));
		else if (ModelType.contains(type))
			return getProperties(ModelType.valueOf(type));
		else
			// Log?
			return new ObjectProperties();
	}
	
	public final static ObjectProperties getProperties(ModelType type) {
		// UPDATE WHEN ADD NEW MODEL
		ObjectProperties p = new ObjectProperties();
		switch (type) {
		default:
			return p;
		}
	}

	public final static ObjectProperties getProperties(MarketType type) {
		// UPDATE WHEN ADD NEW MARKET
		ObjectProperties p = new ObjectProperties();
		switch (type) {
		case CALL:
			p.put(CallMarket.PRICING_POLICY_KEY, "0.5");
			p.put(CallMarket.CLEAR_FREQ_KEY, "100");
			return p;
		case CDA:
			return p;
		default:
			return p;
		}
	}

	public final static ObjectProperties getProperties(AgentType type) {
		// UPDATE WHEN ADD NEW AGENT
		ObjectProperties p = new ObjectProperties();
		switch (type) {
		case LA:
			p.put(Agent.SLEEPTIME_KEY, "0");
			p.put(Agent.SLEEPVAR_KEY, "100");
			p.put(LAAgent.ALPHA_KEY, "0.001");
			return p;
		case BASICMM:
			p.put(Agent.SLEEPTIME_KEY, "200");
			p.put(Agent.SLEEPVAR_KEY, "100");
			p.put(BasicMarketMaker.NUMRUNGS_KEY, "10");
			p.put(BasicMarketMaker.RUNGSIZE_KEY, "1000");
			return p;
		case ZI:
			p.put(Agent.BIDRANGE_KEY, "2000");
			return p;
		case ZIR:
			p.put(Agent.BIDRANGE_KEY, "5000");
			p.put(Agent.MAXQUANTITY_KEY, "10");
			return p;
		case ZIP:
			p.put(Agent.SLEEPTIME_KEY, "50");
			p.put(Agent.SLEEPVAR_KEY, "100");
			p.put("c_R", "0.05");
			p.put("c_A", "0.05");
			p.put("beta", "0.03");
			p.put("betaVar", "0.005");
			p.put("gamma", "0.5");
			return p;
		case AA:
			// TODO Fill in
			p.put(AAAgent.BIDRANGE_KEY, "200"); // example only
			return p;
		default:
			return p;
		}
	}

}
