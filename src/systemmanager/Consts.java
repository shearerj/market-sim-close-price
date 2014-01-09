package systemmanager;

/**
 * System-wide constants
 * 
 * @author ewah
 */
public interface Consts {
	
	public enum DiscountFactor {
		NO_DISC(0), MEDIUM(0.0006);
		// SMALL(0.0003), LARGE(0.0009)

		public final double discount;

	    DiscountFactor(double discount) {
	        this.discount = discount;
	    }
	    
	    @Override
	    public String toString() {
	    	if (discount == 0)
	    		return "no_disc";
	    	return "disc_" + discount;
	    }
	}
	
	public final static int[] PERIODS = { 1, 250 };
	
	public final static int UP_TO_TIME = 3000;	// compute statistics up to this time
	
	// **********************************************************
	// Agent, market, and model types
	// UPDATE WHEN ADD NEW AGENT, MARKET, OR MODEL
	public static enum AgentType { AA, ZI, ZIP, ZIR, BASICMM, LA, DA, ODA };
	public static enum MarketType { CDA, CALL };
	
	public static enum Presets { TWOMARKET, TWOMARKETLA, CENTRALCDA, CENTRALCALL };
	
	// **********************************************************
	// FILENAMES
	
	// Directories
	public final static String TEST_OUTPUT_DIR = "simulations/unit_testing/";
	public final static String CONFIG_DIR = "config/";
	public final static String LOG_DIR = "logs/";
	
	// Config/spec file names
	public final static String SIM_SPEC_FILE = "simulation_spec.json";
	public final static String CONFIG_FILE = "env.properties";
	public final static String OBS_FILE_PREFIX = "observation";
	public final static String OBJS_FILE_PREFIX = "objects";

	// Constants in simulation_spec file
	public final static String SETUP_SUFFIX = "_setup";

}
