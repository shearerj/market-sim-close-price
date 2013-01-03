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
		if (obs == null) return;
		
		if (!observations.containsKey("players")) {
			ArrayList<Object> array = new ArrayList<Object>();
			array.add(obs);
			observations.put("players", array);
		} else {
			((ArrayList<Object>) observations.get("players")).add(obs);
		}
	}

	/**
	 * Used to put observation as feature in the case
	 * @param agentID
	 * @return
	 */
	public void addObservationAsFeature(int agentID) {
		Agent ag = data.getAgent(agentID);
		addFeature("agent" + agentID, ag.getObservation());
	}
	
	/**
	 * Adds a feature (for entire simulation, not just one player).
	 * @param description
	 * @param ft
	 */
	public void addFeature(String description, HashMap<String,Object> ft) {
		if (!observations.containsKey("features")) {
			HashMap<String,Object> feats = new HashMap<String,Object>();
			feats.put(description, ft);
			observations.put("features", feats);
		} else {
			((HashMap<String,Object>) observations.get("features")).put(description, ft);
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
			feat.put("med" + suffix, "NaN");
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
	 * Extracts surplus features for background agents in a given model.
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String,Object> getSurplusFeatures(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		ArrayList<Integer> ids = model.getMarketIDs();
		HashMap<Integer,Integer> allSurplus = data.getSurplus(ids);
		Object[] objs = (new ArrayList<Integer>(allSurplus.values())).toArray();
		double[] values = new double[objs.length];  
		for (int i = 0; i < values.length; i++) {
	    		Integer tmp = (Integer) objs[i];
			values[i] = tmp.doubleValue();
		}
		addAllStatistics(feat,values);

		for (Iterator<Integer> it = model.getAgentIDs().iterator(); it.hasNext(); ) {
			int aid = it.next();
			// check if agent is a player in a role
			if (!data.isBackgroundAgent(aid)) {
				String type = data.getAgent(aid).getType();
				String label = "with_" + type.toLowerCase();
				
				// Append the agentID if there is 1+ of this type in the simulation
				if (data.numAgentType.get(type) > 1) {
					label += aid;
				}
				Agent a = data.getAgent(aid);
				DescriptiveStatistics ds = new DescriptiveStatistics(values);
				feat.put(label, ds.getSum() + a.getRealizedProfit().get(model.getID()));
			}
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
	 * Gets general bid information, such as number executed, number expired, 
	 * number left unexpired, total number of bids submitted by 1) background agents,
	 * 2) role agents, 3) all agents
	 * 
	 * @return
	 */
	public HashMap<String,Object> getBidInfo() {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		int num = 0;
		int totBids = 0;
//		for (Map.Entry<Integer,Agent> entry : agents.entrySet()) {
//			Agent a = entry.getValue();
//			if (data.isBackgroundAgent(a.getID())) {
//				
//				// MORE TODO here
//				Consts.SubmittedBidMarket x = ((ZIAgent) a).submittedBidType;
//				if (x == null)
//					System.err.print("ERROR");
//				totBids++;
//				switch (x) {
//				case MAIN:
//					// do nothing
//				case ALTERNATE:
//					num++;
//				}
//			}
//		}
		feat.put("num_bids_alt", num);
		feat.put("num_total_bids", totBids);
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
//		double[] values = new double[data.executionTime.size()];
		double[] values = new double[data.executionSpeed.size()];
		int cnt = 0;
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			int mktID = it.next();
			
//			for (Map.Entry<Integer, TimeStamp> entry : data.executionTime.entrySet()) {
//				int bidID = entry.getKey();
//				TimeStamp ts = entry.getValue();
//				PQBid b = data.getBid(bidID);
//				if (mktID != b.getMarketID()) {  // should be ==?
//					System.out.println("bidID=" + bidID + ", submit=" + data.submissionTime.get(bidID) + ", execute=" + ts);	// TODO debug only
//					values[cnt] =  (double) ts.diff(data.submissionTime.get(bidID)).longValue();
//					cnt++;
//				}
//			}
			for (Map.Entry<Integer, TimeStamp> entry : data.executionSpeed.entrySet()) {
				int bidID = entry.getKey();
				PQBid b = data.getBid(bidID);
				if (mktID == b.getMarketID()) {
					// bid in the market currently being checked
//					System.out.println("bidID=" + bidID + ", submit=" + data.submissionTime.get(bidID) + ", speed=" + entry.getValue());
					values[cnt] = (double) entry.getValue().longValue();
					cnt++;
				}
			}
			// Reinitialize to get rid of unused portion of array
			double[] speeds = new double[cnt];
			for (int i = 0; i < cnt; i++) {
				speeds[i] = values[i];
			}
			addStatistics(feat,values,"mkt" + (-mktID),false);
		}
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
	
		ArrayList<Integer> ids = model.getMarketIDs();

		HashMap<Integer,PQTransaction> transactions = data.getTrans(ids);
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
				if (data.numAgentType.get(type) > 1) {
					suffix += aid;
				}
				feat.put("buys" + suffix, buys);
				feat.put("sells" + suffix, sells);
			}
		}
		return feat;
	}
	
	
	/**
	 * Computes spread metrics for the given model..
	 * 
	 * @param model
	 * @return
	 */
	public HashMap<String,Object> getSpreadInfo(MarketModel model) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
	
		ArrayList<Integer> ids = model.getMarketIDs();
		for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
			int mktID = it.next();
			HashMap<TimeStamp,Integer> marketSpread = data.marketSpread.get(mktID);
			if (marketSpread != null) {
				double[] spreads = extractTimeSeries(marketSpread);
				addStatistics(feat,spreads,"mkt" + (-mktID),true);
			}
			
