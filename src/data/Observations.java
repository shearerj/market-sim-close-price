package data;

import event.*;
import model.*;
import entity.*;
import market.*;
import data.*;

// import java.io.BufferedWriter;
// import java.io.File;
// import java.io.FileWriter;
import java.util.HashMap;
import java.util.ArrayList;

import org.json.simple.*;
import org.apache.commons.math3.stat.descriptive.*;
import org.apache.commons.lang3.ArrayUtils;

import systemmanager.Consts;
import systemmanager.SimulationSpec;

/**
 * Contains payoff data and features for all players in the simulation. Computes
 * metrics to output to the observation file.
 * 
 * @author ewah, drhurd
 */
public class Observations {
	
	private SystemData data;
	private HashMap<String, Object> observations;

	// Constants in observation file
	public final static String PLAYERS_KEY = "players";
	public final static String FEATURES_KEY = "features";
	public final static String ROLE_KEY = "role";
	public final static String PAYOFF_KEY = "payoff";
	public final static String STRATEGY_KEY = "strategy";
	public final static String OBS_KEY = "obs";
	
	
	// Descriptors
	public final static String SURPLUS = "surplus";
	public final static String EXECTIME = "exectime";
	public final static String TRANSACTIONS = "trans";
	public final static String ROUTING = "routing";
	public final static String SPREADS = "spreads";
	public final static String PRICEVOL = "pricevol";
	public final static String PRIVATEVALUES = "pv";
	public final static String NUM = "num";
	public final static String MARKET = "mkt";
	public final static String BUYS = "buys";
	public final static String SELLS = "sells";
	public final static String ALL = "all";
	public final static String POSITION = "position";
	public final static String PROFIT = "profit";
	public final static String LIQUIDATION = "liq";
	
	public final static String BACKGROUND = "bkgrd";
	public final static String ENVIRONMENT = "env";
	public final static String HFT = "hft";
	public final static String MARKETMAKER = "mm";
	
	// Prefixes
	public final static String PRICE = "price";
	public final static String QUANTITY = "quantity";
	public final static String PRE = "pre";
	public final static String POST = "post";
	
	// Suffixes
	public final static String AGENTSETUP = "setup";
	public final static String DISCOUNTED = "disc";
	public final static String TOTAL = "total";
	public final static String NBBO = "nbbo";
	

	
	/**
	 * Constructor
	 */
	public Observations(SystemData d) {
		observations = new HashMap<String, Object>();
		data = d;
	}

	/**
	 * Writes observations to the JSON file.
	 */
	public String generateObservationFile() {
		try {
			computeAll();
			return JSONObject.toJSONString(observations);
		} catch (Exception e) {
			System.err.println(this.getClass().getSimpleName() + 
					"::generateObservationFile error");
			e.printStackTrace();
		}
		return null;
	}

	
	/**
	 * Compute all features for observation file.
	 */
	public void computeAll() {
		// log observations for players
		for (Integer id : data.getPlayerIDs()) {
			addObservation(id); // TODO
			// aggregate for roles???
		}
		addFeature("", getConfiguration());
		
		// iterate through all models
		for (MarketModel model : data.getModels().values()) {
			String modelName = model.getLogName() + "_";
			
			// TODO truncation/extraction not working yet
			long maxTime = Math.round(data.getNumEnvAgents() / data.arrivalRate);
			long begTime = Market.quantize((int) maxTime, 500) - 1000;
			for (long i = Math.max(begTime, 500); i <= maxTime + 1000; i += 500) {
				addFeature(modelName + SPREADS + "_" + i, getSpread(model, i));
				addFeature(modelName + PRICEVOL + "_" + i, getVolatility(model, i));
			}
			
			// TODO - how to check that a market maker agent exists?
//			if (model.getNumAgentType("MARKETMAKER") >= 1) {
//			addFeature(prefix + "marketmaker", getMarketMakerInfo(model));
//			}
			
			// DONE
			addFeature(modelName + EXECTIME, getTimeToExecution(model));
			addFeature(modelName + TRANSACTIONS, getTransactionInfo(model));

			// Surplus features (for all values of rho)
			addFeature(modelName + SURPLUS, getSurplus(model));
		
			// TODO - need to remove the position balance hack
			// addFeature(modelName + ROUTING, getRegNMSRoutingInfo(model));
			
		}
		
	}
	
	
	/**
	 * Gets the agent's observation and adds to the HashMap container.
	 * 
	 * @param agentID
	 */
	@SuppressWarnings("unchecked")
	public void addObservation(int agentID) {
		HashMap<String, Object> obs = data.getAgent(agentID).getObservation();

		// Don't add observation if agent is not a player in the game
		// (i.e. not a role) or if observations are empty
		if (obs == null || obs.isEmpty()) return;
		
		// if not a player, then don't add? will aggregate? aggregate only if not player 
		// TODO
		
		// check if list of players has already been inserted in observations file
		if (!observations.containsKey(PLAYERS_KEY)) {
			ArrayList<Object> array = new ArrayList<Object>();
			array.add(obs);
			observations.put(PLAYERS_KEY, array);
		} else {
			((ArrayList<Object>) observations.get(PLAYERS_KEY)).add(obs);
		}
	}

