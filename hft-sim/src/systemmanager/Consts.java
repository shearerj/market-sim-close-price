package systemmanager;

/**
 * System-wide constants
 * 
 * @author ewah
 */
public interface Consts {
	
	// **********************************************************
	// Agent, market, and model types
	// XXX UPDATE WHEN ADD NEW AGENT, MARKET, OR MODEL
	// TODO Use the class name instead of this strange enum? This could be a Collection<Class<? extends Agent>>
	public static enum AgentType { NOOP, AA, ZI, ZIP, ZIR, ZIRP, 
							ADAPTIVEMM, BASICMM, CONSTMM, MAMM, WMAMM, LA, ODA, MARKETDATA };
	public static enum MarketType { CDA, CALL };
		
	// **********************************************************
	// FILENAMES
	
	// Directories
	public final static String TEST_OUTPUT_DIR = "simulations/unit_testing/";
	
	// Exchanges for market data agent
	public final static String NYSE = "nyse";
	public final static String NASDAQ = "nasdaq";

}
