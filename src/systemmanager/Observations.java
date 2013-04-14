package systemmanager;

import event.*;
import model.*;
import entity.*;
import market.*;

// import java.io.BufferedWriter;
// import java.io.File;
// import java.io.FileWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.json.simple.*;
import org.apache.commons.math3.stat.descriptive.*;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.*;

/**
 * Contains payoff data and features for all players in the simulation. Computes
 * metrics to output to the observation file.
 * 
 * @author ewah
 */
public class Observations {

	private HashMap<String, Object> observations;
	private SystemData data;

	// Constants in observation file
	public final static String PLAYERS_KEY = "players";
	public final static String FEATURES_KEY = "features";
	public final static String ROLE_KEY = "role";
	public final static String PAYOFF_KEY = "payoff";
	public final static String STRATEGY_KEY = "strategy";
	
	/**
	 * Constructor
	 */
	public Observations(SystemData d) {
		observations = new HashMap<String, Object>();
		data = d;
	}

	/**
	 * Gets the agent's observation and adds to the HashMap container.
	 * 
	 * @param agentID
	 */
	public void addObservation(int agentID) {
		HashMap<String, Object> obs = data.getAgent(agentID).getObservation();

		// Don't add observation if agent is not a player in the game (i.e. not
		// a role)
		// or if observations are empty
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
	 * Used to put observation as feature in the case of players not in the
	 * primary model.
	 * 
	 * @param agentID
	 * @return
	 */
	public void addObservationAsFeature(int agentID) {
		HashMap<String, Object> obs = data.getAgent(agentID).getObservation();

		if (obs == null || obs.isEmpty())
			return;
		else {
			Agent ag = data.getAgent(agentID);
			String s = ag.getModel().getLogName() + "_" + ag.getType()
					+ ag.getLogID();
			addFeature(s, obs);
		}
	}

	/**
	 * Adds a feature (for entire simulation, not just one player).
	 * 
	 * @param description
	 * @param ft
	 */
	public void addFeature(String description, HashMap<String,Object> ft) {
		if (!observations.containsKey(FEATURES_KEY)) {
			HashMap<String,Object> feats = new HashMap<String,Object>();
			feats.put(description.toLowerCase(), ft);
			observations.put(FEATURES_KEY, feats);
		} else {
			((HashMap<String,Object>) observations.get(FEATURES_KEY)).put(
					description.toLowerCase(), ft);
		}
	}

	/**
	 * Adds the mean, max, min, sum, var, & med to the feature.
	 * 
	 * @param feat
	 * @param values
	 * @param suffix
	 */
	private void addAllStatistics(HashMap<String, Object> feat,
			double[] values, String suffix) {
		if (suffix != null && suffix != "") {
			suffix = "_" + suffix;
		}
		DescriptiveStatistics ds = new DescriptiveStatistics(values);
		Median med = new Median();
		if (values.length > 0) {
			feat.put("mean" + suffix, ds.getMean());
			feat.put("max" + suffix, ds.getMax());
			feat.put("min" + suffix, ds.getMin());
			feat.put("sum" + suffix, ds.getSum());
			feat.put("var" + suffix, ds.getVariance());
			feat.put("med" + suffix, med.evaluate(values));
		} else {
			feat.put("mean" + suffix, "NaN");
			feat.put("max" + suffix, "NaN");
			feat.put("min" + suffix, "NaN");
			feat.put("sum" + suffix, "NaN");
			feat.put("var" + suffix, "NaN");
			feat.put("med" + suffix, "NaN");
		}
	}

	/**
	 * Adds either {mean, max, min, & var} or {mean, med}, based on mid
	 * parameter.
	 * 
	 * @param feat
	 * @param values
	 * @param suffix
	 *            to append to feature type
	 * @param mid
	 *            is true if only add middle-type metrics (e.g. mean/median),
	 *            false otherwise
	 */
	private void addStatistics(HashMap<String, Object> feat, double[] values,
			String suffix, boolean mid) {
		if (suffix != null && suffix != "") {
			suffix = "_" + suffix;
		}
		if (values.length > 0) {
			DescriptiveStatistics dp = new DescriptiveStatistics(values);
			feat.put("mean" + suffix, dp.getMean());
			if (mid) {
				Median med = new Median();
				feat.put("med" + suffix, med.evaluate(values));
			}
			if (!mid) {
				feat.put("max" + suffix, dp.getMax());
				feat.put("min" + suffix, dp.getMin());
				feat.put("var" + suffix, dp.getVariance());
			}
		} else {
			feat.put("mean" + suffix, "NaN");
			if (mid) {
				feat.put("med" + suffix, "NaN");
			}
			if (!mid) {
				feat.put("max" + suffix, "NaN");
				feat.put("min" + suffix, "NaN");
				feat.put("var" + suffix, "NaN");
			}
		}
	}

	/**
	 * Adds mean to the feature hash map.
	 * 
	 * @param prefix
	 * @param feat
	 * @param values
	 */
	private void addMean(String prefix, HashMap<String, Object> feat,
			double[] values) {
		if (prefix != null && prefix != "") {
			prefix = prefix + "_";
		}
		if (values.length > 0) {
			DescriptiveStatistics dp = new DescriptiveStatistics(values);
			feat.put(prefix + "mean", dp.getMean());
		} else {
			feat.put(prefix + "mean", "NaN");
		}
	}

	/**
	 * Adds standard deviation to the feature hash map.
	 * 
	 * @param prefix
	 * @param feat
	 * @param values
	 */
	private void addStdDev(String prefix, HashMap<String, Object> feat,
			double[] values) {
		if (prefix != null && prefix != "") {
			prefix = prefix + "_";
		}
		if (values.length > 0) {
			StandardDeviation sd = new StandardDeviation();
			feat.put(prefix + "stddev", sd.evaluate(values));
		} else {
			feat.put(prefix + "stddev", "NaN");
		}
	}

	/**
	 * Computes the mean up to numTimeSteps (i.e. from index 0 to
	 * numTimeSteps-1). If it doesn't exist, returns -1.
	 * 
	 * @param values
	 * @param numTimeSteps
	 */
	private double computeMean(double[] values, int numTimeSteps) {
		if (values.length > 0) {
			Mean mn = new Mean();
			if (numTimeSteps < values.length) {
				return mn.evaluate(values, 0, numTimeSteps);
			} else {
				return mn.evaluate();
			}
		} else {
			return -1;
		}
	}

	/**
	 * Computes the median up to numTimeSteps (i.e. from index 0 to
	 * numTimeSteps-1). If it doesn't exist, returns -1.
	 * 
	 * @param values
	 * @param numTimeSteps
	 */
	private double computeMedian(double[] values, int numTimeSteps) {
		if (values.length > 0) {
			Median med = new Median();
			if (numTimeSteps < values.length) {
				return med.evaluate(values, 0, numTimeSteps);
			} else {
				return med.evaluate(values);
			}
		} else {
			return -1;
		}
	}

	/**
	 * Computes the standard deviation up to numTimeSteps (i.e. from index 0 to
	 * numTimeSteps-1). If it doesn't exist, returns -1.
	 * 
	 * @param values
	 * @param numTimeSteps
	 */
	private double computeStdDev(double[] values, int numTimeSteps) {
		if (values.length > 0) {
			StandardDeviation sd = new StandardDeviation();
			if (numTimeSteps < values.length) {
				return sd.evaluate(values, 0, numTimeSteps);
			} else {
				return sd.evaluate(values);
			}
		} else {
			return -1;
		}
	}

	private double[] convertIntsToArray(HashMap<Integer, Integer> map) {
		Object[] objs = (new ArrayList<Integer>(map.values())).toArray();
		double[] values = new double[objs.length];
		for (int i = 0; i < values.length; i++) {
			Integer tmp = (Integer) objs[i];
			values[i] = tmp.doubleValue();
		}
		return values;
	}

	private double[] convertDoublesToArray(HashMap<Integer, Double> map) {
		Object[] objs = (new ArrayList<Double>(map.values())).toArray();
		double[] values = new double[objs.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = (Double) objs[i];
		}
		return values;
	}

	/**
	 * Extracts surplus features for all agents in a given model.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String, Object> getSurplusFeatures(MarketModel model) {
		HashMap<String, Object> feat = new HashMap<String, Object>();
		int modelID = model.getID();

		int totalSurplus = 0;

		// // TODO: have it call discounted surplus with rho=0
		// HashMap<Integer,Double> discSurplus =
		// data.getDiscountedSurplus(modelID, rho);
		// double[] vals = convertDoublesToArray(discSurplus);
		// DescriptiveStatistics ds = new DescriptiveStatistics(vals);
		// feat.put((new Double(rho)).toString(), ds.getSum());

		// get background surplus for all agents with a private value
		HashMap<Integer, Integer> bkgrdSurplus = data.getBackgroundSurplus(modelID);
		double[] values = convertIntsToArray(bkgrdSurplus);
		addAllStatistics(feat, values, "bkgrd");
		DescriptiveStatistics ds = new DescriptiveStatistics(values);
		totalSurplus += ds.getSum();

		// add role (HFT) agent surplus/payoff
		for (Iterator<Integer> it = model.getAgentIDs().iterator(); it
				.hasNext();) {
			int aid = it.next();
			Agent a = data.getAgent(aid);
			String type = a.getType();
			String label = "sum_with_" + type.toLowerCase();

			// Append the agentID if there is 1+ of this type in the simulation
//			if (model.getNumAgentType(type) > 1) {
				label += a.getLogID();
//			}
			

			int surplus = 0;
			if (!data.isNonPlayer(aid)) {
				// HFT agent
				surplus = a.getRealizedProfit();
				feat.put(label, ds.getSum() + surplus);
				totalSurplus += surplus;
			} else if (data.isNonPlayer(aid) && data.getAgent(aid).getPrivateValue() == -1) {
				// background agent with undefined private value (e.g. MarketMaker)
				surplus = data.getSurplusForAgent(modelID, aid);
				feat.put(label, ds.getSum() + surplus);
				feat.put("profit_" + type.toLowerCase() + a.getLogID(), surplus);
				totalSurplus += surplus;
			} // otherwise already included in background surplus
		}
		feat.put("sum_total", totalSurplus);
		return feat;
	}

	/**
	 * Extracts discounted surplus values (only for background agents)
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String, Object> getDiscountedSurplusFeatures(
			MarketModel model) {
		HashMap<String, Object> feat = new HashMap<String, Object>();
		int modelID = model.getID();

		// total discounted surplus for varying values of rho
		for (int i = 0; i < Consts.rhos.length; i++) {
			double rho = Consts.rhos[i];
			HashMap<Integer, Double> discSurplus = data.getDiscountedSurplus(
					modelID, rho);
			double[] vals = convertDoublesToArray(discSurplus);
			DescriptiveStatistics ds = new DescriptiveStatistics(vals);
			feat.put((new Double(rho)).toString(), ds.getSum());
		}
		return feat;
	}

	/**
	 * Extracts features of a list of times (e.g. intervals, arrival times).
	 * 
	 * @param allTimes
	 *            ArrayList of TimeStamps
	 * @return
	 */
	public HashMap<String, Object> getTimeStampFeatures(
			ArrayList<TimeStamp> allTimes) {
		HashMap<String, Object> feat = new HashMap<String, Object>();

		Object[] times = allTimes.toArray();
		double[] values = new double[times.length];
		for (int i = 0; i < values.length; i++) {
			TimeStamp tmp = (TimeStamp) times[i];
			values[i] = (double) tmp.longValue();
		}
		addAllStatistics(feat, values, "");
		return feat;
	}

	/**
	 * Extracts features of a list of times
	 * 
	 * @param allPrices
	 *            ArrayList of Prices
	 * @param description
	 *            is prefix to append to key in map
	 * @return
	 */
	public HashMap<String, Object> getPriceFeatures(ArrayList<Price> allPrices) {
		HashMap<String, Object> feat = new HashMap<String, Object>();

		Object[] prices = allPrices.toArray();
		double[] values = new double[prices.length];
		for (int i = 0; i < values.length; i++) {
			Price tmp = (Price) prices[i];
			values[i] = (double) tmp.getPrice();
		}
		addAllStatistics(feat, values, "");
		return feat;
	}

	/**
	 * Computes execution speed metrics for a given model.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String, Object> getTimeToExecution(MarketModel model) {
		HashMap<String, Object> feat = new HashMap<String, Object>();

		ArrayList<Integer> ids = model.getMarketIDs();

		// Initialize with maximum number of possible bids
		double[] values = new double[data.timeToExecution.size()];
		int cnt = 0;
		for (Map.Entry<Integer, TimeStamp> entry : data.timeToExecution
				.entrySet()) {
			int bidID = entry.getKey();
			PQBid b = data.getBid(bidID);
			if (ids.contains(new Integer(b.getMarketID()))) {
				values[cnt] = (double) entry.getValue().longValue();
				cnt++;
			}
		}
		// Reinitialize to get rid of unused portion of array
		double[] speeds = new double[cnt];
		for (int i = 0; i < cnt; i++) {
			speeds[i] = values[i];
		}
		addStatistics(feat, speeds, "", false);

		return feat;
	}

	/**
	 * Computes statistical values on the transaction data for a given model.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String, Object> getTransactionInfo(MarketModel model) {
		HashMap<String, Object> feat = new HashMap<String, Object>();

		HashMap<Integer, PQTransaction> trans = data.getTrans(model.getID());
		feat.put("num", trans.size());

		double[] prices = new double[trans.size()];
		double[] quantities = new double[trans.size()];
		double[] fundamentalVals = new double[trans.size()];
		int i = 0;
		for (PQTransaction tr : trans.values()) {
			prices[i] = tr.price.getPrice();
			quantities[i] = tr.quantity;
			fundamentalVals[i] = data.getFundamentalAt(tr.timestamp).getPrice();
			i++;
		}
		addStatistics(feat, prices, "price", false);
		// addSomeStatistics(feat,quantities,"qty",false);
		
		// add root mean square deviation measure
		feat.put("rmsd", computeRMSD(prices, fundamentalVals));
		
		for (Iterator<Integer> it = model.getAgentIDs().iterator(); it.hasNext();) {
			int aid = it.next();
			// check if agent is player in role
			if (!data.isNonPlayer(aid)) {
				String type = data.getAgent(aid).getType();

				// count buys/sells
				int buys = 0;
				int sells = 0;
				for (PQTransaction tr : trans.values()) {
					if (tr.sellerID == aid) {
						buys++;
					} else if (tr.buyerID == aid) {
						sells++;
					}
				}
				// must append the agentID if there is more than one of this
				// type
				String suffix = "_" + type.toLowerCase();
//				if (model.getNumAgentType(type) > 1) {
					suffix += aid;
//				}
				feat.put("buys" + suffix, buys);
				feat.put("sells" + suffix, sells);
			}
		}
		return feat;
	}

	/**
	 * Computes spread metrics for the given model, for time 0 to maxTime.
	 * 
	 * @param model
	 * @param maxTime
	 * @return
	 */
	public HashMap<String, Object> getSpreadInfo(MarketModel model, long maxTime) {
		HashMap<String, Object> feat = new HashMap<String, Object>();

		ArrayList<Integer> ids = model.getMarketIDs();
		double[] meds = new double[ids.size()]; // store medians to be averaged
		int cnt = 0;
		for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
			int mktID = it.next();
			HashMap<TimeStamp, Double> marketSpread = data.marketSpread
					.get(mktID);
			if (marketSpread != null) {
				double[] spreads = truncateTimeSeries(marketSpread, maxTime);
				addStatistics(feat, spreads, "mkt" + (-mktID), true);

				// add model-level statistics (average the median spreads)
				double med = computeMedian(spreads, spreads.length);
				if (med != -1) {
					meds[cnt] = med;
					cnt++; // at most is the number of markets in the model
				}
			}
		}
		// Reinitialize to get rid of unused portion of array
		double[] medians = new double[cnt];
		for (int i = 0; i < cnt; i++) {
			medians[i] = meds[i];
		}
		// store mean median spread (across all markets in the model)
		addMean("med", feat, medians);

		HashMap<TimeStamp, Double> nbboSpread = data.NBBOSpread.get(model
				.getID());
		double[] nbboSpreads = {};
		if (nbboSpread != null) {
			nbboSpreads = truncateTimeSeries(nbboSpread, maxTime);
		}
		addStatistics(feat, nbboSpreads, "nbbo", true);

		return feat;
	}

