package systemmanager;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import event.TimeStamp;


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
	
	// FIXME shouldn't this just be simulation length?
	public final static long upToTime = 3000;	// compute statistics up to this time
	
	// **********************************************************
	// Agent, market, and model types
	// UPDATE WHEN ADD NEW AGENT, MARKET, OR MODEL
	public static enum AgentType {
		AA, ZI, ZIP, ZIR, BASICMM, LA, DUMMY;
		public static boolean contains(String s) {
			for (AgentType a : values()) if (a.toString().equals(s)) return true;
			return false;
		}
	};
	
	public static enum ModelType {
		TWOMARKET, CENTRALCDA, CENTRALCALL;
		public static boolean contains(String s) {
			for (ModelType a : values()) if (a.toString().equals(s)) return true;
			return false;
		}
	};
	
	public static enum MarketType {
		CDA, CALL;
		public static boolean contains(String s) {
			for (MarketType a : values()) if (a.toString().equals(s)) return true;
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
	public final static TimeStamp START_TIME = new TimeStamp(0);
	
	// Price TODO Move to Price
	public final static int INF_PRICE = Integer.MAX_VALUE;
	
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
	@Deprecated
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
	@Deprecated
	public static String getAgentType(String className) {
		if (className.contains("Agent"))
			return className.replace("Agent","").toUpperCase();
		else if (className.contains("MarketMaker"))
			return className.replace("MarketMaker","MM").toUpperCase();
		else
			return className.toUpperCase();
	}
	
	@Deprecated
	public static String getModelType(String className) {
		return className.toUpperCase();
	}

}
