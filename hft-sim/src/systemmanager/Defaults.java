package systemmanager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores ALL hard-coded defaults for simulation spec (environment) parameters 
 * and agent strategy parameters.
 *  
 * @author ewah
 *
 */
public class Defaults implements Serializable {

	private static final long serialVersionUID = -2159139939831086051L;
	
	// TODO reduce size of strategy strings (they're all way too long, provide some 
	// better guidelines on usage

	protected final static Map<String, String> defaults = new HashMap<String, String>();
	
	public static void initialize() {
				
		// General
		add(Keys.NUM,					0);
		add(Keys.NUM_AGENTS,			0);
		add(Keys.NUM_MARKETS, 			1);
		
		add(Keys.TICK_SIZE,				1);
		add(Keys.MARKET_TICK_SIZE,		1);
		add(Keys.AGENT_TICK_SIZE,		1);
		
		// Simulation spec (general)
		add(Keys.SIMULATION_LENGTH, 	60000);
		add(Keys.FUNDAMENTAL_MEAN, 		100000);
		add(Keys.FUNDAMENTAL_KAPPA,		0.05);
		add(Keys.FUNDAMENTAL_SHOCK_VAR, 1000000);
		add(Keys.RAND_SEED,				System.currentTimeMillis());
		add(Keys.NUM_SIMULATIONS,		1);
		add(Keys.NBBO_LATENCY,			-1);
		add(Keys.MARKET_LATENCY,		-1);
		add(Keys.QUOTE_LATENCY,			-1);
		add(Keys.TRANSACTION_LATENCY,	-1);		

		add(Keys.PRICING_POLICY,		0.5);
		add(Keys.CLEAR_FREQ,			1000);
		
		// Agent-level defaults
		add(Keys.ARRIVAL_RATE,			0.075);
		add(Keys.REENTRY_RATE,			0.005);
		// Agent defaults by role
		addBackgroundAgentDefaults();
		addMarketMakerDefaults();
		addHFTAgentDefaults();
	}
	
	
	private static void addHFTAgentDefaults() {
		add(Keys.LA_LATENCY, 			-1);
		add(Keys.ALPHA, 				0.001);
	}
	
	private static void addBackgroundAgentDefaults() {
		add(Keys.BACKGROUND_REENTRY_RATE, Keys.REENTRY_RATE);
		
		add(Keys.PRIVATE_VALUE_VAR,		1000000);
		add(Keys.MAX_QUANTITY, 			10);
		add(Keys.BID_RANGE_MIN, 		0);
		add(Keys.BID_RANGE_MAX, 		5000); 
		add(Keys.WINDOW_LENGTH, 		5000);
		
		add(Keys.WITHDRAW_ORDERS, 		true);	// for ZIRs
		
		add(Keys.ACCEPTABLE_PROFIT_FRACTION, 0.8);	// for ZIRPs
		
		addAAAgentDefaults();
		addZIPAgentDefaults();
	}
	
	private static void addAAAgentDefaults() {
		add(Keys.AGGRESSION, 			0);
		add(Keys.THETA, 				-4);
		add(Keys.THETA_MIN, 			-8);
		add(Keys.THETA_MAX, 			2);
		add(Keys.ETA, 					3);
		add(Keys.LAMBDA_R, 				0.05);
		add(Keys.LAMBDA_A, 				0.02);	// 0.02 in paper 
		add(Keys.GAMMA, 				2);
		add(Keys.BETA_R, 				0.4); 	// or U[0.2, 0.6] 
		add(Keys.BETA_T, 				0.4); 	// or U[0.2, 0.6] 
		add(Keys.BUYER_STATUS, 			true);
		add(Keys.DEBUG, 				false);
	}
	
