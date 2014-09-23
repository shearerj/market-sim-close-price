package systemmanager;

/**
 * All of the keys for use in the simulation spec. Parameters be camelCase.
 * OK for observation file descriptors to not be camel case.
 * 
 * @author erik
 * 
 */
public interface Keys {

	public final static String
	PRESETS = 					"presets",

	RAND_SEED = 				"randomSeed",
	NUM_SIMULATIONS =	 		"numSims",

	FILENAME = 					"fileName",

	ARRIVAL_RATE = 				"arrivalRate",

	REENTRY_RATE = 				"reentryRate",
	BACKGROUND_REENTRY_RATE = 	"backgroundReentryRate",
	MARKETMAKER_REENTRY_RATE = 	"marketmakerReentryRate",

	WINDOW_LENGTH = 			"windowLength",
	BID_RANGE_MAX = 			"bidRangeMax",
	BID_RANGE_MIN = 			"bidRangeMin",
	MAX_QUANTITY = 				"maxQty",
	ALPHA = 					"alpha",

	TICK_SIZE = 				"tickSize",
	MARKET_TICK_SIZE = 			"marketTickSize",
	AGENT_TICK_SIZE = 			"agentTickSize",

	PRIVATE_VALUE_VAR = 		"privateValueVar",
	SIMULATION_LENGTH = 		"simLength",
	FUNDAMENTAL_MEAN = 			"meanValue",
	FUNDAMENTAL_KAPPA = 		"kappa",
	FUNDAMENTAL_SHOCK_VAR = 	"shockVar",
	PRIMARY_MODEL = 			"primaryModel",

	NUM = 						"num",
	NUM_AGENTS =				"numAgents",
	NUM_MARKETS = 				"numMarkets",

	// SimulationSpec Keys
	ASSIGN = 					"assignment",
	CONFIG = 					"configuration",
	NO_OP = 					"noOp",

	// Observation Keys
	PV_BUY1 = 					"pv_buy1",
	PV_SELL1 = 					"pv_sell1",
	PV_POSITION1_MAX_ABS =		"pv_position_max_abs1",

	// Latency
	QUOTE_LATENCY = 			"quoteLatency",
	TRANSACTION_LATENCY = 		"transactionLatency",
	NBBO_LATENCY = 				"nbboLatency",
	MARKET_LATENCY = 			"mktLatency",
	LA_LATENCY = 				"laLatency",
	FUNDAMENTAL_LATENCY = 		"fundamentalLatency",

	// Call Market
	CLEAR_FREQ =				"clearFreq",
	PRICING_POLICY = 			"pricingPolicy",

	// Agents
	WITHDRAW_ORDERS = 			"withdrawOrders", 
	NUM_HISTORICAL = 			"numHistorical",

	// Market Maker
	NUM_RUNGS = 				"numRungs",
	RUNG_SIZE = 				"rungSize",
	TRUNCATE_LADDER = 			"truncateLadder",
	TICK_IMPROVEMENT = 			"tickImprovement",
	TICK_OUTSIDE = 				"tickOutside",
	INITIAL_LADDER_MEAN = 		"initLadderMean",
	INITIAL_LADDER_RANGE = 		"initLadderRange",

	// AAAgent
	ETA = 						"eta",
	LAMBDA_R = 					"lambdaR",
	LAMBDA_A = 					"lambdaA",
	GAMMA = 					"gamma",
	BETA_R = 					"betaR",
	BETA_T = 					"betaT",
	AGGRESSION = 				"aggression",
	THETA = 					"theta",
	THETA_MAX = 				"thetaMax",
	THETA_MIN = 				"thetaMin",
	DEBUG = 					"debug",
	BUYER_STATUS = 				"buyerStatus",

	// ZIPAgent
	MARGIN_MIN = 				"marginMin",
	MARGIN_MAX = 				"marginMax",
	GAMMA_MIN = 				"gammaMin",
	GAMMA_MAX = 				"gammaMax",
	BETA_MIN = 					"betaMin",
	BETA_MAX = 					"betaMax",
	COEFF_R = 					"rangeR",
	COEFF_A = 					"rangeA",

	// Market Makers
	WEIGHT_FACTOR = 			"weightFactor",
	SPREADS = 					"spreads",
	USE_MEDIAN_SPREAD = 		"useMedianSpread",
	MOVING_AVERAGE_PRICE = 		"movingAveragePrice",	// TODO remove in next iteration, only keeping for backwards compatibility
	FAST_LEARNING = 			"fastLearning",
	USE_LAST_PRICE = 			"useLastPrice",
	END_FUNDAMENTAL_ESTIMATE = 	"fundEstimate",

	// ZIRPAgent
	ACCEPTABLE_PROFIT_FRACTION = "acceptableProfFrac";
}
