package systemmanager;

/**
 * All of the keys for use in the simulation spec.
 * 
 * @author erik
 * 
 */
public interface Keys {

	public final static String MODEL_NAME = "modelName";
	public final static String MODEL_NUM = "modelNum";
	
	public final static String PRESETS = "presets";
	
	public final static String RAND_SEED = "randomSeed";
	public final static String NUM_SIMULATIONS = "numSims";
	
	public final static String ARRIVAL_RATE = "arrivalRate";
	public final static String REENTRY_RATE = "reentryRate";

	public final static String WINDOW_LENGTH = "windowLength";
	public final static String BID_RANGE_MAX = "bidRangeMax";
	public final static String BID_RANGE_MIN = "bidRangeMin";
	public final static String MAX_QUANTITY = "maxqty";
	public final static String ALPHA = "alpha";
	
	public final static String TICK_SIZE = "tickSize";
	
	public final static String PRIVATE_VALUE_VAR = "privateValueVar";
	public final static String SIMULATION_LENGTH = "simLength";
	public final static String FUNDAMENTAL_MEAN = "meanValue";
	public final static String FUNDAMENTAL_KAPPA = "kappa";
	public final static String FUNDAMENTAL_SHOCK_VAR = "shockVar";
	public final static String PRIMARY_MODEL = "primaryModel";
	
	public final static String NUM_LA = "numLA";
	public final static String NUM = "num";
	
	// SimulationSpec Keys
	public final static String ASSIGN = "assignment";
	public final static String CONFIG = "configuration";
	public final static String NO_OP = "noOp";

	// Latency
	public final static String QUOTE_LATENCY = "quoteLatency";
	public final static String TRANSACTION_LATENCY = "transactionLatency";
	public final static String NBBO_LATENCY = "nbboLatency";
	public final static String MARKET_LATENCY = "mktLatency";
	public final static String LA_LATENCY = "laLatency";
	
	// Call Market
	public final static String CLEAR_FREQ = "clearFreq";
	public final static String PRICING_POLICY = "pricingPolicy";

	// Agents
	public final static String WITHDRAW_ORDERS = "withdrawOrders"; 

	// Market Maker
	public final static String NUM_RUNGS = "numRungs";
	public final static String RUNG_SIZE = "rungSize";
	public final static String TRUNCATE_LADDER = "truncateLadder";

	// AAAgent
	public final static String HISTORICAL = "historical";
	public final static String ETA = "eta";
	public final static String LAMBDA_R = "lambdaR";
	public final static String LAMBDA_A = "lambdaA";
	public final static String GAMMA = "gamma";
	public final static String BETA_R = "betaR";
	public final static String BETA_T = "betaT";
	public final static String AGGRESSION = "aggression";
	public final static String THETA = "theta";
	public final static String THETA_MAX = "thetaMax";
	public final static String THETA_MIN = "thetaMin";
	public final static String DEBUG = "debug";
	public final static String TEST = "AAtesting";
	public final static String BUYER_STATUS = "buyerStatus";
	
	// ZIPAgent
	public final static String MARGIN_MIN = "marginMin";
	public final static String MARGIN_MAX = "marginMax";
	public final static String GAMMA_MIN = "gammaMin";
	public final static String GAMMA_MAX = "gammaMax";
	public final static String BETA_MIN = "betaMin";
	public final static String BETA_MAX = "betaMax";
	public final static String COEFF_R = "rangeR";
	public final static String COEFF_A = "rangeA";
	
	// MarketMakers
	public final static String WEIGHT_FACTOR = "weightFactor";
}