	/**
	 * Computes depth metrics the given market IDs. // TODO - remove depth
	 * measures?
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String, Object> getDepthInfo(MarketModel model) {
		HashMap<String, Object> feat = new HashMap<String, Object>();

		ArrayList<Integer> ids = model.getMarketIDs();

		for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
			int mktID = it.next();
			HashMap<TimeStamp, Double> marketDepth = data.marketDepth
					.get(mktID);
			if (marketDepth != null) {
				double[] depths = extractTimeSeries(marketDepth);
				addStatistics(feat, depths, "mkt" + (-mktID), true);
			}
		}
		return feat;
	}

	/**
	 * Computes volatility metrics, for time 0 to maxTime. Volatility is
	 * measured as: - log of std dev of price series (midquote prices of global
	 * quotes) - std dev of log returns (compute over a window over multiple
	 * window sizes) the standard deviation of logarithmic returns.
	 * 
	 * @param model
	 * @param maxTime
	 * @return
	 */
	public HashMap<String, Object> getVolatilityInfo(MarketModel model,
			long maxTime) {
		HashMap<String, Object> feat = new HashMap<String, Object>();

		ArrayList<Integer> ids = model.getMarketIDs();

		// get volatility for all prices in a market model (based on global
		// quote)
		double[] vol = new double[ids.size()]; // store std dev of prices in
												// multiple markets
		int cnt = 0;
		for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
			int mktID = it.next();
			HashMap<TimeStamp, Double> marketMidQuote = data.marketMidQuote
					.get(mktID);
			if (marketMidQuote != null) {
				// truncated time series does not include any undefined prices
				// at beginning
				// and will cut off at maxTime into the simulation
				double[] prices = truncateTimeSeries(marketMidQuote, maxTime);
				addStdDev("mkt" + (-mktID), feat, prices);

				// add model-level statistics (average the log of std devs of
				// prices)
				double std = computeStdDev(prices, (int) maxTime);
				if (std != -1) {
					if (std != 0) {
						vol[cnt++] = Math.log(std); // log of price volatility
					} else {
						vol[cnt++] = std; // volatility is 0 (no price change)
					}
				}
			}
		}
		// store price volatility (averaged across all markets in the model)
		addMean("log", feat, vol);