	/**
	 * Adds a feature to the observation file.
	 * 
	 * @param description 	key for list of features to insert
	 * @param ft
	 */
	@SuppressWarnings("unchecked")
	public void addFeature(String description, Feature ft) {
		if (!observations.containsKey(FEATURES_KEY)) {
			HashMap<String,Object> feats = new HashMap<String,Object>();
			feats.put(description.toLowerCase(), ft.get());
			observations.put(FEATURES_KEY, feats);
		} else {
			((HashMap<String,Object>) observations.get(FEATURES_KEY)).put(
					description.toLowerCase(), ft.get());
		}
	}

	
//	/**
//	 * Adds the mean, max, min, sum, var, & med to the feature.
//	 * 
//	 * @param feat
//	 * @param values
//	 * @param suffix
//	 */
//	private void addAllStatistics(HashMap<String, Object> feat,
//			double[] values, String suffix) {
//		if (suffix != null && suffix != "") {
//			suffix = "_" + suffix;
//		}
//		DescriptiveStatistics ds = new DescriptiveStatistics(values);
//		Median med = new Median();
//		if (values.length > 0) {
//			feat.put("mean" + suffix, ds.getMean());
//			feat.put("max" + suffix, ds.getMax());
//			feat.put("min" + suffix, ds.getMin());
//			feat.put("sum" + suffix, ds.getSum());
//			feat.put("var" + suffix, ds.getVariance());
//			feat.put("med" + suffix, med.evaluate(values));
//		} else {
//			feat.put("mean" + suffix, "NaN");
//			feat.put("max" + suffix, "NaN");
//			feat.put("min" + suffix, "NaN");
//			feat.put("sum" + suffix, "NaN");
//			feat.put("var" + suffix, "NaN");
//			feat.put("med" + suffix, "NaN");
//		}
//	}

//	/**
//	 * Adds either {mean, max, min, & var} or {mean, med}, based on mid
//	 * parameter.
//	 * 
//	 * @param feat
//	 * @param values
//	 * @param suffix
//	 *            to append to feature type
//	 * @param mid
//	 *            is true if only add middle-type metrics (e.g. mean/median),
//	 *            false otherwise
//	 */
//	private void addStatistics(HashMap<String, Object> feat, double[] values,
//			String suffix, boolean mid) {
//		if (suffix != null && suffix != "") {
//			suffix = "_" + suffix;
//		}
//		if (values.length > 0) {
//			DescriptiveStatistics dp = new DescriptiveStatistics(values);
//			feat.put("mean" + suffix, dp.getMean());
//			if (mid) {
//				Median med = new Median();
//				feat.put("med" + suffix, med.evaluate(values));
//			}
//			if (!mid) {
//				feat.put("max" + suffix, dp.getMax());
//				feat.put("min" + suffix, dp.getMin());
//				feat.put("var" + suffix, dp.getVariance());
//			}
//		} else {
//			feat.put("mean" + suffix, "NaN");
//			if (mid) {
//				feat.put("med" + suffix, "NaN");
//			}
//			if (!mid) {
//				feat.put("max" + suffix, "NaN");
//				feat.put("min" + suffix, "NaN");
//				feat.put("var" + suffix, "NaN");
//			}
//		}
//	}

//	/**
//	 * Adds mean to the feature hash map.
//	 * 
//	 * @param prefix
//	 * @param feat
//	 * @param values
//	 */
//	private void addMean(String prefix, HashMap<String, Object> feat,
//			double[] values) {
//		if (prefix != null && prefix != "") {
//			prefix = prefix + "_";
//		}
//		if (values.length > 0) {
//			DescriptiveStatistics dp = new DescriptiveStatistics(values);
//			feat.put(prefix + "mean", dp.getMean());
//		} else {
//			feat.put(prefix + "mean", "NaN");
//		}
//	}
//	//Overloaded function
//	private void addMean(String prefix, HashMap<String, Object> feat, ArrayList<Double> values) {
//		if(prefix != null && prefix != "") prefix += "_";
//		if(values.size() > 0) {
//			double[] temp = new double[values.size()];
//			int i = 0;
//			for(Double cur : values) temp[i++] = cur;
//			DescriptiveStatistics dp = new DescriptiveStatistics(temp);
//			feat.put(prefix + "mean", dp.getMean());
//		}
//		else feat.put(prefix + "mean", "Nan");
//	}
//
//	/**
//	 * Adds standard deviation to the feature hash map.
//	 * 
//	 * @param prefix
//	 * @param feat
//	 * @param values
//	 */
//	private void addStdDev(String prefix, HashMap<String, Object> feat,
//			double[] values) {
//		if (prefix != null && prefix != "") {
//			prefix = prefix + "_";
//		}
//		if (values.length > 0) {
//			StandardDeviation sd = new StandardDeviation();
//			feat.put(prefix + "stddev", sd.evaluate(values));
//		} else {
//			feat.put(prefix + "stddev", "NaN");
//>>>>>>> origin/dataOpt
//		}
//	}
	
	

	
	/********************************************
	 * Data aggregation
	 *******************************************/
	
//	/**
//	 * Computes the mean up to numTimeSteps (i.e. from index 0 to
//	 * numTimeSteps-1). If it doesn't exist, returns -1.
//	 * 
//	 * @param values
//	 * @param numTimeSteps
//	 */
//	private double computeMean(double[] values, int numTimeSteps) {
//		if (values.length > 0) {
//			Mean mn = new Mean();
//			if (numTimeSteps < values.length) {
//				return mn.evaluate(values, 0, numTimeSteps);
//			} else {
//				return mn.evaluate();
//			}
//		} else {
//			return -1;
//		}
//	}

//	/**
//	 * Computes the median up to numTimeSteps (i.e. from index 0 to
//	 * numTimeSteps-1). If it doesn't exist, returns -1.
//	 * 
//	 * @param values
//	 * @param numTimeSteps
//	 */
//	private double computeMedian(double[] values, int numTimeSteps) {
//		if (values.length > 0) {
//			Median med = new Median();
//			if (numTimeSteps < values.length) {
//				return med.evaluate(values, 0, numTimeSteps);
//			} else {
//				return med.evaluate(values);
//			}
//		} else {
//			return -1;
//		}
//	}

//	/**
//	 * Computes the standard deviation up to numTimeSteps (i.e. from index 0 to
//	 * numTimeSteps-1). If it doesn't exist, returns -1.
//	 * 
//	 * @param values
//	 * @param numTimeSteps
//	 */
//	private double computeStdDev(double[] values, int numTimeSteps) {
//		if (values.length > 0) {
//			StandardDeviation sd = new StandardDeviation();
//			if (numTimeSteps < values.length) {
//				return sd.evaluate(values, 0, numTimeSteps);
//			} else {
//				return sd.evaluate(values);
//			}
//		} else {
//			return -1;
//		}
//	}
//
//	private double[] convertIntsToArray(HashMap<Integer, Integer> map) {
//		Object[] objs = (new ArrayList<Integer>(map.values())).toArray();
//		double[] values = new double[objs.length];
//		for (int i = 0; i < values.length; i++) {
//			Integer tmp = (Integer) objs[i];
//			values[i] = tmp.doubleValue();
//		}
//		return values;
//	}
//
//	private double[] convertDoublesToArray(HashMap<Integer, Double> map) {
//		Object[] objs = (new ArrayList<Double>(map.values())).toArray();
//		double[] values = new double[objs.length];
//		for (int i = 0; i < values.length; i++) {
//			values[i] = (Double) objs[i];
//		}
//		return values;
//	}
	

//	/**
//	 * Extracts features of a list of times (e.g. intervals, arrival times).
//	 * 
//	 * @param allTimes
//	 *            ArrayList of TimeStamps
//	 * @return
//	 */
//	public HashMap<String, Object> getTimeStampFeatures(
//			ArrayList<TimeStamp> allTimes) {
//		HashMap<String, Object> feat = new HashMap<String, Object>();
//
//		Object[] times = allTimes.toArray();
//		double[] values = new double[times.length];
//		for (int i = 0; i < values.length; i++) {
//			TimeStamp tmp = (TimeStamp) times[i];
//			values[i] = (double) tmp.longValue();
//		}
//		addAllStatistics(feat, values, "");
//		return feat;
//	}