//				// JUST FOR DEBUGGING PURPOSES TODO
//				TreeSet<TimeStamp> sortedTimes = new TreeSet<TimeStamp>(marketSpread.keySet());
//				Median med = new Median();
//				if (med.evaluate(spreads) > 200000 || med.evaluate(spreads) == 0) {					
//					ArrayList<Integer> sortedSpreads = new ArrayList<Integer>();
//					for (Iterator<TimeStamp> xt = sortedTimes.iterator(); xt.hasNext(); ) {
//						sortedSpreads.add(marketSpread.get(xt.next()));
//					}
//					try {
//						// Check first if directory exists
//						File file = new File("spreads_mkt" + mktID + "_obs" + data.obsNum + ".txt");
//						if (!file.exists()) {
//							file.createNewFile();
//							BufferedWriter f = new BufferedWriter(new FileWriter(file));
//							f.write(sortedTimes.toString());
//							f.newLine();
//							f.write(sortedSpreads.toString());
//							f.flush();
//							f.close();
//						}
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}				
		}
		
		double[] nbboSpreads = extractTimeSeries(data.NBBOSpread.get(model.getID()));
		addStatistics(feat,nbboSpreads,"nbbo",true);
		
		return feat;
	}
	

	/**
	 * Computes spread metrics the given market IDs.
	 * 
	 * @param ids
	 * @return
	 */
	public HashMap<String,Object> getDepthInfo(ArrayList<Integer> ids) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
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
	 * Computes volatility metrics.
	 * 
	 * Volatility is measured as the standard deviation of logarithmic returns.
	 * TODO: try multiple periods for determining returns
	 * 
	 * @param ids
	 * @return
	 */
	public HashMap<String, Object> getVolatilityInfo(ArrayList<Integer> ids) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		for (Iterator<Integer> it = data.getMarketIDs().iterator(); it.hasNext(); ) {
			int mktID = it.next();
				
				
		}
		
		return feat;
	}
	
	
	/**
	 * Generate the distribution info on a certain dataset.
	 * 
	 * @param ids
	 * @return
	 */
	public HashMap<String, Object> getDistributionInfo(ArrayList<Integer> ids) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		
		return feat;
	}
	
	
	
	public HashMap<String, Object> getTransactionComparison() {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		// cycle through all the market models 
		// so iterate through the centralized markets
		// and within that, iterate through the number of two-market models (here just 1)
		
		// determine the call market ID, because will use that as the baseline
		for (Map.Entry<Integer, Market> entry : data.markets.entrySet()) {
			
		}
			
		for (Map.Entry<Integer,MarketModel> entry : data.getModels().entrySet()) {
			// Do for combinations with 2M model
			MarketModel model = entry.getValue();
			ArrayList<Integer> ids = model.getMarketIDs();
			
			HashMap<Integer,PQTransaction> transactions = data.getTrans(ids);
			
			for (Map.Entry<Integer,PQTransaction> tr : transactions.entrySet()) {
				PQTransaction trans = tr.getValue();
//					if (trans.sellerID == id) {
//						buys++;
//					} else if (trans.buyerID == id) {
//						sells++;
//					}
			}
		}
		
	
//		ArrayList<Integer> ids = data.getCentralMarketIDs();
//		// perform same analysis for comparing the two centralized markets
//		
//		for (Iterator<Integer> aid = data.roleAgentIDs.iterator(); aid.hasNext(); ) {
//			int id = aid.next();
//			suffix = suffix + "_" + data.getAgent(id).getType().toLowerCase();
//
//			int buys = 0;
//			int sells = 0;
//			
//
//			if (data.numAgentType.get(data.getAgent(id).getType()) > 1) {
//				suffix += id;
//			}
//			feat.put("buys" + suffix , buys);
//			feat.put("sells" + suffix, sells);
//		}
		return feat;
		
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
		ArrayList<TimeStamp> keys = new ArrayList<TimeStamp>( map.keySet());
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
				// fill in the timeSeries array, but only for segments where it is not undefined
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
