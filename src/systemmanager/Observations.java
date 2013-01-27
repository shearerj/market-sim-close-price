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
import org.apache.commons.math3.stat.descriptive.rank.*;

/**
 * Contains payoff data and features for all players in the simulation.
 * Computes metrics to output to the observation file.
 *  
 * @author ewah
 */
public class Observations {

	private HashMap<String,Object> observations;
	private SystemData data;
	
	/**
	 * Constructor
	 */
	public Observations(SystemData d) {
		observations = new HashMap<String,Object>();
		data = d;
	}
	
	/**
	 * Gets the agent's observation and adds to the HashMap container.
	 * @param agentID
	 */
	public void addObservation(int agentID) {
		HashMap<String,Object> obs = data.getAgent(agentID).getObservation();
		
		// Don't add observation if agent is not a player in the game (i.e. not a role)
		// or if observations are empty
		if (obs == null || obs.isEmpty()) return;
		
		if (!observations.containsKey("players")) {
			ArrayList<Object> array = new ArrayList<Object>();
			array.add(obs);
			observations.put("players", array);
		} else {
			((ArrayList<Object>) observations.get("players")).add(obs);
		}
	}

	/**
	 * Used to put observation as feature in the case of players not in the primary model.
	 * 
	 * @param agentID
	 * @return
	 */
	public void addObservationAsFeature(int agentID) {
		HashMap<String,Object> obs = data.getAgent(agentID).getObservation();
		
		if (obs == null || obs.isEmpty()) return;
		else {
			Agent ag = data.getAgent(agentID);
			String s = ag.getModel().getLogName() + "_" + ag.getType() + ag.getLogID();
			addFeature(s, obs);
		}
	}
	
	/**
	 * Adds a feature (for entire simulation, not just one player).
	 * @param description
	 * @param ft
	 */
	public void addFeature(String description, HashMap<String,Object> ft) {
		if (!observations.containsKey("features")) {
			HashMap<String,Object> feats = new HashMap<String,Object>();
			feats.put(description.toLowerCase(), ft);
			observations.put("features", feats);
		} else {
			((HashMap<String,Object>) observations.get("features")).put(description.toLowerCase(), ft);
		}
	}
	
	
	/**
	 * Adds the mean, max, min, sum, var, & med to the feature.
	 * 
	 * @param feat
	 * @param values
	 */
	private void addAllStatistics(HashMap<String,Object> feat, double[] values) {
		DescriptiveStatistics ds = new DescriptiveStatistics(values);
		Median med = new Median();
		if (values.length > 0) {
			feat.put("mean", ds.getMean());
			feat.put("max", ds.getMax());
			feat.put("min", ds.getMin());
			feat.put("sum", ds.getSum());
			feat.put("var", ds.getVariance());
			feat.put("med", med.evaluate(values));
		} else {
			feat.put("mean", "NaN");
			feat.put("max", "NaN");
			feat.put("min", "NaN");
			feat.put("sum", "NaN");
			feat.put("var", "NaN");
			feat.put("med", "NaN");
		}
	}
	