	/**
	 * @return HashMap of configuration parameters.
	 */
	public Feature getConfiguration() {
		Feature config = new Feature();
		config.put(OBS_KEY, data.obsNum);
		config.put(SimulationSpec.SIMULATION_LENGTH, data.simLength);
		config.put(SimulationSpec.TICK_SIZE, data.tickSize);
		config.put(SimulationSpec.LATENCY, data.nbboLatency);
		config.put(SimulationSpec.ARRIVAL_RATE, data.arrivalRate);
		config.put(SimulationSpec.REENTRY_RATE, data.reentryRate);
		config.put(SimulationSpec.FUNDAMENTAL_KAPPA, data.kappa);
		config.put(SimulationSpec.FUNDAMENTAL_MEAN, data.meanValue);
		config.put(SimulationSpec.FUNDAMENTAL_SHOCK_VAR, data.shockVar);
		config.put(SimulationSpec.PRIVATE_VALUE_VAR, data.pvVar);

		for (AgentPropsPair app : data.getEnvAgentMap().keySet()) {
			String agType = app.getAgentType();
			config.put(agType + "_" + NUM, data.getEnvAgentMap().get(app));
			config.put(agType + "_" + AGENTSETUP, 
					app.getProperties().toStrategyString());
		}
		return config;
	}

	
	/**
	 * Execution time metrics.
	 * 
	 * @param model
	 * @return
	 */
	public Feature getTimeToExecution(MarketModel model) {
		Feature feat = new Feature();
		ArrayList<Integer> ids = model.getMarketIDs();
		DescriptiveStatistics speeds = new DescriptiveStatistics();
		for (Integer bidID : data.timeToExecution.keySet()) {
			PQBid b = data.getBid(bidID);
			if (ids.contains(b.getMarketID())) {
				speeds.addValue((double) data.timeToExecution.get(bidID).longValue());
			}
		}
		feat.addMax(speeds);
		feat.addMin(speeds);
		feat.addMean(speeds);
		return feat;
	}
	
	
	/**
	 * Transaction statistics. // TODO depends on how store transaction info
	 * 
	 * @param model
	 * @return
	 */
	public Feature getTransactionInfo(MarketModel model) {
		Feature feat = new Feature();

		HashMap<Integer, PQTransaction> trans = data.getTrans(model.getID());
		feat.put(NUM, trans.size());

		DescriptiveStatistics prices = new DescriptiveStatistics();
		DescriptiveStatistics quantity = new DescriptiveStatistics();
		DescriptiveStatistics fundamental = new DescriptiveStatistics();
		for (PQTransaction tr : trans.values()) {
			prices.addValue(tr.price.getPrice());
			quantity.addValue(tr.quantity);
			fundamental.addValue(data.getFundamentalAt(tr.timestamp).getPrice());
		}
		feat.addMean(PRICE, "", prices);
		feat.addStdDev(PRICE, "", prices);
		feat.addRMSD(prices, fundamental);
		
		for (Integer aid : model.getAgentIDs()) {
			// check if agent is player in role
			if (!data.isNonPlayer(aid)) {
				String type = data.getAgent(aid).getType();

				// count buys/sells
				int buys = 0;
				int sells = 0;
				for (PQTransaction tr : trans.values()) {
					if (tr.sellerID == aid) 	buys++;
					else if (tr.buyerID == aid) sells++;
				}
				// add agentID in case there is >1 of this type
				String suffix = "_" + type.toLowerCase() + aid;
				feat.put(BUYS + suffix, buys);
				feat.put(SELLS + suffix, sells);
				feat.put(TRANSACTIONS + suffix, buys+sells);
			}
		}
		return feat;
	}
	
	
	/**
	 * @return HashMap of order routing info
	 */
	public Feature getRegNMSRoutingInfo(MarketModel model) {
		Feature feat = new Feature();
		
		int numAlt = 0;
		int numMain = 0;
		int numAltTrans = 0;
		int numAltNoTrans = 0;
		int numMainTrans = 0;
		int numMainNoTrans = 0;
		
		for (Integer agentID : model.getAgentIDs()) {
			Agent ag = data.getAgent(agentID);
			// must check that background agent (affected by routing)
			if (ag instanceof BackgroundAgent) {
				BackgroundAgent b = (BackgroundAgent) ag;
				if (b.getMarketID() != b.getMarketIDSubmittedBid()) {
					// order routed to alternate market
					numAlt++;
					// TODO hack to determine if its bid transacted or not
					if (b.getPositionBalance() == 0) numAltNoTrans++;
					else numAltTrans++;
				} else {
					// order was not routed
					numMain++;
					// TODO hack to determine if its bid transacted or not
					if (b.getPositionBalance() == 0) numMainNoTrans++;
					else numMainTrans++;
				}
			}
		}
		
		feat.put("main", numMain);
		// number of orders routed to alternate market
		feat.put("alt", numAlt);
		// number of orders that are routed that transact
		feat.put("alt_transact", numAltTrans);
		// number of orders that are routed that do not transact
		feat.put("alt_notrans", numAltNoTrans);
		// number of orders that are not routed that transact
		feat.put("main_transact", numMainTrans);
		// number of orders that are not routed that do not transact
		feat.put("main_notrans", numMainNoTrans);
		return feat;
	}
	
	
	/**
	 * Computes spread metrics for the given model, for time 0 to maxTime.
	 * 
	 * @param model
	 * @param maxTime
	 * @return
	 */
	public Feature getSpread(MarketModel model, long maxTime) {
		Feature feat = new Feature();

		DescriptiveStatistics medians = new DescriptiveStatistics();
		for (int mktID : model.getMarketIDs()) {
			HashMap<TimeStamp, Double> s = data.marketSpread.get(mktID);
			if (s != null) {
				DescriptiveStatistics spreads = new DescriptiveStatistics(
							ArrayUtils.toPrimitive(s.values().toArray(new Double[s.size()])));
				double med = feat.addMedianUpToTime("", MARKET + (-mktID), 
													spreads, maxTime);
				medians.addValue(med);
			}	
		}
		feat.addMean(medians);
		
		HashMap<TimeStamp, Double> nbbo = data.NBBOSpread.get(model.getID());
		if (nbbo != null) {
			DescriptiveStatistics spreads = new DescriptiveStatistics(
					ArrayUtils.toPrimitive(nbbo.values().toArray(new Double[nbbo.size()])));
			feat.addMedianUpToTime("", NBBO, spreads, maxTime);
		}
		return feat;
	}
	
	
	/**
	 * Computes volatility metrics, for time 0 to maxTime. Volatility is
	 * measured as:
	 * 
	 * - log of std dev of price series (midquote prices of global quotes)
	 * - std dev of log returns (compute over a window over multiple
	 * 	 window sizes) the standard deviation of logarithmic returns.
	 * 
	 * @param model
	 * @param maxTime
	 * @return
	 */
	public Feature getVolatility(MarketModel model,	long maxTime) {
		Feature feat = new Feature();

		DescriptiveStatistics stddev = new DescriptiveStatistics();
		DescriptiveStatistics logpvol = new DescriptiveStatistics();
		for (int mktID : model.getMarketIDs()) {
			HashMap<TimeStamp, Double> mq = data.marketMidQuote.get(mktID);
			
			if (mq != null) {
				DescriptiveStatistics p = new DescriptiveStatistics(
						ArrayUtils.toPrimitive(mq.values().toArray(new Double[mq.size()])));
				double s = feat.addStdDevUpToTime("", MARKET + (-mktID), 
												  p, maxTime);
				stddev.addValue(s);
				double logstddev = s; 
				if (s != 0) {
					logstddev = Math.log(s);
				}
				logpvol.addValue(logstddev);
			}
		}
		// average log price volatility across all markets
		feat.addMean("", "log", logpvol);

		
		// TODO
//		// change sampling frequency of prices
		
		// for windowing, can cycle through & add to DS object as needed
//		for (int i = 0; i < Consts.windows.length; i++) {
//			String prefix = "window" + Consts.windows[i] + "_";
//
//			// TODO
//			
//			double[] logstddevs = new double[ids.size()]; // store std dev of
//															// prices in
//															// multiple markets
//			cnt = 0;
//			for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
//				int mktID = it.next();
//				HashMap<TimeStamp, Double> marketMidQuote = data.marketMidQuote
//						.get(mktID);
//				if (marketMidQuote != null) {
//					// extract all
//					double[] allPrices = extractTimeSeries(marketMidQuote);
//					double stddev = computeWindowStdDev(allPrices,
//							Consts.windows[i], maxTime);
//					if (stddev != -1) {
//						if (stddev != 0) {
//							logstddevs[cnt++] = Math.log(stddev); // log of
//																	// sampled
//																	// price
//																	// volatility
//						} else {
//							logstddevs[cnt++] = stddev; // volatility is 0 (no
//														// price change)
//						}
//					}
//				}
//			}
//			// store price volatility (averaged across all markets in the model)
//			addMean(prefix + "log", feat, logstddevs);
//		}
		return feat;
	}
	
