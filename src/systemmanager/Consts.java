package systemmanager;

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
	
	public final static int[] periods = {1, 250};
	
	public final static long upToTime = 3000;	// compute statistics up to this time
	
	// **********************************************************
	// Agent, market, and model types
	// UPDATE WHEN ADD NEW AGENT, MARKET, OR MODEL
	public static enum AgentType { AA, ZI, ZIP, ZIR, BASICMM, LA };
	public static enum ModelType { TWOMARKET, CENTRALCDA, CENTRALCALL };
	public static enum MarketType { CDA, CALL };
	
	// **********************************************************
	// FILENAMES
	
	// Directories
	public final static String CONFIG_DIR = "config/";
	public final static String LOG_DIR = "logs/";
	
	// Config/spec file names
	public final static String SIM_SPEC_FILE = "simulation_spec.json";
	public final static String CONFIG_FILE = "env.properties";
	public final static String OBS_FILE_PREFIX = "observation";

	// Constants in simulation_spec file
	public final static String SETUP_SUFFIX = "_setup";

}