	/**
	 * Adds either {mean, max, min, & var} or {mean, med}, based on mid parameter.
	 * 
	 * @param feat
	 * @param values
	 * @param suffix to append to feature type
	 * @param mid is true if only add middle-type metrics (e.g. mean/median), false otherwise
	 */
	private void addStatistics(HashMap<String,Object> feat, double[] values, String suffix, 
			boolean mid) {
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
	 * Adds mean.
	 * 
	 * @param feat
	 * @param values
	 * @param prefix to append to feature type
	 */
	private void addMean(HashMap<String,Object> feat, double[] values, String prefix) {
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
	 * Computes the median. If it doesn't exist, returns -1.
	 * 
	 * @param values
	 */
	private double computeMedian(double[] values) {
		if (values.length > 0) {
			Median med = new Median();
			return med.evaluate(values);
		} else {
			return -1;
		}
	}
	
	private double[] convertIntsToArray(HashMap<Integer,Integer> map) {
		Object[] objs = (new ArrayList<Integer>(map.values())).toArray();
		double[] values = new double[objs.length];
		for (int i = 0; i < values.length; i++) {
	    	Integer tmp = (Integer) objs[i];
			values[i] = tmp.doubleValue();
		}
		return values;
	}
	
	private double[] convertDoublesToArray(HashMap<Integer,Double> map) {
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
	public HashMap<String,Object> getSurplusFeatures(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		int modelID = model.getID();
		
		int totalSurplus = 0;
		
		// get background surplus for all agents with a private value
		HashMap<Integer,Integer> bkgrdSurplus = data.getBackgroundSurplus(modelID);
		double[] values = convertIntsToArray(bkgrdSurplus);
		addAllStatistics(feat,values);
		DescriptiveStatistics ds = new DescriptiveStatistics(values);
		totalSurplus += ds.getSum();
		
		// add role (HFT) agent surplus/payoff
		for (Iterator<Integer> it = model.getAgentIDs().iterator(); it.hasNext(); ) {
			int aid = it.next();
			Agent a = data.getAgent(aid);
			String type = a.getType();
			String label = "sum_with_" + type.toLowerCase();
			
			// Append the agentID if there is 1+ of this type in the simulation
			if (model.getNumAgentType(type) > 1) {
				label += a.getLogID();
			}
			
			int surplus = 0;
			if (!data.isBackgroundAgent(aid)) {
				// HFT agent
				surplus = a.getRealizedProfit();
				feat.put(label, ds.getSum() + surplus);
				totalSurplus += surplus;
			} else if (data.isBackgroundAgent(aid) && data.getAgent(aid).getPrivateValue() == -1) {
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
	 * Extracts discounted surplus values.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String,Object> getDiscountedSurplusFeatures(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		int modelID = model.getID();
		
		// total discounted surplus for varying values of rho
		for (int i = 0; i < Consts.rhos.length; i++) {
			double rho = Consts.rhos[i];
			HashMap<Integer,Double> discSurplus = data.getDiscountedSurplus(modelID, rho);
			double[] vals = convertDoublesToArray(discSurplus);
			DescriptiveStatistics ds = new DescriptiveStatistics(vals);
			feat.put("disc_" +  (new Double(rho)).toString(), ds.getSum());
		}
		return feat;
	}
	
	
	/**
	 * Extracts features of a list of times (e.g. intervals, arrival times).
	 * 
	 * @param allTimes ArrayList of TimeStamps
	 * @return
	 */
	public HashMap<String,Object> getTimeStampFeatures(ArrayList<TimeStamp> allTimes) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		Object[] times = allTimes.toArray();
		double[] values = new double[times.length];  
	    for (int i = 0; i < values.length; i++) {
	    	TimeStamp tmp = (TimeStamp) times[i];
	        values[i] = (double) tmp.longValue();
	    }
		addAllStatistics(feat,values);
		return feat;
	}
	
	
	/**
	 * Extracts features of a list of times
	 * @param allPrices ArrayList of Prices
	 * @param description is prefix to append to key in map
	 * @return
	 */
	public HashMap<String,Object> getPriceFeatures(ArrayList<Price> allPrices) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		Object[] prices = allPrices.toArray();
		double[] values = new double[prices.length];
	    for (int i = 0; i < values.length; i++) {
	    	Price tmp = (Price) prices[i];
	        values[i] = (double) tmp.getPrice();
	    }
	    addAllStatistics(feat,values);
		return feat;
	}


	/**
	 * Computes execution speed metrics for a given model.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String,Object> getExecutionSpeed(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		ArrayList<Integer> ids = model.getMarketIDs();
		
		// Initialize with maximum number of possible bids
		double[] values = new double[data.executionSpeed.size()];
		int cnt = 0;
		for (Map.Entry<Integer, TimeStamp> entry : data.executionSpeed.entrySet()) {
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
		addStatistics(feat,speeds,"",false);
		
		return feat;
	}
	
	
	/**
	 * Computes statistical values on the transaction data for a given model.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String,Object> getTransactionInfo(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
	
		HashMap<Integer,PQTransaction> transactions = data.getTrans(model.getID());
		feat.put("num", transactions.size());

		double[] prices = new double[transactions.size()];
		double[] quantities = new double[transactions.size()];
		int i = 0;
		for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
			PQTransaction trans = entry.getValue();
			prices[i] = trans.price.getPrice();
			quantities[i] = trans.quantity;
			i++;
		}
		addStatistics(feat,prices,"price",false);
		//		addSomeStatistics(feat,quantities,"qty",false);
		for (Iterator<Integer> it = model.getAgentIDs().iterator(); it.hasNext(); ) {
			int aid = it.next();
			// check if agent is player in role
			if (!data.isBackgroundAgent(aid)) {
				String type = data.getAgent(aid).getType();
				
				// count buys/sells
				int buys = 0;
				int sells = 0;
				for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
					PQTransaction trans = entry.getValue();
					if (trans.sellerID == aid) {
						buys++;
					} else if (trans.buyerID == aid) {
						sells++;
					}
				}
				// must append the agentID if there is more than one of this type
				String suffix = "_" + type.toLowerCase();
				if (model.getNumAgentType(type) > 1) {
					suffix += aid;
				}
				feat.put("buys" + suffix, buys);
				feat.put("sells" + suffix, sells);
			}
		}
		return feat;
	}
	
	
	/**
	 * Computes spread metrics for the given model.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String,Object> getSpreadInfo(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
	
		ArrayList<Integer> ids = model.getMarketIDs();
		double[] meds = new double[ids.size()];	// store medians to be averaged
		int cnt = 0;
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			int mktID = it.next();
			HashMap<TimeStamp,Integer> marketSpread = data.marketSpread.get(mktID);
			if (marketSpread != null) {
				double[] spreads = extractTimeSeries(marketSpread);
				addStatistics(feat,spreads,"mkt" + (-mktID),true);
				
				// add model-level statistics
				double med = computeMedian(spreads);
				if (med != -1) {
					meds[cnt] = med;
					cnt++;
				}
			}
		}
		// Reinitialize to get rid of unused portion of array
		double[] medians = new double[cnt];
		for (int i = 0; i < cnt; i++) {
			medians[i] = meds[i];
		}
		// store mean median spread (across all markets in the model)
		addMean(feat,medians,"med");
		
		HashMap<TimeStamp,Integer> nbboSpread = data.NBBOSpread.get(model.getID());
		double[] nbboSpreads = {};
		if (nbboSpread != null) {
			nbboSpreads = extractTimeSeries(nbboSpread);
		}
		addStatistics(feat,nbboSpreads,"nbbo",true);
		
		return feat;
	}
	

	/**
	 * Computes depth metrics the given market IDs. // TODO - remove depth measures?
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String,Object> getDepthInfo(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		ArrayList<Integer> ids = model.getMarketIDs();
		
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			int mktID = it.next();
			HashMap<TimeStamp,Integer> marketDepth = data.marketDepth.get(mktID);
			if (marketDepth != null) {
				double[] depths = extractTimeSeries(marketDepth);
				addStatistics(feat,depths,"mkt" + (-mktID),true);
			}
		}	
		return feat;
	}
	
	
	/**
	 * Computes volatility metrics. Volatility is measured as:
	 * - std dev of price series (midquote prices of global, NBBO quotes)
	 * - std dev of log returns (compute over a window over multiple window sizes)
	 * the standard deviation of logarithmic returns.
	 * TODO - finish
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String, Object> getVolatilityInfo(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		ArrayList<Integer> ids = model.getMarketIDs();
		
		// get volatility for all prices in a market model (based on global quote)
		
		
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			int mktID = it.next();
			// TODO: vol measures. also try multiple periods for determining returns
		}
		return feat;
	}
	
	
	/**
	 * Buckets based on comparison with the performance in the centralized call market
	 * model. For each non-CentralCall model, checks if a transaction occurred in that model
	 * and whether or not it occurred in the CentralCall model.
	 * 
	 * Adds features directly to the Observations object.
	 */
	public void addTransactionComparison() {
				
		// set as baseline the centralized call market model
		int baseID = 0;
		
		// hashed by modelID
		HashMap<Integer, ModelComparison> bucketMap = new HashMap<Integer, ModelComparison>();
		HashMap<Integer, ArrayList<PQTransaction>> transMap = new HashMap<Integer, ArrayList<PQTransaction>>();		
		for (Map.Entry<Integer,MarketModel> entry : data.getModels().entrySet()) {
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
		for (Iterator<PQTransaction> it = uniqueTrans.iterator(); it.hasNext(); ) {
			PQTransaction trans = it.next();
			
			for (Iterator<Integer> id = data.getModelIDs().iterator(); id.hasNext(); ) {
				int modelID = id.next();
				
				ArrayList<PQTransaction> baseTrans = transMap.get(baseID);
				ArrayList<PQTransaction> modelTrans = transMap.get(modelID);
				
				if (modelTrans.contains(trans) && baseTrans.contains(trans)) {
					bucketMap.get(modelID).YY++;
				} else if (!modelTrans.contains(trans) && baseTrans.contains(trans)) {
					bucketMap.get(modelID).NY++;
				} else if (modelTrans.contains(trans) && !baseTrans.contains(trans)) {
					bucketMap.get(modelID).YN++;
				} else if (!modelTrans.contains(trans) && !baseTrans.contains(trans)) {
					bucketMap.get(modelID).NN++;
				}
			}
			
		}
		// add as feature
		for (Iterator<Integer> id = data.getModelIDs().iterator(); id.hasNext(); ) {
			int modelID = id.next();
			String prefix = data.getModel(modelID).getLogName() + "_";
			HashMap<String,Object> feat = new HashMap<String,Object>();

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
	 * hashed by TimeStamp.
	 * @param map
	 * @return
	 */
	private double[] extractTimeSeries(HashMap<TimeStamp,Integer> map) {
		
		// Have to sort the TimeStamps since not necessarily sorted in HashMap
		TreeSet<TimeStamp> times = new TreeSet<TimeStamp>();
		ArrayList<TimeStamp> keys = new ArrayList<TimeStamp>(map.keySet());
		for (Iterator<TimeStamp> i = keys.iterator(); i.hasNext(); ) {
			TimeStamp t = i.next();
			if (t != null) 
				times.add(t);
		}
		
		int cnt = 0;
		TimeStamp prevTime = null;
		double[] vals = new double[(int) data.simLength.longValue()];
		for (Iterator<TimeStamp> it = times.iterator(); it.hasNext(); ) {
			if (prevTime == null) {
				// if prevTime has not been defined yet, set as the first time where spread measured
				prevTime = it.next();
			} else {
				// next Time is the next time at which to extend the time series
				TimeStamp nextTime = it.next();
				// fill in the vals array, but only for segments where it is not undefined
				if (!map.get(prevTime).equals(Consts.INF_PRICE)) {
					for (int i = (int) prevTime.longValue(); i < nextTime.longValue(); i++) {
						vals[cnt] = map.get(prevTime);
						cnt++;
					}
				}
				prevTime = nextTime;
			}
		}
		// fill in to end of array
		if (!prevTime.after(data.simLength)) {
			if (map.get(prevTime) != Consts.INF_PRICE) { 
				for (int i = (int) prevTime.longValue(); i < data.simLength.longValue(); i++) {
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
	public HashMap<String,Object> getConfiguration() {
		HashMap<String,Object> config = new HashMap<String,Object>();
		config.put("obs", data.obsNum);
		config.put("latency", data.nbboLatency);
		config.put("tick_size", data.tickSize);
		config.put("kappa", data.kappa);
		config.put("arrival_rate", data.arrivalRate);
		config.put("mean_pv", data.meanPV);
		config.put("shock_var", data.shockVar);
		config.put("expire_rate", data.expireRate);
		config.put("bid_range", data.bidRange);
		config.put("pv_var", data.privateValueVar);
		
		// market maker params
		config.put("mm_sleep_time", data.marketmaker_sleepTime);
		config.put("mm_num_rungs", data.marketmaker_numRungs);
		config.put("mm_rung_size", data.marketmaker_rungSize);
		
		return config;
	}
	
	
	/**
	 * @return HashMap of models/types in the simulation.
	 */
	public HashMap<String,Object> getModelInfo() {
		return null;
	}
	
	/**
	 * @return HashMap of number of agents of each type in the simulation.
	 */
	public HashMap<String,Object> getAgentInfo() {
		HashMap<String,Object> info = new HashMap<String,Object>();
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
