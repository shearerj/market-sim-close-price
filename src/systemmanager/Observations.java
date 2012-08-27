package systemmanager;

import entity.*;
import event.TimeStamp;
import market.PQTransaction;
import market.Price;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.*;
import org.apache.commons.math3.stat.descriptive.*;
import org.apache.commons.math3.stat.descriptive.rank.*;


/**
 * Contains payoff data/features for all players in the simulation.
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
		
		// Don't add observation if agent is not a player in the game
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
	 * Takes hashmap of agent surplus and extracts features for observation file.
	 * 
	 * @param surplus
	 * @return
	 */
	public HashMap<String,Object> getSurplusFeatures(HashMap<Integer,Integer> allSurplus) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		Object[] surplus = (new ArrayList<Integer>(allSurplus.values())).toArray();
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
	 * Determines number of negative private values. Counts number of times
	 * NBBO agent sees quote of its alternate markets.
	 * @param privateValues
	 * @param agents
	 * @return
	 */
	public HashMap<String,Object> getNBBOInfo(ArrayList<Price> privateValues,
			HashMap<Integer,Agent> agents) {
		HashMap<String,Object> feat = new HashMap<String,Object>();
		
		Object[] prices = privateValues.toArray();
		double[] values = new double[prices.length];
		int cnt = 0;
	    for (int i = 0; i < values.length; i++) {
	    	Price tmp = (Price) prices[i];
	        values[i] = (double) tmp.getPrice();
	        if (values[i] <= 0) {
	        	cnt++;
	        }
	    }
		feat.put("num_neg_PV", cnt);
		
		int num = 0;
		for (Map.Entry<Integer,Agent> entry : agents.entrySet()) {
			Agent a = entry.getValue();
			if (a.getType().equals("NBBO")) {
				SystemConsts.NBBOBidType x = ((NBBOAgent) a).bidSubmittedType;
				if (x == null)
					System.err.print("ERROR");
				switch (x) {
				case CURRENT:
					num++;
				case ALTERNATE:
					// do nothing
				}
			}
		}
		feat.put("num_nbbo_worse", num);
		return feat;
	}

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
		
		DescriptiveStatistics dq = new DescriptiveStatistics(quantities);
		feat.put("qty_mean", dq.getMean());
		feat.put("qty_max", dq.getMax());
		feat.put("qty_min", dq.getMin());
		feat.put("qty_var", dq.getVariance());
		
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
