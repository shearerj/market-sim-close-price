package data;

import model.*;
import entity.*;
import market.*;
import systemmanager.*;

// import java.io.BufferedWriter;
// import java.io.File;
// import java.io.FileWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.*;
import org.apache.commons.math3.stat.descriptive.*;
import org.apache.commons.lang3.ArrayUtils;


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
	public final static String LOG = "log";
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
	public final static String MAIN = "main";
	public final static String ALTERNATE = "alt";
	public final static String TRANSACT = "transact";
	public final static String NO_TRANSACT = "notrans";
	
	// Suffixes
	public final static String AGENTSETUP = "setup";
	public final static String DISCOUNTED = "disc";
	public final static String UNDISCOUNTED = "no" + DISCOUNTED;
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
			addObservation(id);
		}
		addFeature("", getConfiguration());
		
		// set up max time (where most agents have arrived)
		long time = Math.round(data.getNumEnvAgents() / data.arrivalRate);
		long maxTime = Market.quantize((int) time, 1000);
		
		// iterate through all models
		for (MarketModel model : data.getModels().values()) {
			String modelName = model.getLogName() + "_";

			addFeature(modelName + SURPLUS, getSurplus(model));
			addFeature(modelName + EXECTIME, getTimeToExecution(model));
			addFeature(modelName + TRANSACTIONS, getTransactionInfo(model));
			addFeature(modelName + SPREADS, getSpread(model, maxTime));
			for (int win : Consts.windows) {
				addFeature(modelName + PRICEVOL, getVolatility(model, win, maxTime));
			}
			addFeature(modelName + MARKETMAKER, getMarketMakerInfo(model));
			
			// TODO - need to remove the position balance hack
			// addFeature(modelName + ROUTING, getRegNMSRoutingInfo(model));
		}
		
	}
	
	/**
	 * Adds a feature to the observation file. Only add if ft is non-empty.
	 * 
	 * @param description 	key for list of features to insert
	 * @param ft			Feature to add
	 */
	@SuppressWarnings("unchecked")
	public void addFeature(String description, Feature ft) {
		if (ft.isEmpty()) return;
		
		if (!observations.containsKey(FEATURES_KEY)) {
			HashMap<String,Object> feats = new HashMap<String,Object>();
			feats.put(description.toLowerCase(), ft.get());
			observations.put(FEATURES_KEY, feats);
		} else {
			((HashMap<String,Object>) observations.get(FEATURES_KEY)).put(
					description.toLowerCase(), ft.get());
		}
	}
	
	/**
	 * Adds the player's observation.
	 * 
	 * @param agentID
	 */
	@SuppressWarnings("unchecked")
	public void addObservation(int agentID) {
		
		// Don't add observation if agent is not a player in the game
		if (!data.isPlayer(agentID)) return;
		
		HashMap<String, Object> obs = data.getAgent(agentID).getObservation();
		if (obs == null || obs.isEmpty()) return;
		
		// Add agent to list of player observations
		if (!observations.containsKey(PLAYERS_KEY)) {
			ArrayList<Object> array = new ArrayList<Object>();
			array.add(obs);
			observations.put(PLAYERS_KEY, array);
		} else {
			((ArrayList<Object>) observations.get(PLAYERS_KEY)).add(obs);
		}
	}

	
	/********************************************
	 * Data aggregation
	 *******************************************/
	
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
	 * Extract discounted surplus for background agents, including those
	 * that are players (in the EGTA use case).
	 * 
	 * TODO update for more specific agent surplus?
	 * 
	 * @param model
	 * @return
	 */
	public Feature getSurplus(MarketModel model) {
		Feature feat = new Feature();
		
		for (double rho : Consts.rhos) {
			String suffix = "";
			if (rho != 0) {
				suffix += "_" + DISCOUNTED + rho;
			} else {
				suffix += "_" + UNDISCOUNTED;
			}
			
			Surplus surplus = data.getSurplus(model.getID(), rho);
			DescriptiveStatistics total = new DescriptiveStatistics(
					ArrayUtils.toPrimitive(surplus.values().toArray(new 
					Double[surplus.size()])));
			feat.addSum("", TOTAL + suffix, total);
			
			// sub-categories for surplus
			DescriptiveStatistics bkgrd = new DescriptiveStatistics();
			DescriptiveStatistics hft = new DescriptiveStatistics();
			DescriptiveStatistics mm = new DescriptiveStatistics();
			DescriptiveStatistics env = new DescriptiveStatistics();
			
			// go through all agents & update for each agent type
			for (Integer agentID : surplus.agents()) {
				Agent ag = data.getAgent(agentID);
				double val = surplus.get(agentID);
				
				if (ag instanceof BackgroundAgent) {
					bkgrd.addValue(val);
				} else if (ag instanceof HFTAgent) {
					val = ag.getRealizedProfit();
					hft.addValue(val);
				} else if (ag instanceof MarketMaker) {
					mm.addValue(val);
				}
				if (data.isEnvironmentAgent(agentID)) {
					env.addValue(val);
				}
			}
			feat.addSum("", BACKGROUND + suffix, bkgrd);
			feat.addSum("", MARKETMAKER + suffix, mm);
			feat.addSum("", HFT, hft);
			if (data.isEGTAUseCase()) {	// only add if EGTA use case
				feat.addSum("", ENVIRONMENT, env);
			}
		}
		return feat;
	}
	
	
	/**
	 * Transaction statistics.
	 * 
	 * @param model
	 * @return
	 */
	public Feature getTransactionInfo(MarketModel model) {
		Feature feat = new Feature();

		List<PQTransaction> trans = new ArrayList<PQTransaction>();
		for (int mktID : model.getMarketIDs()) {
			//Getting the transaction data for each market in the model
			trans.addAll(data.transactionLists.get(mktID));
		}
		feat.put(NUM, trans.size());
		
		DescriptiveStatistics prices = new DescriptiveStatistics();
		DescriptiveStatistics quantity = new DescriptiveStatistics();
		DescriptiveStatistics fundamental = new DescriptiveStatistics();
		for (PQTransaction tr : trans) {
			prices.addValue(tr.price.getPrice());
			quantity.addValue(tr.quantity);
			fundamental.addValue(data.getFundamentalAt(tr.timestamp).getPrice());
		}
		feat.addMean(PRICE, "", prices);
		feat.addStdDev(PRICE, "", prices);
		feat.addRMSD(prices, fundamental);
		
		for (Integer aid : model.getAgentIDs()) {
			// check if agent is an HFT agent or a player
			if (data.getAgent(aid) instanceof HFTAgent || 
					!data.isEnvironmentAgent(aid)) {
				String type = data.getAgent(aid).getType();

				// count buys/sells
				int buys = 0;
				int sells = 0;
				for (PQTransaction tr : trans) {
					if (tr.sellerID == aid) {
						++sells;
					}
					else if (tr.buyerID == aid) {
						++buys;
					}
				}
				// add agentID in case there is more than 1 of this type
				String suffix = "_" + type.toLowerCase() + aid;
				feat.put(BUYS + suffix, buys);
				feat.put(SELLS + suffix, sells);
				feat.put(TRANSACTIONS + suffix, buys+sells);
			}
		}
		return feat;
	}
	
	
	/**
	 * Computes spread metrics for the given model.
	 * 
	 * @param model
	 * @param maxTime
	 * @return
	 */
	public Feature getSpread(MarketModel model, long maxTime) {
		Feature feat = new Feature();
		
		DescriptiveStatistics medians = new DescriptiveStatistics();
		for (int mktID : model.getMarketIDs()) {
			TimeSeries s = data.marketSpread.get(mktID);
			if (s != null) {
				double[] array = s.getArray();
				DescriptiveStatistics spreads = new DescriptiveStatistics(array);
				double med = feat.addMedianUpToTime("", MARKET + (-mktID), 
						spreads, maxTime);
				medians.addValue(med);
			}
		}
		// average of median market spreads (for all markets in this model)
		feat.addMean(medians);
		
		TimeSeries nbbo = data.NBBOSpread.get(model.getID());
		if (nbbo != null) {
			double[] array = nbbo.getArray();
			DescriptiveStatistics spreads = new DescriptiveStatistics(array);
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
	 * 	 window sizes) the standard deviation of logarithmic returns. (DLYAN'S CODE) TODO
	 * 
	 * @param model
	 * @param window
	 * @param maxTime
	 * @return
	 */
	public Feature getVolatility(MarketModel model, int window, long maxTime) {
		Feature feat = new Feature();
		String prefix = "";
		if (window > 0) {
			prefix += "win" + window;
		}
		
		DescriptiveStatistics stddev = new DescriptiveStatistics();
		DescriptiveStatistics logpvol = new DescriptiveStatistics();
		for (int mktID : model.getMarketIDs()) {
			TimeSeries mq = data.marketMidQuote.get(mktID);
			if (mq != null) {
				double[] array = mq.getSampleArray(window, maxTime);
				DescriptiveStatistics prices = new DescriptiveStatistics(array);
				
				double s = feat.addStdDev(prefix, MARKET + (-mktID), prices);
				stddev.addValue(s);
				
				double logstddev = s; 
				if (s != 0) {
					logstddev = Math.log(s);
					logpvol.addValue(logstddev);
				}
			}
		}
		// average measures across all markets in this model 
		feat.addMean(prefix, LOG, logpvol);
		feat.addMean(prefix, LOG, stddev);
		
		return feat;
	}
	
	/**
	 * TODO remove hack to determine if its bid transacted or not
	 * 
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
					if (b.getPositionBalance() == 0) numAltNoTrans++;
					else numAltTrans++;
				} else {
					// order was not routed
					numMain++;
					if (b.getPositionBalance() == 0) numMainNoTrans++;
					else numMainTrans++;
				}
			}
		}
		feat.put(MAIN, numMain);	
		feat.put(ALTERNATE, numAlt);	
		feat.put(ALTERNATE + "_" + TRANSACT, numAltTrans);
		feat.put(ALTERNATE + "_" + NO_TRANSACT, numAltNoTrans);
		feat.put(MAIN + "_" + TRANSACT, numMainTrans);
		feat.put(MAIN + "_" + NO_TRANSACT, numMainNoTrans);
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



//	/** For now, a junk function - but meant to replace getTransactionInfo and getVolatilityInfo
//	 * TODO
//	 * 
//	 * @param model
//	 * @param maxTime
//	 */
//	public void addTransactionData(MarketModel model, long maxTime) {
//		//Market Specific Data
//		//For each market in the model
//		for(int mktID : model.getMarketIDs()) {
//			//Getting the transaction data
//			List<PQTransaction> transactions = data.transactionLists.get(mktID);
//			//Truncating the time
//			DescriptiveStatistics stat = new DescriptiveStatistics(transformData(transactions, maxTime));
//		}
//	}
//	
//	/**
//	 * Function that takes in an data set and returns a double[] truncated by maxTime
//	 * Currently takes List<PQTransaction> and HashMap<TimeStamp,Double>
//	 * 
//	 * @param series
//	 * @param maxTime
//	 * @return
//	 */
//	private double[] transformData(List<PQTransaction> series, long maxTime) {
//		int num;
//		for(num=0; num < series.size(); ++num) {
//			if(series.get(num).timestamp.getLongValue() > maxTime) break;
//			++num;
//		}
//		double[] ret = new double[num];
//		for(int i=0; i < num; ++i) {
//			ret[i] = series.get(i).price.getPrice();
//		}
//		return ret;
//	}
	
//	/**
//	 * Construct a double array storing a time series from a HashMap of integers
//	 * hashed by TimeStamp. Starts recording at first time step, even if value
//	 * undefined. Will remove any undefined values from the beginning of the 
//	 * array and cut it off at maxTime.
//	 * 
//	 * @param series
//	 * @param maxTime	-1 if not truncating time series
//	 * @return
//	 */
//	private DescriptiveStatistics transformSeries(HashMap<TimeStamp,Double> series, 
//			long maxTime) {
//		// sort TimeStamps first (not necessarily sorted in HashMap)
//		Set<TimeStamp> times = new TreeSet<TimeStamp>(series.keySet());
//		
//		DescriptiveStatistics ds = new DescriptiveStatistics();
//		TimeStamp prevTime = null;
//		for (Iterator<TimeStamp> it = times.iterator(); it.hasNext();) {
//			if (prevTime == null)
//				prevTime = it.next();
//			else {
//				TimeStamp nextTime = it.next();
//				// fill up to nextTime as long as defined
//				if (series.get(prevTime).intValue() != Consts.INF_PRICE) {
//					for (long i = prevTime.longValue();	i < nextTime.longValue(); i++) {
//						if (i >= maxTime && maxTime > 0) break;
//						ds.addValue(series.get(prevTime));
//					}
//				}
//				prevTime = nextTime;
//			}
//		}
//		// fill up to maxTime
//		if (prevTime.longValue() <= maxTime && maxTime > 0) {
//			if (series.get(prevTime).intValue() != Consts.INF_PRICE) {
//				for (long i = prevTime.longValue(); i < maxTime; i++) {
//					ds.addValue(series.get(prevTime));
//				}
//			}
//		}
//		return ds;
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

	
	
//	/**
//	 * Modifies transactions for the given model by setting TimeStamp to be constant, 
//	 * and using agent log IDs rather than agent IDs for indicating buyer or seller.
//	 * Also sets constant price (because surplus will be same regardless of price), as
//	 * well as constant bid IDs since different bids are submitted to each model.
//	 * 
//	 * @param modelID
//	 * @return
//	 */
//	public ArrayList<PQTransaction> getComparableTrans(int modelID) {
//		ArrayList<Integer> mktIDs = getModel(modelID).getMarketIDs();
//				
//		ArrayList<PQTransaction> trans = new ArrayList<PQTransaction>();
//		for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
//			if (mktIDs.contains(entry.getValue().marketID)) {
//				PQTransaction tr = entry.getValue();
//				
//				// Modify values to be comparable between models
//				// - Use logID rather than agentID
//				// - Set constant TimeStamp
//				// - Set constant marketID
//				// - Set constant price
//				// - Set constant bid IDs
//				// todo for now, ignore quantity
//				int bID = getAgent(tr.buyerID).getLogID();
//				int sID = getAgent(tr.sellerID).getLogID();
//				Price p = new Price(0);
//				TimeStamp ts = new TimeStamp(-1);
//				int mktID = 0;
//				int bBidID = 0;
//				int sBidID = 0;
//				
//				PQTransaction trNew = new PQTransaction(tr.quantity, p, bID, sID, bBidID, sBidID, ts, mktID);
//				trans.add(trNew);
//			}
//		}
//		return trans;
//	}
	
//	/**
//	 * Modifies all transactions in transData to be comparable between models.
//	 * 
//	 * @return
//	 */
//	public ArrayList<PQTransaction> getUniqueComparableTrans() {
//		Set<PQTransaction> trans = new HashSet<PQTransaction>();
//		
//		for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
//			PQTransaction tr = entry.getValue();
//			
//			// Modify values to be comparable between models
//			// - Use logID rather than agentID
//			// - Set constant TimeStamp
//			// - Set constant marketID
//			// - Set constant price
//			// - Set constant bid IDs
//			// - for now, ignore quantity
//			int bID = getAgent(tr.buyerID).getLogID();
//			int sID = getAgent(tr.sellerID).getLogID();
//			Price p = new Price(0);
//			TimeStamp ts = new TimeStamp(-1);
//			int mktID = 0;
//			int bBidID = 0;
//			int sBidID = 0;
//			
//			PQTransaction trNew = new PQTransaction(tr.quantity, p, bID, sID, bBidID, sBidID, ts, mktID);
//			trans.add(trNew);
//		}
//		ArrayList<PQTransaction> uniqueTrans = new ArrayList<PQTransaction>();
//		uniqueTrans.addAll(trans);
//		return uniqueTrans;
//	}
	
//	/**
//	 * @return list of actual private values of all agents
//	 */
//	public ArrayList<Price> getPrivateValues() {
//		ArrayList<Price> pvs = new ArrayList<Price>(numAgents);
//		for (Iterator<Integer> ag = getAgentIDs().iterator(); ag.hasNext(); ) {
//			Price val = agents.get(ag.next()).getPrivateValue();
//			// PV will be null if it doesn't exist for the agent
//			if (val != null) {
//				pvs.add(val);
//			}
//		}
//		return pvs;
//	}
	
}