	/**
	 * Track liquidation-related features & profit for market makers.
	 * 
	 * @param model
	 * @return
	 */
	public Feature getMarketMakerInfo(MarketModel model) {
		Feature feat = new Feature();
		for (Integer agentID : model.getAgentIDs()) {
			if (data.getAgent(agentID) instanceof MarketMaker) {
				Agent ag = data.getAgent(agentID);
				// append the agentID in case more than one of this type
				String suffix = Integer.toString(agentID);				
				feat.put(POSITION + "_" + PRE + "_" + LIQUIDATION + suffix, 
						ag.getPreLiquidationPosition());
				// just to double-check, should be 0 position after liquidation
				feat.put(POSITION + "_" + POST + "_" + LIQUIDATION + suffix, 
						ag.getPositionBalance());
				feat.put(PROFIT + "_" + PRE + "_" + LIQUIDATION + suffix, 
						ag.getPreLiquidationProfit());
				feat.put(PROFIT + "_" + POST + "_" + LIQUIDATION + suffix,
						ag.getRealizedProfit());
			}
		}
		return feat;
	}


	/**
	 * Extract discounted surplus for background agents, including those
	 * that are players (in the EGTA use case).
	 * 
	 * @param model
	 * @return
	 */
	public Feature getSurplus(MarketModel model) {
		Feature feat = new Feature();
		
		double rho = 0;
		processSurplus(feat, rho);
		for (int i = 0; i < Consts.rhos.length; i++) {
			rho = Consts.rhos[i];
			if (rho != 0) {
				processSurplus(feat, rho);
			}
		}
		return feat;
	}
	
