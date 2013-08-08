package data;

public interface Keys {

	public final static String RAND_SEED = "randomSeed";
	
	public final static String ARRIVAL_RATE = "arrivalRate";
	public final static String REENTRY_RATE = "reentryRate";
	
	public final static String SLEEP_TIME = "sleepTime";

	public final static String BID_RANGE = "bidRange";
	public final static String MAX_QUANTITY = "maxqty";
	public final static String NUM_RUNGS = "numRungs";
	public final static String RUNG_SIZE = "rungSize";
	public final static String ALPHA = "alpha";
	
	public final static String TICK_SIZE = "tickSize";
	
	public final static String PRIVATE_VALUE_VAR = "privateValueVar";
	public final static String SIMULATION_LENGTH = "simLength";
	public final static String FUNDAMENTAL_MEAN = "meanValue";
	public final static String FUNDAMENTAL_KAPPA = "kappa";
	public final static String FUNDAMENTAL_SHOCK_VAR = "shockVar";
	public final static String PRIMARY_MODEL = "primaryModel";
	
	public final static String NUM_LA = "numLA";
	
	// Latency
	public final static String NBBO_LATENCY = "nbboLatency";
	public final static String MARKET_LATENCY = "mktLatency";
	public final static String LA_LATENCY = "laLatency";
	
	// Call Market
	public final static String CLEAR_FREQ = "clearFreq";
	public final static String PRICING_POLICY = "pricingPolicy";

	// SimulationSpec Keys
	public final static String ASSIGN = "assignment";
	public final static String CONFIG = "configuration";

	// AAAgent
	public final static String HISTORICAL = "historical";
	public final static String ETA = "eta";
	public final static String AGGRESSION = "aggression";
	public final static String THETA = "theta";
	public final static String THETA_MAX = "thetaMax";
	public final static String THETA_MIN = "thetaMin";
	public final static String DEBUG = "debug";
	public final static String TEST = "AAtesting";
	public final static String BUYER_STATUS = "buyerStatus";
	
	// ZIP
	public final static String CR = "cR";
	public final static String CA = "cA";
	public final static String BETA = "beta";
	public final static String BETA_VAR = "betaVar";
	public final static String GAMMA = "gamma";
	
}
