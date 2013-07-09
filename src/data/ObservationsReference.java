package data;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import market.PQTransaction;
import market.Transaction;
import model.MarketModel;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.json.simple.JSONObject;

import systemmanager.Consts;
import systemmanager.SimulationSpec;
import utils.MathUtils;
import entity.Agent;
import entity.BackgroundAgent;
import entity.HFTAgent;
import entity.Market;
import entity.MarketMaker;
import event.TimeStamp;
// import java.io.BufferedWriter;
// import java.io.File;
// import java.io.FileWriter;

/**
 * Contains payoff data and features for all players in the simulation. Computes
 * metrics to output to the observation file.
 * 
 * @author ewah, drhurd
 */
public class ObservationsReference {

	private SystemData data;
	private HashMap<String, Object> observations;
	private long maxTime;

	// for truncated series
	public final static String TIMESERIES_MAXTIME = "up_to_maxtime";

	// Constants in observation file
	public final static String PLAYERS_KEY = "players";
	public final static String FEATURES_KEY = "features";
	public final static String ROLE_KEY = "role";
	public final static String PAYOFF_KEY = "payoff";
	public final static String STRATEGY_KEY = "strategy";
	public final static String OBS_KEY = "obs";

	// Descriptors
	public final static String PRICE = "price";
	public final static String QUANTITY = "quantity";
	public final static String SURPLUS = "surplus";
	public final static String EXECTIME = "exectime";
	public final static String TRANSACTIONS = "trans";
	public final static String ROUTING = "routing";
	public final static String SPREADS = "spreads";
	public final static String RETURN = "return";
	public final static String VOL = "vol";
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

	// Prefixes
	public final static String PRE = "pre";
	public final static String POST = "post";
	public final static String MAIN = "main";
	public final static String ALTERNATE = "alt";
	public final static String TRANSACT = "transact";
	public final static String NO_TRANSACT = "notrans";
	public final static String PERIODICITY = "freq";

	// Suffixes
	public final static String AGENTSETUP = "setup";
	public final static String DISCOUNTED = "disc";
	public final static String UNDISCOUNTED = "no" + DISCOUNTED;
	public final static String TOTAL = "total";
	public final static String NBBO = "nbbo";

	/**
	 * Constructor
	 */
	public ObservationsReference(SystemData d) {
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
			System.err.println(this.getClass().getSimpleName()
					+ "::generateObservationFile error");
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

		// set up max time (where most agents have arrived)
		long time = Math.round(data.getNumEnvAgents() / data.arrivalRate);
		maxTime = Math.max(Consts.upToTime, MathUtils.quantize((int) time, 1000));

		addFeature("", getConfiguration());

		// iterate through all models
		for (MarketModel model : data.getModels().values()) {
			String modelName = model.getLogName() + "_";

			addFeature(modelName + SURPLUS, getSurplus(model));
			addFeature(modelName + EXECTIME, getExecutionTime(model));
			addFeature(modelName + TRANSACTIONS, getTransactionInfo(model));
			addFeature(modelName + SPREADS, getSpread(model, maxTime));
			for (int period : Consts.periods) {
				addFeature(modelName + VOL, getVolatility(model, period,
						maxTime));
			}
			addFeature(modelName + Consts.ROLE_MARKETMAKER.toLowerCase(),
					getMarketMakerInfo(model));

			// TODO - need to remove the position balance hack
			// addFeature(modelName + ROUTING, getRegNMSRoutingInfo(model));
		}
	}