	/**
	 * @param feat
	 * @param rho
	 */
	public void processSurplus(Feature feat, double rho) {
		String suffix = "";
		if (rho != 0) {
			suffix += "_" + DISCOUNTED + rho;
		}
		
		HashMap<Integer,Double> surplus = data.getSurplusByRho(rho);
		DescriptiveStatistics total = new DescriptiveStatistics(
				ArrayUtils.toPrimitive(surplus.values().toArray(new Double[surplus.size()])));
		feat.addSum("", TOTAL + suffix, total);
		
		// sub-categories for surplus
		DescriptiveStatistics bkgrd = new DescriptiveStatistics();
		DescriptiveStatistics hft = new DescriptiveStatistics();
		DescriptiveStatistics mm = new DescriptiveStatistics();
		DescriptiveStatistics env = new DescriptiveStatistics();
		
		// go through all agents & update for each agent type
		for (Integer agentID : surplus.keySet()) {
			Agent ag = data.getAgent(agentID);
			double val = surplus.get(agentID);
			if (ag instanceof BackgroundAgent) {
				bkgrd.addValue(val);
			} else if (ag instanceof HFTAgent) {
				val = ag.getRealizedProfit();
				hft.addValue(val);
				// TODO will probably have to change if HFT w/ latency
			} else if (ag instanceof MarketMaker) {
				mm.addValue(val);
			}
			
			// Add for environment agents
			if (data.isNonPlayer(agentID)) {
				env.addValue(val);
			}
		}
		
		// Compute statistics
		feat.addSum("", BACKGROUND + suffix, bkgrd);
		feat.addSum("", MARKETMAKER + suffix, mm);
		feat.addSum("", HFT, hft);
		
		// only add if EGTA use case
		if (data.isEGTAUseCase()) {
			feat.addSum("", ENVIRONMENT, env);
		}
	}

//	/**
//	 * Computes the standard deviation by sampling the price every window time
//	 * steps, then calculating the standard deviation on this sampled time
//	 * series.
//	 * 
//	 * @param allPrices
//	 * @param window
//	 * @param maxTime
//	 * @return
//	 */
//	public double computeWindowStdDev(double[] allPrices, int window,
//			long maxTime) {
//		int newSize = (int) Math.floor(allPrices.length / window);
//		double[] sample = new double[newSize - 1];
//		int cnt = 0;
//		for (int i = 1; i < newSize; i++) { // sample at the end of the window,
//											// not the beginning
//			if (allPrices[i * window - 1] != Consts.INF_PRICE) {
//				sample[cnt++] = allPrices[i * window - 1];
//			}
//		}
//		// Reinitialize to get rid of unused portion of array (if any values
//		// undefined)
//		if (cnt != sample.length) {
//			double[] samplePrices = new double[cnt];
//			for (int i = 0; i < cnt; i++) {
//				samplePrices[i] = sample[i];
//			}
//			return computeStdDev(samplePrices, (int) maxTime);
//		}
//		return computeStdDev(sample, (int) maxTime);
//	}

	
//	/**
//	 * Computes depth metrics the given market IDs.
//	 * 
//	 * @param model
//	 * @return
//	 */
//	public HashMap<String, Object> getDepthInfo(MarketModel model) {
//		HashMap<String, Object> feat = new HashMap<String, Object>();
//
//		ArrayList<Integer> ids = model.getMarketIDs();
//
//		for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
//			int mktID = it.next();
//			HashMap<TimeStamp, Double> marketDepth = data.marketDepth
//					.get(mktID);
//			if (marketDepth != null) {
//				double[] depths = extractTimeSeries(marketDepth);
//				addStatistics(feat, depths, "mkt" + (-mktID), true);
//			}
//		}
//		return feat;
//	}
//
//	/**
//	 * Extracts features of a list of times (e.g. intervals, arrival times).
//	 * 
//	 * @param allTimes
//	 *            ArrayList of TimeStamps
//	 * @return
//	 */
//	public HashMap<String, Object> getTimeStampFeatures(
//			ArrayList<TimeStamp> allTimes) {
//		HashMap<String, Object> feat = new HashMap<String, Object>();
//
//		Object[] times = allTimes.toArray();
//		double[] values = new double[times.length];
//		for (int i = 0; i < values.length; i++) {
//			TimeStamp tmp = (TimeStamp) times[i];
//			values[i] = (double) tmp.longValue();
//		}
//		addAllStatistics(feat, values, "");
//		return feat;
//	}
	
//	/**
//	 * Extracts features of a list of prices
//	 * 
//	 * @param allPrices
//	 *            ArrayList of Prices
//	 * @param description
//	 *            is prefix to append to key in map
//	 * @return
//	 */
//	public HashMap<String, Object> getPriceFeatures(ArrayList<Price> allPrices) {
//		HashMap<String, Object> feat = new HashMap<String, Object>();
//
//		Object[] prices = allPrices.toArray();
//		double[] values = new double[prices.length];
//		for (int i = 0; i < values.length; i++) {
//			Price tmp = (Price) prices[i];
//			values[i] = (double) tmp.getPrice();
//		}
//		addAllStatistics(feat, values, "");
//		return feat;
//	}
//
//	
//	/**
//	 * Buckets based on comparison with the performance in the centralized call
//	 * market model. For each non-CentralCall model, checks if a transaction
//	 * occurred in that model and whether or not it occurred in the CentralCall
//	 * model.
//	 * 
//	 * Adds features directly to the Observations object.
//	 */
//	public void addTransactionComparison() {
//
//		// set as baseline the centralized call market model
//		int baseID = 0;
//
//		// hashed by modelID
//		HashMap<Integer, ModelComparison> bucketMap = new HashMap<Integer, ModelComparison>();
//		HashMap<Integer, ArrayList<PQTransaction>> transMap = new HashMap<Integer, ArrayList<PQTransaction>>();
//		for (Map.Entry<Integer, MarketModel> entry : data.getModels()
//				.entrySet()) {
//			MarketModel model = entry.getValue();
//			int modelID = entry.getKey();
//			// Set base (centralized call market) model ID
//			if (model instanceof CentralCall) {
//				baseID = modelID;
//			}
//			transMap.put(modelID, data.getComparableTrans(model.getID()));
//			bucketMap.put(modelID, new ModelComparison());
//		}
//
//		// iterate through all transactions, and identify the model
//		ArrayList<PQTransaction> uniqueTrans = data.getUniqueComparableTrans();
//		for (Iterator<PQTransaction> it = uniqueTrans.iterator(); it.hasNext();) {
//			PQTransaction trans = it.next();
//
//			for (Iterator<Integer> id = data.getModelIDs().iterator(); id
//					.hasNext();) {
//				int modelID = id.next();
//
//				ArrayList<PQTransaction> baseTrans = transMap.get(baseID);
//				ArrayList<PQTransaction> modelTrans = transMap.get(modelID);
//
//				if (modelTrans.contains(trans) && baseTrans.contains(trans)) {
//					bucketMap.get(modelID).YY++;
//				} else if (!modelTrans.contains(trans)
//						&& baseTrans.contains(trans)) {
//					bucketMap.get(modelID).NY++;
//				} else if (modelTrans.contains(trans)
//						&& !baseTrans.contains(trans)) {
//					bucketMap.get(modelID).YN++;
//				} else if (!modelTrans.contains(trans)
//						&& !baseTrans.contains(trans)) {
//					bucketMap.get(modelID).NN++;
//				}
//			}
//
//		}
//		// add as feature
//		for (Iterator<Integer> id = data.getModelIDs().iterator(); id.hasNext();) {
//			int modelID = id.next();
//			String prefix = data.getModel(modelID).getLogName() + "_";
//			HashMap<String, Object> feat = new HashMap<String, Object>();
//
//			ModelComparison mc = bucketMap.get(modelID);
//			feat.put("YY", mc.YY);
//			feat.put("YN", mc.YN);
//			feat.put("NY", mc.NY);
//			feat.put("NN", mc.NN);
//			this.addFeature(prefix + "compare", feat);
//		}
//	}