	private static void addZIPAgentDefaults() {
		add(Keys.MARGIN_MIN, 			0.05);
		add(Keys.MARGIN_MAX, 			0.35);
		add(Keys.GAMMA_MIN, 			0);
		add(Keys.GAMMA_MAX, 			0.1);
		add(Keys.BETA_MIN, 				0.1);
		add(Keys.BETA_MAX, 				0.5);
		add(Keys.COEFF_A, 				0.05); 
		add(Keys.COEFF_R, 				0.05);
	}
	
	
	private static void addMarketMakerDefaults() {
		add(Keys.MARKETMAKER_REENTRY_RATE, Keys.REENTRY_RATE);
		
		add(Keys.NUM_RUNGS, 			100);
		add(Keys.RUNG_SIZE, 			1000);
		add(Keys.TRUNCATE_LADDER, 		true);
		add(Keys.TICK_IMPROVEMENT, 		true);
		add(Keys.TICK_OUTSIDE, 			false);
		add(Keys.INITIAL_LADDER_MEAN, 	getAsInt(Keys.FUNDAMENTAL_MEAN));
		add(Keys.INITIAL_LADDER_RANGE, 	1000);
		
		// MAMM
		add(Keys.NUM_HISTORICAL, 		5);
		
		// WMAMM
		add(Keys.WEIGHT_FACTOR, 		0);
		
		// AdaptiveMM
		add(Keys.SPREADS, 				new int[]{500,1000,2500,5000});
		add(Keys.USE_MEDIAN_SPREAD, 	false); // XXX get rid of this altogether?
		add(Keys.MOVING_AVERAGE_PRICE, 	true);
		add(Keys.FAST_LEARNING, 		true);
		add(Keys.USE_LAST_PRICE, 		true);
	}
	
	
	
	public static boolean hasKey(String key) {
		return defaults.containsKey(key);
	}
	
	
	private static void add(String key, String str) {
		defaults.put(key, str);
	}
	
	private static void add(String key, boolean value) {
		defaults.put(key, Boolean.toString(value));
	}
	
	private static void add(String key, long value) {
		defaults.put(key, Long.toString(value));
	}
	
	private static void add(String key, int value) {
		defaults.put(key, Integer.toString(value));
	}
	
	private static void add(String key, int[] values) {
		StringBuilder str = new StringBuilder();
		for (int i : values) str.append(i).append(Consts.DELIMITER);
		str.deleteCharAt(str.length() - 1);
		defaults.put(key, str.toString());
	}
	
	private static void add(String key, double value) {
		defaults.put(key, Double.toString(value));
	}
	
	@SuppressWarnings("unused")
	private static void add(String key, float value) {
		defaults.put(key, Float.toString(value));
	}
	
	
	
	public static String getAsString(String key) {
		if (!hasKey(key)) throw new IllegalArgumentException("Unknown key " + key);
		return defaults.get(key);
	}
	
	public static boolean getAsBoolean(String key) {
		if (!hasKey(key)) throw new IllegalArgumentException("Unknown key " + key);
		return Boolean.parseBoolean(defaults.get(key));
	}
	
	public static long getAsLong(String key) {
		if (!hasKey(key)) throw new IllegalArgumentException("Unknown key " + key);
		return Long.parseLong(defaults.get(key));
	}
	
	public static int getAsInt(String key) {
		if (!hasKey(key)) throw new IllegalArgumentException("Unknown key " + key);
		return Integer.parseInt(defaults.get(key));
	}
	
	public static int[] getAsIntArray(String key, String delim) {
		if (!hasKey(key)) throw new IllegalArgumentException("Unknown key " + key);
		String[] vals = defaults.get(key).split(delim);
		int[] result = new int[vals.length];
		for(int i = 0; i < vals.length; i++)
			result[i] = Integer.parseInt(vals[i]); 
		return result;
	}
	
	public static double getAsDouble(String key) {
		if (!hasKey(key)) throw new IllegalArgumentException("Unknown key " + key);
		return Double.parseDouble(defaults.get(key));
	}
	
	public static float getAsFloat(String key) {
		if (!hasKey(key)) throw new IllegalArgumentException("Unknown key " + key);
		return Float.parseFloat(defaults.get(key));
	}
	
}