	/**
	 * Adds a feature to the observation file. Only add if ft is non-empty.
	 * 
	 * @param description
	 *            key for list of features to insert
	 * @param ft
	 *            Feature to add
	 */
	@SuppressWarnings("unchecked")
	public void addFeature(String description, Feature ft) {
		if (ft.isEmpty())
			return;

		if (!observations.containsKey(FEATURES_KEY)) {
			HashMap<String, Object> feats = new HashMap<String, Object>();
			feats.put(description.toLowerCase(), ft.get());
			observations.put(FEATURES_KEY, feats);
		} else {
			((HashMap<String, Object>) observations.get(FEATURES_KEY)).put(
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
		if (!data.isPlayer(agentID))
			return;

		HashMap<String, Object> obs = new HashMap<String, Object>(); //data.getAgent(agentID).getObservation();
		if (obs == null || obs.isEmpty())
			return;

		// Add agent to list of player observations
		if (!observations.containsKey(PLAYERS_KEY)) {
			ArrayList<Object> array = new ArrayList<Object>();
			array.add(obs);
			observations.put(PLAYERS_KEY, array);
		} else {
			((ArrayList<Object>) observations.get(PLAYERS_KEY)).add(obs);
		}
	}

	/**
	 * For testing: output a TimeSeries object's generated series.
	 * 
	 * @param s
	 * @param type
	 */
	public void outputSeries(TimeSeries s, String type) {
		s.writeSeriesToFile(new File(new File(data.simDir, Consts.logDir), type
				+ ".txt"));
	}

	/**
	 * For testing: output a sampled TimeSeries object: points, and sampled
	 * arrays.
	 * 
	 * @param s
	 * @param type
	 */
	public void outputSampledSeries(TimeSeries s, String type) {

		s.writePointsToCSFile(data.simDir + Consts.logDir + type + "points.csv");
		for (int period : Consts.periods) {
			s.writeSampledSeriesToFile(period, data.simLength.longValue(),
					data.simDir + Consts.logDir + type + "_" + period + ".csv");
		}
	}

	public void outputSampledSeries(int period, long maxTime, TimeSeries s,
			String type) {
		s.writeSampledSeriesToFile(period, maxTime, data.simDir + Consts.logDir
				+ type + "_period" + period + ".csv");
	}

	/********************************************
	 * Data aggregation
	 *******************************************/

	/**
	 * @return HashMap of configuration parameters.
	 */
	public Feature getConfiguration() {
		Feature config = new Feature();
		config.put(OBS_KEY, data.num);
		config.put(SimulationSpec.SIMULATION_LENGTH, data.simLength);
		config.put(SimulationSpec.TICK_SIZE, data.tickSize);
		config.put(SimulationSpec.LATENCY, data.nbboLatency);
		config.put(SimulationSpec.ARRIVAL_RATE, data.arrivalRate);
		config.put(SimulationSpec.REENTRY_RATE, data.reentryRate);
		config.put(SimulationSpec.FUNDAMENTAL_KAPPA, data.kappa);
		config.put(SimulationSpec.FUNDAMENTAL_MEAN, data.meanValue);
		config.put(SimulationSpec.FUNDAMENTAL_SHOCK_VAR, data.shockVar);
		config.put(SimulationSpec.PRIVATE_VALUE_VAR, data.pvVar);

		config.put(TIMESERIES_MAXTIME, maxTime);

		for (AgentPropsPair app : data.getEnvAgentMap().keySet()) {
			String agType = app.getAgentType().toString();
			config.put(agType + "_" + NUM, data.getEnvAgentMap().get(app));
			config.put(agType + "_" + AGENTSETUP,
					app.getProperties().toConfigString());
		}
		return config;
	}

	/**
	 * Execution time metrics.
	 * 
	 * @param model
	 * @return
	 */
	public Feature getExecutionTime(MarketModel model) {
		Feature feat = new Feature();
		DescriptiveStatistics speeds = new DescriptiveStatistics();
		for(Transaction tr : model.getTrans()) {
			TimeStamp execTime = tr.getExecTime();
			TimeStamp buyerExecTime = execTime.diff(tr.getBuyBid().getSubmitTime());
			TimeStamp sellerExecTime = execTime.diff(tr.getSellBid().getSubmitTime());
			for(int q=0; q < tr.getQuantity(); q++) {
				speeds.addValue((double) buyerExecTime.getLongValue());
				speeds.addValue((double) sellerExecTime.getLongValue());
			}
		}
		// feat.addMax(speeds);
		// feat.addMin(speeds);
		feat.addMean(speeds);
		return feat;
	}

	/**
	 * Extract discounted surplus for background agents, including those that
	 * are players (in the EGTA use case).
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
			
			DescriptiveStatistics modelSurplus = new DescriptiveStatistics();
			modelSurplus.addValue(model.getModelSurplus(rho));
			
			feat.addSum("", TOTAL + suffix, modelSurplus);
			
			// sub-categories for surplus (roles)
			DescriptiveStatistics bkgrd = new DescriptiveStatistics();
			DescriptiveStatistics hft = new DescriptiveStatistics();
			DescriptiveStatistics mm = new DescriptiveStatistics();
			DescriptiveStatistics env = new DescriptiveStatistics();
			
			// go through all agents & update for each agent type
			for (Agent ag : model.getAgents()) {
				double val = ag.getSurplus(rho);
				
				if (ag instanceof BackgroundAgent) {
					bkgrd.addValue(val);
				} else if (ag instanceof HFTAgent) {
					val = ag.getRealizedProfit();
					hft.addValue(val);
				} else if (ag instanceof MarketMaker) {
					mm.addValue(val);
				}
				if (data.isEnvironmentAgent(ag.getID())) {
					env.addValue(val);

				feat.addSum("", Consts.ROLE_BACKGROUND.toLowerCase() + suffix,
						bkgrd);
				feat.addSum("", Consts.ROLE_MARKETMAKER.toLowerCase() + suffix,
						mm);
				feat.addSum("", Consts.ROLE_HFT.toLowerCase(), hft);
				if (data.isEGTAUseCase()) { // only add if EGTA use case
					feat.addSum("", Consts.TYPE_ENVIRONMENT.toLowerCase(), env);
				}
				} else {
					System.err.println(this.getClass().getSimpleName()
							+ "::getSurplus: "
							+ "no surplus records found for rho=" + rho);
				}
			}
		}
		return feat;
	}

	/**
	 * Transaction statistics.
	 * 
	 * Note that the DescriptiveStatistics prices object is NOT comparable to
	 * the TimeSeries transPrices; the latter does not allow more than one value
	 * for each TimeStamp and thus it only gives the most recent transaction at
	 * any given time.
	 * 
	 * @param model
	 * @return
	 */
	public Feature getTransactionInfo(MarketModel model) {
		Feature feat = new Feature();

		List<Transaction> trans = new ArrayList<Transaction>(model.getTrans());

		DescriptiveStatistics prices = new DescriptiveStatistics();
		DescriptiveStatistics quantity = new DescriptiveStatistics();
		DescriptiveStatistics fundamental = new DescriptiveStatistics();
		TimeSeries transPrices = new TimeSeries();
		TimeSeries fundPrices = new TimeSeries();

		// number of transactions, hashed by agent type
		HashMap<String, Integer> numTrans = new HashMap<String, Integer>();

		for (Transaction t : trans) {
			PQTransaction tr = (PQTransaction) t;
			prices.addValue(tr.getPrice().getPrice());
			quantity.addValue(tr.getQuantity());
			fundamental.addValue(model.getFundamentalAt(tr.getExecTime()).getPrice());

			transPrices.add(tr.getExecTime(), new Double(tr.getPrice().getPrice()));
			fundPrices.add(tr.getExecTime(), new Double(model.getFundamentalAt(
					tr.getExecTime()).getPrice()));

			// update number of transactions
			// buyer
			for (Agent ag : Arrays.asList(tr.getBuyer(), tr.getSeller())) {
				if (model.getAgents().contains(ag)) {
					String type = ag.getType();
					int num = 0;
					if (numTrans.containsKey(type)) {
						num += numTrans.get(type);
					}
					numTrans.put(type, num + 1);
				}
			}
		}
		feat.addMean(PRICE, "", prices);
		feat.addStdDev(PRICE, "", prices);

		// number of transactions for each agent type
		for (String type : numTrans.keySet()) {
			feat.put(type.toLowerCase() + "_" + NUM, numTrans.get(type));
		}

		// compute RMSD (for price discovery) at different sampling frequencies
		for (int period : Consts.periods) {
			String prefix = "";
			if (period > 1) {
				prefix += PERIODICITY + period;
			}
			double[] pr = transPrices.getSampledArray(period,
					data.simLength.longValue());
			double[] fund = fundPrices.getSampledArray(period,
					data.simLength.longValue());
			feat.addRMSD(prefix, "", new DescriptiveStatistics(pr),
					new DescriptiveStatistics(fund));
		}
		return feat;
	}

	/**
	 * Computes spread metrics for the given model.
	 * 
	 * NOTE: The computed median will include NaNs in the list of numbers.
	 * 
	 * @param model
	 * @param maxTime
	 * @return
	 */
	public Feature getSpread(MarketModel model, long maxTime) {
		Feature feat = new Feature();

		DescriptiveStatistics medians = new DescriptiveStatistics();
		for(Market market : model.getMarkets()) {
			TimeSeries s = market.getSpread();
//			if (!s.isEmpty()) {
				double[] array = s.getSampledArray(0, maxTime);
				DescriptiveStatistics spreads = new DescriptiveStatistics(array);
				double med = feat.addMedian("", MARKET + (-market.getID()) + "_"
						+ TIMESERIES_MAXTIME, spreads);
				medians.addValue(med);
//			}
		}
		// average of median market spreads (for all markets in this model)
		feat.addMean("", MARKET + "_" + TIMESERIES_MAXTIME, medians);

		TimeSeries nbbo = model.getNBBOSpreads();
		if (nbbo != null) {
			double[] array = nbbo.getSampledArray(0, maxTime);
			DescriptiveStatistics spreads = new DescriptiveStatistics(array);
			feat.addMedian("", NBBO + "_" + TIMESERIES_MAXTIME, spreads);
		}
		return feat;
	}

	/**
	 * Computes volatility metrics for time 0 to maxTime, sampled at interval
	 * specified by period. Volatility is measured as:
	 * 
	 * - log of standard deviation of time series of mid-quote prices - standard
	 * deviation of log returns
	 * 
	 * @param model
	 * @param period
	 * @param maxTime
	 *            (inclusive)
	 * @return
	 */
	public Feature getVolatility(MarketModel model, int period, long maxTime) {
		Feature feat = new Feature();
		String prefix = "";
		if (period > 1)
			prefix += PERIODICITY + period;

		DescriptiveStatistics stddev = new DescriptiveStatistics();
		DescriptiveStatistics logPriceVol = new DescriptiveStatistics();
		DescriptiveStatistics logRetVol = new DescriptiveStatistics();
		for (Market market : model.getMarkets()) {
			String suffix = "_" + MARKET + (-market.getID());

			TimeSeries ma = market.getMidQuotes();
//			if (!ma.isEmpty()) { // can't be empty.
				double[] mid = ma.getSampledArrayWithoutNaNs(period, maxTime);

				// compute log price volatility for this market
				DescriptiveStatistics mktPrices = new DescriptiveStatistics(mid);
				double s = feat.addStdDev(prefix, PRICE + suffix, mktPrices);
				stddev.addValue(s);
				if (s != 0) {
					logPriceVol.addValue(Math.log(s));
				} // don't add if stddev is 0

				// compute log-return volatility for this market
				double[] midquote = ma.getSampledArray(period, maxTime);
				int size = midquote.length;
				if (midquote.length > 0)
					size = midquote.length - 1;

				double[] logReturns = new double[size];
				DescriptiveStatistics mktLogReturns = new DescriptiveStatistics();
				for (int i = 1; i < midquote.length; i++) {
					logReturns[i - 1] = Math.log(midquote[i] / midquote[i - 1]);
					if (!Double.isNaN(logReturns[i - 1])) {
						mktLogReturns.addValue(logReturns[i - 1]);
					}
				}
				logRetVol.addValue(feat.addStdDev(prefix,
						LOG + RETURN + suffix, mktLogReturns));
//			}
		}
		// average measures across all markets in this model
		feat.addMean(prefix, Feature.STDDEV + PRICE, stddev);
		feat.addMean(prefix, LOG + PRICE, logPriceVol);
		feat.addMean(prefix, LOG + RETURN, logRetVol);
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

		for (Agent ag : model.getAgents()) {
			// must check that background agent (affected by routing)
			if (ag instanceof BackgroundAgent) {
				BackgroundAgent b = (BackgroundAgent) ag;
				if (b.getMarket().getID() != b.getMarketSubmittedBid().getID()) {
					// order routed to alternate market
					numAlt++;
					if (b.getPositionBalance() == 0)
						numAltNoTrans++;
					else
						numAltTrans++;
				} else {
					// order was not routed
					numMain++;
					if (b.getPositionBalance() == 0)
						numMainNoTrans++;
					else
						numMainTrans++;
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
		for (Agent ag : model.getAgents()) {
			if (ag instanceof MarketMaker) {
				// append the agentID in case more than one of this type
				String suffix = Integer.toString(ag.getID());
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

	// /**
	// * Computes depth metrics the given market IDs.
	// *
	// * @param model
	// * @return
	// */
	// public HashMap<String, Object> getDepthInfo(MarketModel model) {
	// HashMap<String, Object> feat = new HashMap<String, Object>();
	//
	// ArrayList<Integer> ids = model.getMarketIDs();
	//
	// for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
	// int mktID = it.next();
	// HashMap<TimeStamp, Double> marketDepth = data.marketDepth
	// .get(mktID);
	// if (marketDepth != null) {
	// double[] depths = extractTimeSeries(marketDepth);
	// addStatistics(feat, depths, "mkt" + (-mktID), true);
	// }
	// }
	// return feat;
	// }
	//
	// /**
	// * Extracts features of a list of times (e.g. intervals, arrival times).
	// *
	// * @param allTimes
	// * ArrayList of TimeStamps
	// * @return
	// */
	// public HashMap<String, Object> getTimeStampFeatures(
	// ArrayList<TimeStamp> allTimes) {
	// HashMap<String, Object> feat = new HashMap<String, Object>();
	//
	// Object[] times = allTimes.toArray();
	// double[] values = new double[times.length];
	// for (int i = 0; i < values.length; i++) {
	// TimeStamp tmp = (TimeStamp) times[i];
	// values[i] = (double) tmp.longValue();
	// }
	// addAllStatistics(feat, values, "");
	// return feat;
	// }
	//
	// /**
	// * Buckets based on comparison with the performance in the centralized
	// call
	// * market model. For each non-CentralCall model, checks if a transaction
	// * occurred in that model and whether or not it occurred in the
	// CentralCall
	// * model.
	// *
	// * Adds features directly to the Observations object.
	// */
	// public void addTransactionComparison() {
	//
	// // set as baseline the centralized call market model
	// int baseID = 0;
	//
	// // hashed by modelID
	// HashMap<Integer, ModelComparison> bucketMap = new HashMap<Integer,
	// ModelComparison>();
	// HashMap<Integer, ArrayList<PQTransaction>> transMap = new
	// HashMap<Integer, ArrayList<PQTransaction>>();
	// for (Map.Entry<Integer, MarketModel> entry : data.getModels()
	// .entrySet()) {
	// MarketModel model = entry.getValue();
	// int modelID = entry.getKey();
	// // Set base (centralized call market) model ID
	// if (model instanceof CentralCall) {
	// baseID = modelID;
	// }
	// transMap.put(modelID, data.getComparableTrans(model.getID()));
	// bucketMap.put(modelID, new ModelComparison());
	// }
	//
	// // iterate through all transactions, and identify the model
	// ArrayList<PQTransaction> uniqueTrans = data.getUniqueComparableTrans();
	// for (Iterator<PQTransaction> it = uniqueTrans.iterator(); it.hasNext();)
	// {
	// PQTransaction trans = it.next();
	//
	// for (Iterator<Integer> id = data.getModelIDs().iterator(); id
	// .hasNext();) {
	// int modelID = id.next();
	//
	// ArrayList<PQTransaction> baseTrans = transMap.get(baseID);
	// ArrayList<PQTransaction> modelTrans = transMap.get(modelID);
	//
	// if (modelTrans.contains(trans) && baseTrans.contains(trans)) {
	// bucketMap.get(modelID).YY++;
	// } else if (!modelTrans.contains(trans)
	// && baseTrans.contains(trans)) {
	// bucketMap.get(modelID).NY++;
	// } else if (modelTrans.contains(trans)
	// && !baseTrans.contains(trans)) {
	// bucketMap.get(modelID).YN++;
	// } else if (!modelTrans.contains(trans)
	// && !baseTrans.contains(trans)) {
	// bucketMap.get(modelID).NN++;
	// }
	// }
	//
	// }
	// // add as feature
	// for (Iterator<Integer> id = data.getModelIDs().iterator(); id.hasNext();)
	// {
	// int modelID = id.next();
	// String prefix = data.getModel(modelID).getLogName() + "_";
	// HashMap<String, Object> feat = new HashMap<String, Object>();
	//
	// ModelComparison mc = bucketMap.get(modelID);
	// feat.put("YY", mc.YY);
	// feat.put("YN", mc.YN);
	// feat.put("NY", mc.NY);
	// feat.put("NN", mc.NN);
	// this.addFeature(prefix + "compare", feat);
	// }
	// }

	// /**
	// * Modifies transactions for the given model by setting TimeStamp to be
	// constant,
	// * and using agent log IDs rather than agent IDs for indicating buyer or
	// seller.
	// * Also sets constant price (because surplus will be same regardless of
	// price), as
	// * well as constant bid IDs since different bids are submitted to each
	// model.
	// *
	// * @param modelID
	// * @return
	// */
	// public ArrayList<PQTransaction> getComparableTrans(int modelID) {
	// ArrayList<Integer> mktIDs = getModel(modelID).getMarketIDs();
	//
	// ArrayList<PQTransaction> trans = new ArrayList<PQTransaction>();
	// for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
	// if (mktIDs.contains(entry.getValue().marketID)) {
	// PQTransaction tr = entry.getValue();
	//
	// // Modify values to be comparable between models
	// // - Use logID rather than agentID
	// // - Set constant TimeStamp
	// // - Set constant marketID
	// // - Set constant price
	// // - Set constant bid IDs
	// // todo for now, ignore quantity
	// int bID = getAgent(tr.buyerID).getLogID();
	// int sID = getAgent(tr.sellerID).getLogID();
	// Price p = new Price(0);
	// TimeStamp ts = new TimeStamp(-1);
	// int mktID = 0;
	// int bBidID = 0;
	// int sBidID = 0;
	//
	// PQTransaction trNew = new PQTransaction(tr.quantity, p, bID, sID, bBidID,
	// sBidID, ts, mktID);
	// trans.add(trNew);
	// }
	// }
	// return trans;
	// }

	// /**
	// * Modifies all transactions in transData to be comparable between models.
	// *
	// * @return
	// */
	// public ArrayList<PQTransaction> getUniqueComparableTrans() {
	// Set<PQTransaction> trans = new HashSet<PQTransaction>();
	//
	// for (Map.Entry<Integer,PQTransaction> entry : transactions.entrySet()) {
	// PQTransaction tr = entry.getValue();
	//
	// // Modify values to be comparable between models
	// // - Use logID rather than agentID
	// // - Set constant TimeStamp
	// // - Set constant marketID
	// // - Set constant price
	// // - Set constant bid IDs
	// // - for now, ignore quantity
	// int bID = getAgent(tr.buyerID).getLogID();
	// int sID = getAgent(tr.sellerID).getLogID();
	// Price p = new Price(0);
	// TimeStamp ts = new TimeStamp(-1);
	// int mktID = 0;
	// int bBidID = 0;
	// int sBidID = 0;
	//
	// PQTransaction trNew = new PQTransaction(tr.quantity, p, bID, sID, bBidID,
	// sBidID, ts, mktID);
	// trans.add(trNew);
	// }
	// ArrayList<PQTransaction> uniqueTrans = new ArrayList<PQTransaction>();
	// uniqueTrans.addAll(trans);
	// return uniqueTrans;
	// }

	// /**
	// * @return list of actual private values of all agents
	// */
	// public ArrayList<Price> getPrivateValues() {
	// ArrayList<Price> pvs = new ArrayList<Price>(numAgents);
	// for (Iterator<Integer> ag = getAgentIDs().iterator(); ag.hasNext(); ) {
	// Price val = agents.get(ag.next()).getPrivateValue();
	// // PV will be null if it doesn't exist for the agent
	// if (val != null) {
	// pvs.add(val);
	// }
	// }
	// return pvs;
	// }

}