	/** For now, a junk function - but meant to replace getTransactionInfo and getVolatilityInfo
	 * 
	 * @param model
	 * @param maxTime
	 */
	public void addTransactionData(MarketModel model, long maxTime) {
		//Market Specific Data
		//For each market in the model
		for(int mktID : model.getMarketIDs()) {
			//Getting the transaction data
			ArrayList<PQTransaction> transactions = data.transactionLists.get(mktID);
			//Truncating the time
			DescriptiveStatistics stat = new DescriptiveStatistics(transformData(transactions, maxTime));
		}
	}
	
	/**
	 * Function that takes in an data set and returns a double[] truncated by maxTime
	 * Currently takes ArrayList<PQTransaction> and HashMap<TimeStamp,Double>
	 * @param series
	 * @param maxTime
	 * @return
	 */
	private double[] transformData(ArrayList<PQTransaction> series, long maxTime) {
		int num;
		for(num=0; num < series.size(); ++num) {
			if(series.get(num).timestamp.getLongValue() > maxTime) break;
			++num;
		}
		double[] ret = new double[num];
		for(int i=0; i < num; ++i) {
			ret[i] = series.get(i).price.getPrice();
		}
		
		return ret;
	}
	private double[] transformData(HashMap<TimeStamp,Double> series, long maxTime) {
		int num = 0;
		for(TimeStamp ts : series.keySet()) {
			if(ts.getLongValue() > maxTime) break;
			num++;
		}
		double[] ret = new double[num];
		int i = 0;
		for(TimeStamp ts : series.keySet()) {
			ret[i++] = series.get(ts);
		}
		return ret;
	}
}