		// change sampling frequency of prices
		for (int i = 0; i < Consts.windows.length; i++) {
			String prefix = "window" + Consts.windows[i] + "_";

			double[] logstddevs = new double[ids.size()]; // store std dev of
															// prices in
															// multiple markets
			cnt = 0;
			for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
				int mktID = it.next();
				HashMap<TimeStamp, Double> marketMidQuote = data.marketMidQuote
						.get(mktID);
				if (marketMidQuote != null) {
					// extract all
					double[] allPrices = extractTimeSeries(marketMidQuote);
					double stddev = computeWindowStdDev(allPrices,
							Consts.windows[i], maxTime);
					if (stddev != -1) {
						if (stddev != 0) {
							logstddevs[cnt++] = Math.log(stddev); // log of
																	// sampled
																	// price
																	// volatility
						} else {
							logstddevs[cnt++] = stddev; // volatility is 0 (no
														// price change)
						}
					}
				}
			}
			// store price volatility (averaged across all markets in the model)
			addMean(prefix + "log", feat, logstddevs);
		}
		return feat;
	}

	/**
	 * Computes the standard deviation by sampling the price every window time
	 * steps, then calculating the standard deviation on this sampled time
	 * series.
	 * 
	 * @param allPrices
	 * @param window
	 * @param maxTime
	 * @return
	 */
	public double computeWindowStdDev(double[] allPrices, int window,
			long maxTime) {
		int newSize = (int) Math.floor(allPrices.length / window);
		double[] sample = new double[newSize - 1];
		int cnt = 0;
		for (int i = 1; i < newSize; i++) { // sample at the end of the window,
											// not the beginning
			if (allPrices[i * window - 1] != Consts.INF_PRICE) {
				sample[cnt++] = allPrices[i * window - 1];
			}
		}
		// Reinitialize to get rid of unused portion of array (if any values
		// undefined)
		if (cnt != sample.length) {
			double[] samplePrices = new double[cnt];
			for (int i = 0; i < cnt; i++) {
				samplePrices[i] = sample[i];
			}
			return computeStdDev(samplePrices, (int) maxTime);
		}
		return computeStdDev(sample, (int) maxTime);
	}

	/**
	 * Track liquidation-related features & profit for market makers.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String, Object> getMarketMakerInfo(MarketModel model) {
		HashMap<String, Object> feat = new HashMap<String, Object>();

		ArrayList<Integer> ids = model.getAgentIDs();
		for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
			int agentID = it.next();
			
			if (data.getAgent(agentID) instanceof MarketMaker) {
				Agent ag = data.getAgent(agentID);

				// must append the agentID if there is more than one of this type
				String suffix = Integer.toString(agentID);				
				feat.put("position_pre_liq" + suffix, ag.getPreLiquidationPosition());
				// just to double-check, should be 0 position after liquidation
				feat.put("position_post_liq" + suffix, ag.getPositionBalance());
				feat.put("profit_pre_liq" + suffix, ag.getPreLiquidationProfit());
				feat.put("profit_post_liq" + suffix, ag.getRealizedProfit());
			}
		}

		return feat;
	}

	/**
	 * Buckets based on comparison with the performance in the centralized call
	 * market model. For each non-CentralCall model, checks if a transaction
	 * occurred in that model and whether or not it occurred in the CentralCall
	 * model.
	 * 
	 * Adds features directly to the Observations object.
	 */
	public void addTransactionComparison() {

		// set as baseline the centralized call market model
		int baseID = 0;

		// hashed by modelID
		HashMap<Integer, ModelComparison> bucketMap = new HashMap<Integer, ModelComparison>();
		HashMap<Integer, ArrayList<PQTransaction>> transMap = new HashMap<Integer, ArrayList<PQTransaction>>();
		for (Map.Entry<Integer, MarketModel> entry : data.getModels()
				.entrySet()) {
			MarketModel model = entry.getValue();
			int modelID = entry.getKey();
			// Set base (centralized call market) model ID
			if (model instanceof CentralCall) {
				baseID = modelID;
			}
			transMap.put(modelID, data.getComparableTrans(model.getID()));
			bucketMap.put(modelID, new ModelComparison());
		}

		// iterate through all transactions, and identify the model
		ArrayList<PQTransaction> uniqueTrans = data.getUniqueComparableTrans();
		for (Iterator<PQTransaction> it = uniqueTrans.iterator(); it.hasNext();) {
			PQTransaction trans = it.next();

			for (Iterator<Integer> id = data.getModelIDs().iterator(); id
					.hasNext();) {
				int modelID = id.next();

				ArrayList<PQTransaction> baseTrans = transMap.get(baseID);
				ArrayList<PQTransaction> modelTrans = transMap.get(modelID);

				if (modelTrans.contains(trans) && baseTrans.contains(trans)) {
					bucketMap.get(modelID).YY++;
				} else if (!modelTrans.contains(trans)
						&& baseTrans.contains(trans)) {
					bucketMap.get(modelID).NY++;
				} else if (modelTrans.contains(trans)
						&& !baseTrans.contains(trans)) {
					bucketMap.get(modelID).YN++;
				} else if (!modelTrans.contains(trans)
						&& !baseTrans.contains(trans)) {
					bucketMap.get(modelID).NN++;
				}
			}

		}
		// add as feature
		for (Iterator<Integer> id = data.getModelIDs().iterator(); id.hasNext();) {
			int modelID = id.next();
			String prefix = data.getModel(modelID).getLogName() + "_";
			HashMap<String, Object> feat = new HashMap<String, Object>();

			ModelComparison mc = bucketMap.get(modelID);
			feat.put("YY", mc.YY);
			feat.put("YN", mc.YN);
			feat.put("NY", mc.NY);
			feat.put("NN", mc.NN);
			this.addFeature(prefix + "compare", feat);
		}
	}

	/**
	 * Construct a double array storing a time series from a HashMap of integers
	 * hashed by TimeStamp. Starts recording at first time step, even if value
	 * undefined. The double array returned will have size same as the
	 * simulation length.
	 * 
	 * @param map
	 * @return
	 */
	private double[] extractTimeSeries(HashMap<TimeStamp, Double> map) {

		// Have to sort the TimeStamps since not necessarily sorted in HashMap
		TreeSet<TimeStamp> times = new TreeSet<TimeStamp>();
		ArrayList<TimeStamp> keys = new ArrayList<TimeStamp>(map.keySet());
		for (Iterator<TimeStamp> i = keys.iterator(); i.hasNext();) {
			TimeStamp t = i.next();
			if (t != null)
				times.add(t);
		}

		int cnt = 0;
		TimeStamp prevTime = null;
		double[] vals = new double[(int) data.simLength.longValue()];
		for (Iterator<TimeStamp> it = times.iterator(); it.hasNext();) {
			if (prevTime == null) {
				// if prevTime has not been defined yet, set as the first time
				// where spread measured
				prevTime = it.next();
				for (int i = 0; i < prevTime.longValue(); i++) {
					vals[cnt++] = map.get(prevTime);
				}
			} else {
				// next Time is the next time at which to extend the time series
				TimeStamp nextTime = it.next();
				// fill in the vals array, even if not undefined
				for (int i = (int) prevTime.longValue(); i < nextTime
						.longValue(); i++) {
					vals[cnt++] = map.get(prevTime);
				}
				prevTime = nextTime;
			}
		}
		// fill in to end of array
		if (!prevTime.after(data.simLength)) {
			for (int i = (int) prevTime.longValue(); i < data.simLength
					.longValue(); i++) {
				// get last inserted value and insert
				vals[cnt++] = map.get(prevTime);
			}
		}
		return vals;
	}

	/**
	 * Same as extractTimeSeries, but will remove any undefined values from the
	 * beginning of the array and cut it off at maxTime.
	 * 
	 * @param map
	 * @param maxTime
	 * @return
	 */
	public double[] truncateTimeSeries(HashMap<TimeStamp, Double> map,
			long maxTime) {

		// Have to sort the TimeStamps since not necessarily sorted in HashMap
		TreeSet<TimeStamp> times = new TreeSet<TimeStamp>();
		ArrayList<TimeStamp> keys = new ArrayList<TimeStamp>(map.keySet());
		for (Iterator<TimeStamp> i = keys.iterator(); i.hasNext();) {
			TimeStamp t = i.next();
			if (t != null)
				times.add(t);
		}

		int cnt = 0;
		TimeStamp prevTime = null;
		double[] vals = new double[(int) maxTime];
		for (Iterator<TimeStamp> it = times.iterator(); it.hasNext();) {
			if (prevTime == null) {
				// if prevTime has not been defined yet, set as the first time
				// where value measured
				prevTime = it.next();
			} else {
				// next Time is the next time at which to extend the time series
				TimeStamp nextTime = it.next();
				// fill in the vals array, but only for segments where it is not
				// undefined
				if (map.get(prevTime).intValue() != Consts.INF_PRICE) {
					for (int i = (int) prevTime.longValue(); i < nextTime
							.longValue() && i < maxTime; i++) {
						vals[cnt] = map.get(prevTime); // fill in with prior
														// value, up to maxTime
						cnt++;
					}
				}
				prevTime = nextTime;
			}
		}
		// fill in to end of array
		if (!prevTime.after(new TimeStamp(maxTime))) {
			if (map.get(prevTime).intValue() != Consts.INF_PRICE) {
				for (int i = (int) prevTime.longValue(); i < maxTime; i++) {
					// get last inserted value and insert
					vals[cnt] = map.get(prevTime);
					cnt++;
				}
			}
		}
		// Must resize vals
		double[] valsMod = new double[cnt];
		for (int i = 0; i < valsMod.length; i++) {
			valsMod[i] = vals[i];
		}
		return valsMod;
	}

	/**
	 * @return HashMap of configuration parameters for inclusion in observation.
	 */
	public HashMap<String, Object> getConfiguration() {
		HashMap<String, Object> config = new HashMap<String, Object>();
		config.put("obs", data.obsNum);
		config.put("sim_length", data.simLength);
		config.put("latency", data.nbboLatency);
		config.put("tick_size", data.tickSize);
		config.put("kappa", data.kappa);
		config.put("arrival_rate", data.arrivalRate);
		config.put("mean_value", data.meanValue);
		config.put("shock_var", data.shockVar);
		config.put("pv_var", data.privateValueVar);

		for (Map.Entry<AgentPropsPair, Integer> entry : data.getEnvAgentMap().entrySet()) {
			String agType = entry.getKey().getAgentType();
			config.put(agType + "_num", entry.getValue());
			config.put(agType + "_setup", entry.getKey().getProperties().toStrategyString());
		}
		
		return config;
	}

	
	/**
	 * @return HashMap of order routing info
	 */
	public HashMap<String, Object> getRegNMSRoutingInfo(MarketModel model) {
		HashMap<String, Object> feat = new HashMap<String, Object>();
		
		int numAlt = 0;
		int numMain = 0;
		int numAltTrans = 0;
		int numAltNoTrans = 0;
		int numMainTrans = 0;
		int numMainNoTrans = 0;
		
		ArrayList<Integer> ids = model.getAgentIDs();
		for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
			int agentID = it.next();
			Agent ag = data.getAgent(agentID);
			// must check that not market maker (since not affected by routing)
			if (ag instanceof SMAgent && !(ag instanceof MarketMaker)) {
				SMAgent sm = (SMAgent) ag;
				if (sm.getMarketID() != sm.getMarketIDSubmittedBid()) {
					// order routed to alternate market
					numAlt++;
					// TODO hack to determine if its bid transacted or not
					if (sm.getPositionBalance() == 0)
						numAltNoTrans++;
					else
						numAltTrans++;
				} else {
					// order was not routed
					numMain++;
					// hack to determine if its bid transacted or not
					if (sm.getPositionBalance() == 0)
						numMainNoTrans++;
					else
						numMainTrans++;
				}
			}
		}
		
		feat.put("main", numMain);
		// number of orders routed to alternate market
		feat.put("alt", numAlt);
		// number of orders that are routed that transact?
		feat.put("alt_transact", numAltTrans);
		// number of orders that are routed that do not transact?
		feat.put("alt_notrans", numAltNoTrans);
		// number of orders that are not routed that transact?
		feat.put("main_transact", numMainTrans);
		// number of orders that are not routed that do not transact?
		feat.put("main_notrans", numMainNoTrans);
		
		return feat;
	}
	
	
	
	
	/**
	 * Returns root mean square deviation metric.
	 * 
	 * @param x1
	 * @param x2
	 * @return
	 */
	private double computeRMSD(double [] x1, double [] x2) {
		double rmsd = 0;
		int n = Math.min(x1.length, x2.length);
		// iterate through as many in shorter array
		for (int i = 0; i < n; i++) {
			rmsd += Math.pow(x1[i] - x2[i], 2);	// sum of squared differences
		}
		return Math.sqrt(rmsd / n);
	}
	
	/**
	 * @return HashMap of models/types in the simulation.
	 */
	public HashMap<String, Object> getModelInfo() {

		// TODO
		return null;
	}

	/**
	 * @return HashMap of number of agents of each type in the simulation.
	 */
	public HashMap<String, Object> getAgentInfo() {
		HashMap<String, Object> info = new HashMap<String, Object>();

		// TODO
		return null;

	}

	/**
	 * Writes observations to the JSON file.
	 */
	public String generateObservationFile() {
		try {
			return JSONObject.toJSONString(observations);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
