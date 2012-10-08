package systemmanager;

import entity.*;
import event.TimeStamp;
import market.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
	 * Gets the agent's observation and adds to the hashmap container.
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
	 * Adds a feature (for entire simulation, not just one player).
	 * 
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
	 * Takes hashmap of agent information (integer values only) and extracts features for 
	 * observation file.
	 * 
	 * @param allSurplus
	 * @param central
	 * @return
	 */
	public HashMap<String,Object> getSurplusFeatures(HashMap<Integer,Integer> allSurplus,
			boolean central) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		Object[] objs = (new ArrayList<Integer>(allSurplus.values())).toArray();
	    double[] values = new double[objs.length];  
	    for (int i = 0; i < values.length; i++) {
	    	Integer tmp = (Integer) objs[i];
	        values[i] = tmp.doubleValue();
	    }
		addAllStatistics(feat,values);
		if (!central) {
			for (Iterator<Integer> it = data.roleAgentIDs.iterator(); it.hasNext(); ) {
				int id = it.next();
				String type = data.getAgent(id).getType();
				String suffix = "sum_plus_" + type.toLowerCase();
				if (data.numAgentType.get(type) > 1) {
					suffix += id;
				}
				DescriptiveStatistics ds = new DescriptiveStatistics(values);
				feat.put(suffix, ds.getSum() + data.getAgent(id).getRealizedProfit());
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
	 * Counts number of times background agent submits bid to alternate market for
	 * (hopefully) immediate transaction, which may not happen if the NBBO quote is
	 * out of date.
	 * 
	 * @param agents
	 * @return
	 */
	public HashMap<String,Object> getBackgroundInfo(HashMap<Integer,Agent> agents) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		int num = 0;
		int totBids = 0;
		for (Map.Entry<Integer,Agent> entry : agents.entrySet()) {
			Agent a = entry.getValue();
			if (a instanceof ZIAgent) {
				Consts.SubmittedBidMarket x = ((ZIAgent) a).submittedBidType;
				if (x == null)
					System.err.print("ERROR");
				totBids++;
				switch (x) {
				case MAIN:
					// do nothing
				case ALTERNATE:
					num++;
				}
			}
		}
		feat.put("num_bids_alt", num);
		feat.put("num_total_bids", totBids);
		return feat;
	}

	/**
	 * Computes statistical values on the transaction data.
	 * 
	 * @param central true if on central market data only
	 * @return
	 */
	public HashMap<String,Object> getTransactionInfo(boolean central) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		int num = 1;
		if (central) {
			num = data.getCentralMarketIDs().size();
		}
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int it = 0; it < num; it++ ) {
			String suffix = "";
			if (central) {
				int mktID = data.getCentralMarketIDs().get(it);
				if (ids.isEmpty()) {
					ids.add(mktID);
				} else {
					ids.set(0, mktID);		// only set index 0
				}
				suffix = "_" + data.getCentralMarketType(mktID).toLowerCase();
			} else {
				ids = data.getMarketIDs();
			}
			HashMap<Integer,PQTransaction> transactions = data.getTrans(ids);
			feat.put("num_trans" + suffix, transactions.size());
	
			double[] prices = new double[transactions.size()];
			double[] quantities = new double[transactions.size()];
			int i = 0;
			for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
				PQTransaction trans = entry.getValue();
				prices[i] = trans.price.getPrice();
				quantities[i] = trans.quantity;
				i++;
			}
			addStatistics(feat,prices,"price" + suffix,false);
			//		addSomeStatistics(feat,quantities,"qty",false);
			
			if (!central) {
				for (Iterator<Integer> aid = data.roleAgentIDs.iterator(); aid.hasNext(); ) {
					int id = aid.next();
					suffix = suffix + "_" + data.getAgent(id).getType().toLowerCase();

					int buys = 0;
					int sells = 0;
					for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
						PQTransaction trans = entry.getValue();
						if (trans.sellerID == id) {
							buys++;
						} else if (trans.buyerID == id) {
							sells++;
						}
					}

					if (data.numAgentType.get(data.getAgent(id).getType()) > 1) {
						suffix += id;
					}
					feat.put("buys" + suffix , buys);
					feat.put("sells" + suffix, sells);
				}
			}
		}
		return feat;
	}
	
	
	/**
	 * Computes execution speed metrics.
	 * 
	 * @param central true if on central market data only
	 * @return
	 */
	public HashMap<String,Object> getExecutionSpeed(boolean central) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		// Initialize with maximum number of possible bids
		double[] values = new double[data.executionTime.size()];
		int cnt = 0;
		
		int num = 1;
		if (central) {
			num = data.getCentralMarketIDs().size();
		}
		for (int it = 0; it < num; it++) {
			int centralMktID = data.getCentralMarketIDs().get(it);
			
			String suffix = "";
			if (central) {
				suffix = data.getCentralMarketType(centralMktID).toLowerCase();
			}
			
			for (Map.Entry<Integer, TimeStamp> entry : data.executionTime.entrySet()) {
				int bidID = entry.getKey();
				TimeStamp ts = entry.getValue();
				PQBid b = data.getBid(bidID);
				if ((central && centralMktID == b.getMarketID()) ||
						(!central && centralMktID != b.getMarketID())) {
					values[cnt] =  (double) ts.diff(data.submissionTime.get(bidID)).longValue();
					cnt++;
				}
			}
			// Reinitialize to get rid of unused portion of array
			double[] speeds = new double[cnt];
			for (int i = 0; i < cnt; i++) {
				speeds[i] = values[i];
			}
			addStatistics(feat,values,suffix,false);
		}
		return feat;
	}
	
	
	/**
	 * Computes spread metrics either on 1) all markets + NBBO, or 2) central market.
	 * 
	 * @param central true if on central market data only
	 * @return
	 */
	public HashMap<String,Object> getSpreadInfo(boolean central) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		if (!central) {
			for (Iterator<Integer> it = data.getMarketIDs().iterator(); it.hasNext(); ) {
				int mktID = it.next();
				HashMap<TimeStamp,Integer> marketSpread = data.marketSpread.get(mktID);
				double[] spreads = extractTimeSeries(marketSpread);
				addStatistics(feat,spreads,"mkt" + (-mktID),true);
				
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
			double[] nbboSpreads = extractTimeSeries(data.NBBOSpread);
			addStatistics(feat,nbboSpreads,"nbbo",true);
			
		} else {
			for (Iterator<Integer> it = data.getCentralMarketIDs().iterator(); it.hasNext(); ) {
				int mktID = it.next();
				HashMap<TimeStamp,Integer> marketSpread = data.marketSpread.get(mktID);
				double[] spreads = extractTimeSeries(marketSpread);
				String mktType = data.getCentralMarketType(mktID).toLowerCase();
				addStatistics(feat,spreads,"mkt_" + mktType,true);
			}
		}
		return feat;
	}
	

	/**
	 * Computes spread metrics either on 1) all markets, or 2) central market only.
	 * 
	 * @param central true if on central market data only
	 * @return
	 */
	public HashMap<String,Object> getDepthInfo(boolean central) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		if (!central) {
			for (Iterator<Integer> it = data.getMarketIDs().iterator(); it.hasNext(); ) {
				int mktID = it.next();
				HashMap<TimeStamp,Integer> marketDepth = data.marketDepth.get(mktID);
				double[] depths = extractTimeSeries(marketDepth);
				addStatistics(feat,depths,"mkt" + (-mktID),true);
			}
		} else {
			for (Iterator<Integer> it = data.getCentralMarketIDs().iterator(); it.hasNext(); ) {
				int mktID = it.next();
				HashMap<TimeStamp,Integer> marketDepth = data.marketDepth.get(mktID);
				double[] depths = extractTimeSeries(marketDepth);
				String mktType = data.getCentralMarketType(mktID).toLowerCase();
				addStatistics(feat,depths,"mkt_" + mktType,true);
			}
		}
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
		config.put("call_freq", data.clearFreq);
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
