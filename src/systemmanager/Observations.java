package systemmanager;

import entity.*;
import event.TimeStamp;
import market.*;

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
	 * Takes hashmap of agent information (integer values only) and extracts features for 
	 * observation file.
	 * 
	 * @param surplus
	 * @return
	 */
	public HashMap<String,Object> getIntegerFeatures(HashMap<Integer,Integer> allValues) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		Object[] surplus = (new ArrayList<Integer>(allValues.values())).toArray();
	    double[] values = new double[surplus.length];  
	    for (int i = 0; i < values.length; i++) {
	    	Integer tmp = (Integer) surplus[i];
	        values[i] = tmp.doubleValue();
	    }
		DescriptiveStatistics ds = new DescriptiveStatistics(values);
		Median med = new Median();
		
		feat.put("mean", ds.getMean());
		feat.put("max", ds.getMax());
		feat.put("min", ds.getMin());
		feat.put("sum", ds.getSum());
		feat.put("var", ds.getVariance());
		feat.put("med", med.evaluate(values));
		
		return feat;
	}
	
	
	/**
	 * Extracts features of a list of times (e.g. intervals, arrival times).
	 * 
	 * @param allTimes ArrayList of TimeStamps
	 * @param description is prefix to append to key in map
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
		DescriptiveStatistics ds = new DescriptiveStatistics(values);
		Median med = new Median();
		
		feat.put("mean", ds.getMean());
		feat.put("max", ds.getMax());
		feat.put("min", ds.getMin());
		feat.put("sum", ds.getSum());
		feat.put("var", ds.getVariance());
		feat.put("med", med.evaluate(values));
		
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
	    DescriptiveStatistics ds = new DescriptiveStatistics(values);
		Median med = new Median();
		
		feat.put("mean", ds.getMean());
		feat.put("max", ds.getMax());
		feat.put("min", ds.getMin());
		feat.put("sum", ds.getSum());
		feat.put("var", ds.getVariance());
		feat.put("med", med.evaluate(values));
		
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
			if (a instanceof BackgroundAgent) {
				Consts.SubmittedBidMarket x = ((BackgroundAgent) a).submittedBidType;
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
	 * @return
	 */
	public HashMap<String,Object> getTransactionInfo() {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		HashMap<Integer,PQTransaction> transactions = data.getTrans();
		feat.put("num_trans", transactions.size());
		
		int buys = 0;
		int sells = 0;
		double[] prices = new double[transactions.size()];
		double[] quantities = new double[transactions.size()];
		int i = 0;
		for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
			PQTransaction trans = entry.getValue();
			prices[i] = trans.price.getPrice();
			quantities[i] = trans.quantity;
			i++;
			if (trans.sellerID == 1) {
				buys++;
			} else if (trans.buyerID == 1) {
				sells++;
			}
		}
		feat.put("hft_buys", buys);
		feat.put("hft_sells", sells);
		
		DescriptiveStatistics dp = new DescriptiveStatistics(prices);
		feat.put("price_mean", dp.getMean());
		feat.put("price_max", dp.getMax());
		feat.put("price_min", dp.getMin());
		feat.put("price_var", dp.getVariance());
		
//		DescriptiveStatistics dq = new DescriptiveStatistics(quantities);
//		feat.put("qty_mean", dq.getMean());
//		feat.put("qty_max", dq.getMax());
//		feat.put("qty_min", dq.getMin());
//		feat.put("qty_var", dq.getVariance());
		
		return feat;
	}
	
	
	/**
	 * Computes spread metrics all markets plus the NBBO.
	 * @return
	 */
	public HashMap<String,Object> getSpreadInfo() {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		for (Iterator<Integer> it = data.getMarketIDs().iterator(); it.hasNext(); ) {
			int mktID = it.next();
			HashMap<TimeStamp,Integer> marketSpread = data.marketSpread.get(mktID);
			double[] spreads = extractTimeSeries(marketSpread);
			
			DescriptiveStatistics dp = new DescriptiveStatistics(spreads);
			Median med = new Median();
			String prefix = "mkt" + (-mktID) + "_";
			feat.put(prefix + "mean", dp.getMean());
//			feat.put(prefix + "max", dp.getMax());
//			feat.put(prefix + "min", dp.getMin());
//			feat.put(prefix + "var", dp.getVariance());
			feat.put(prefix + "med", med.evaluate(spreads));
		}

		double[] nbboSpreads = extractTimeSeries(data.NBBOSpread);
		DescriptiveStatistics dp = new DescriptiveStatistics(nbboSpreads);
		Median med = new Median();
		feat.put("nbbo_mean", dp.getMean());
//		feat.put("nbbo_max", dp.getMax());
//		feat.put("nbbo_min", dp.getMin());
//		feat.put("nbbo_var", dp.getVariance());
		feat.put("nbbo_med", med.evaluate(nbboSpreads));

		return feat;
	}
	

	/**
	 * Compute depth metrics for the given market.
	 * @return
	 */
	public HashMap<String,Object> getDepthInfo() {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		for (Iterator<Integer> it = data.getMarketIDs().iterator(); it.hasNext(); ) {
			int mktID = it.next();
			HashMap<TimeStamp,Integer> marketDepth = data.marketDepth.get(mktID);
			double[] depths = extractTimeSeries(marketDepth);
			
			DescriptiveStatistics dp = new DescriptiveStatistics(depths);
			String prefix = "mkt" + (-mktID) + "_";
			feat.put(prefix + "mean", dp.getMean());
			feat.put(prefix + "max", dp.getMax());
			feat.put(prefix + "min", dp.getMin());
			feat.put(prefix + "var", dp.getVariance());
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
		
		TimeStamp prevTime = new TimeStamp(0);
		double[] vals = new double[(int) data.simLength.longValue()];
		for (Iterator<TimeStamp> it = times.iterator(); it.hasNext(); ) {
			TimeStamp currTime = it.next();
			// fill in the timeSeries array
			for (int i = (int) prevTime.longValue(); i <= currTime.longValue(); i++) {
				vals[i] = map.get(currTime);
			}
			// Increment previous time to one time step after the current time
			prevTime = currTime.sum(new TimeStamp(1));
		}
		// if have not filled in the end of the array, fill in
		if (!prevTime.after(data.simLength)) {
			for (int i = (int) prevTime.longValue(); i < data.simLength.longValue(); i++) {
				// get last inserted value and insert
				vals[i] = map.get(prevTime.diff(new TimeStamp(1)));
			}
		}
		return vals;
	}
	
	
	/**
	 * Computes execution speed metrics.
	 * @return
	 */
	public HashMap<String,Object> getExecutionSpeed() {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		double[] speeds = new double[data.executionTime.size()];
		int i = 0;
		for (Map.Entry<Integer, TimeStamp> entry : data.executionTime.entrySet()) {
			int bidID = entry.getKey();
			TimeStamp ts = entry.getValue();
			speeds[i] =  (double) ts.diff(data.submissionTime.get(bidID)).longValue();
			i++;
		}
		DescriptiveStatistics dp = new DescriptiveStatistics(speeds);
		feat.put("mean", dp.getMean());
		feat.put("max", dp.getMax());
		feat.put("min", dp.getMin());
		feat.put("var", dp.getVariance());
		
		return feat;
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
