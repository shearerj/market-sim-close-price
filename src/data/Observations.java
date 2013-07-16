package data;

import static utils.MathUtils.quantize;
import static utils.StringUtils.delimit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import market.PQTransaction;
import market.Transaction;
import model.MarketModel;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import systemmanager.Consts;
import systemmanager.SimulationSpec;
import utils.DSPlus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

import entity.Agent;
import entity.BackgroundAgent;
import entity.HFTAgent;
import entity.Market;
import entity.MarketMaker;
import event.TimeStamp;

// TODO This current version of observations doesn't round doubles to the nearest ten thousandth
public class Observations {

	// Players
	public final static String PLAYERS_KEY = "players";
	public final static String ROLE_KEY = "role";
	public final static String PAYOFF_KEY = "payoff";
	public final static String STRATEGY_KEY = "strategy";

	// Config
	public final static String NUM = "num";
	public final static String OBS_KEY = "observation";
	public final static String AGENTSETUP = "setup";
	public final static String TIMESERIES_MAXTIME = "up_to_maxtime";

	// Features
	public final static String SURPLUS = "surplus";
	public final static String EXECTIME = "exectime";
	public final static String TRANSACTIONS = "trans";
	public final static String SPREADS = "spreads";
	public final static String VOL = "vol";

	// FEATURE
	public final static String MEAN = "mean";
	public final static String MAX = "max";
	public final static String MIN = "min";
	public final static String SUM = "sum";
	public final static String MEDIAN = "med";
	public final static String VARIANCE = "var";
	public final static String STDDEV = "std";
	public final static String RMSD = "rmsd";

	// SURPLUS
	public final static String DISCOUNTED = "disc";
	public final static String UNDISCOUNTED = "no" + DISCOUNTED;
	public final static String TOTAL = "total";
	public final static String ROLE_HFT = "hft";
	public final static String ROLE_MARKETMAKER = "marketmaker";
	public final static String ROLE_BACKGROUND = "background";
	public final static String TYPE_ENVIRONMENT = "env";
	public final static String TYPE_PLAYER = "player";

	// TRANSACTION INFO
	public final static String PRICE = "price";
	public final static String PERIODICITY = "freq";

	// SPREAD
	public final static String NBBO = "nbbo";
	public final static String MARKET = "mkt";

	// MARKETMAKER
	public final static String POSITION = "position";
	public final static String PROFIT = "profit";
	public final static String LIQUIDATION = "liq";
	public final static String PRE = "pre";
	public final static String POST = "post";

	// VOLITILITY
	public final static String RETURN = "return";
	public final static String LOG = "log";

	protected static transient final Gson gson = new Gson();

	JsonObject observations;

	public Observations(SimulationSpec spec, Collection<MarketModel> models,
			int observationNum) {
		observations = new JsonObject();

		MarketModel firstModel = models.iterator().next();
		observations.add(PLAYERS_KEY, playerObservations(firstModel));

		// FIXME Math.round(firstModel.getNumEnvAgents() / data.arrivalRate);
		long maxTime = 15000;
		maxTime = Math.max(Consts.upToTime, quantize((int) maxTime, 1000));

		JsonObject feature = new JsonObject();
		feature.add("", getConfiguration(spec, observationNum, maxTime));

		for (MarketModel model : models) {
			String modelName = model.getClass().getSimpleName().toLowerCase();

			feature.add(delimit(modelName, SURPLUS), getSurplus(model));

			feature.add(delimit(modelName, EXECTIME), getExecutionTime(model));
			// FIXME this should maybe be simLength instead of maxTime...
			feature.add(delimit(modelName, TRANSACTIONS),
					getTransactionInfo(model, maxTime));
			feature.add(delimit(modelName, SPREADS), getSpread(model, maxTime));
			feature.add(delimit(modelName, ROLE_MARKETMAKER),
					getMarketMakerInfo(model));

			for (int period : Consts.periods)
				feature.add(delimit(modelName, VOL),
						getVolatility(model, period, maxTime));

			// TODO - need to remove the position balance hack
			// addFeature(modelName + ROUTING, getRegNMSRoutingInfo(model));
		}
	}

	protected JsonArray playerObservations(MarketModel model) {
		JsonArray players = new JsonArray();
		for (Player player : model.getPlayers())
			players.add(getPlayerObservation(player));
		return players;
	}

	protected static JsonObject getPlayerObservation(Player player) {
		JsonObject observation = new JsonObject();
		observation.addProperty(ROLE_KEY, player.getRole());
		observation.addProperty(STRATEGY_KEY, player.getStrategy());
		// Get surplus instead of realized profit?
		observation.addProperty(PAYOFF_KEY,
				player.getAgent().getRealizedProfit());
		return observation;
	}

	/********************************************
	 * Data aggregation
	 *******************************************/

	protected static JsonObject getConfiguration(SimulationSpec spec,
			int observationNum, long maxTime) {
		JsonObject config = new JsonObject();

		config.addProperty(OBS_KEY, observationNum);
		config.addProperty(TIMESERIES_MAXTIME, maxTime);
		copyPropertiesToJson(config, spec.getSimulationProperties());
		copyPropertiesToJson(config, spec.getDefaultModelProperties());
		copyPropertiesToJson(config, spec.getDefaultAgentProperties());

		for (Entry<AgentProperties, Integer> agentProps : spec.getBackgroundAgents().entrySet()) {
			AgentProperties props = agentProps.getKey();
			int number = agentProps.getValue();

			config.addProperty(
					delimit("_", props.getAgentType().toString(), NUM), number);
			config.addProperty(
					delimit("_", props.getAgentType().toString(), AGENTSETUP),
					props.toConfigString());
		}
		return config;
	}

	protected static void copyPropertiesToJson(JsonObject json,
			EntityProperties props) {
		for (String key : props.keys())
			json.addProperty(key, props.getAsString(key));
	}

	/**
	 * Extract discounted surplus for background agents, including those that
	 * are players (in the EGTA use case).
	 */
	protected static JsonObject getSurplus(MarketModel model) {
		JsonObject feat = new JsonObject();

		for (double rho : Consts.rhos) {
			String suffix = rho == 0 ? UNDISCOUNTED : DISCOUNTED + rho;

			DescriptiveStatistics modelSurplus = new DescriptiveStatistics();
			// sub-categories for surplus (roles)
			DescriptiveStatistics bkgrd = new DescriptiveStatistics();
			DescriptiveStatistics hft = new DescriptiveStatistics();
			DescriptiveStatistics mm = new DescriptiveStatistics();
			DescriptiveStatistics env = new DescriptiveStatistics();

			Set<Agent> players = new HashSet<Agent>();
			for (Player p : model.getPlayers())
				players.add(p.getAgent());

			// go through all agents & update for each agent type
			for (Agent ag : model.getAgents()) {
				double agentSurplus = ag.getSurplus(rho);
				modelSurplus.addValue(agentSurplus);

				if (ag instanceof BackgroundAgent) {
					bkgrd.addValue(agentSurplus);
				} else if (ag instanceof HFTAgent) {
					agentSurplus = ag.getRealizedProfit();
					hft.addValue(agentSurplus);
				} else if (ag instanceof MarketMaker) {
					mm.addValue(agentSurplus);
				}
				if (!players.contains(ag)) {
					env.addValue(agentSurplus);
				}
			}

			feat.addProperty(delimit("_", SUM, TOTAL, suffix),
					modelSurplus.getSum());
			feat.addProperty(delimit("_", SUM, ROLE_BACKGROUND, suffix),
					bkgrd.getSum());
			feat.addProperty(delimit("_", SUM, ROLE_MARKETMAKER, suffix),
					mm.getSum());
			feat.addProperty(delimit("_", SUM, ROLE_HFT, suffix), hft.getSum());
			if (!players.isEmpty())
			// only add if EGTA use case
				feat.addProperty(delimit("_", SUM, TYPE_ENVIRONMENT, suffix),
						env.getSum());
		}
		return feat;
	}

	/**
	 * Execution time metrics.
	 */
	protected static JsonObject getExecutionTime(MarketModel model) {
		JsonObject feat = new JsonObject();
		DescriptiveStatistics speeds = new DescriptiveStatistics();

		for (Transaction tr : model.getTrans()) {
			TimeStamp execTime = tr.getExecTime();
			TimeStamp buyerExecTime = execTime.minus(tr.getBuyBid().getSubmitTime());
			TimeStamp sellerExecTime = execTime.minus(tr.getSellBid().getSubmitTime());
			for (int quantity = 0; quantity < tr.getQuantity(); quantity++) {
				speeds.addValue((double) buyerExecTime.longValue());
				speeds.addValue((double) sellerExecTime.longValue());
			}
		}

		feat.addProperty(MEAN, speeds.getMean());
		// feat.addProperty(MIN, speeds.getMin());
		// feat.addProperty(MAX, speeds.getMax());
		return feat;
	}

	/**
	 * Transaction statistics.
	 * 
	 * Note that the DescriptiveStatistics prices object is NOT comparable to
	 * the TimeSeries transPrices; the latter does not allow more than one value
	 * for each TimeStamp and thus it only gives the most recent transaction at
	 * any given time.
	 */
	protected static JsonObject getTransactionInfo(MarketModel model,
			long simLength) {
		JsonObject feat = new JsonObject();

		DescriptiveStatistics prices = new DescriptiveStatistics();
		DescriptiveStatistics quantity = new DescriptiveStatistics();
		DescriptiveStatistics fundamental = new DescriptiveStatistics();

		TimeSeries transPrices = new TimeSeries();
		TimeSeries fundPrices = new TimeSeries();

		// number of transactions, hashed by agent type
		Map<String, Integer> numTrans = new HashMap<String, Integer>();

		// So that agent types with 0 transactions still get logged. XXX This
		// maybe should pull only from agent types that had nonzero numbers, not
		// every agent possible.
		for (Agent agent : model.getAgents())
			numTrans.put(agent.getName(), 0);

		for (Transaction t : model.getTrans()) {
			// FIXME If these are always PQTrans then we should store that not
			// generic transactions, or add the common functionality to generic
			// transactions
			PQTransaction tr = (PQTransaction) t;
			prices.addValue(tr.getPrice().getPrice());
			quantity.addValue(tr.getQuantity());
			fundamental.addValue(model.getFundamentalAt(tr.getExecTime()).getPrice());

			transPrices.add(tr.getExecTime(), tr.getPrice().getPrice());
			fundPrices.add(tr.getExecTime(),
					model.getFundamentalAt(tr.getExecTime()).getPrice());

			// update number of transactions
			// buyer
			for (Agent agent : new Agent[] { tr.getBuyer(), tr.getSeller() }) {
				if (model.getAgents().contains(agent)) {
					String name = agent.getName();
					numTrans.put(name, numTrans.get(name) + 1);
				}
			}
		}
		feat.addProperty(delimit("_", MEAN, PRICE), prices.getMean());
		feat.addProperty(delimit("_", STDDEV, PRICE),
				prices.getStandardDeviation());

		// number of transactions for each agent type
		for (Entry<String, Integer> e : numTrans.entrySet())
			feat.addProperty(delimit("_", e.getKey().toLowerCase(), NUM),
					e.getValue());

		// compute RMSD (for price discovery) at different sampling frequencies
		for (int period : Consts.periods) {
			String prefix = period > 1 ? PERIODICITY + period : null;
			// XXX maxTime instead of simLength?
			DSPlus pr = new DSPlus(transPrices.getSampledArray(period,
					simLength));
			DSPlus fund = new DSPlus(fundPrices.getSampledArray(period,
					simLength));

			feat.addProperty(delimit("_", prefix, RMSD), pr.getRMSD(fund));
		}
		return feat;
	}

	/**
	 * Computes spread metrics for the given model.
	 * 
	 * NOTE: The computed median will include NaNs in the list of numbers.
	 * 
	 * FIXME Is this correct? The NaN's appear randomly dispersed, so the median
	 * calculation depends on where in the original series the NaN's occurred.
	 * This definitely doesn't make sense
	 */
	protected static JsonObject getSpread(MarketModel model, long maxTime) {
		JsonObject feat = new JsonObject();

		DescriptiveStatistics medians = new DescriptiveStatistics();
		for (Market market : model.getMarkets()) {
			TimeSeries s = market.getSpread();

			double[] array = s.getSampledArray(0, maxTime);
			DSPlus spreads = new DSPlus(array);
			double med = spreads.getMedian();
			feat.addProperty(
					delimit("_", MEDIAN, MARKET + Math.abs(market.getID()),
							TIMESERIES_MAXTIME), med);
			medians.addValue(med);
		}

		// average of median market spreads (for all markets in this model)
		feat.addProperty(delimit("_", MEAN, MARKET, TIMESERIES_MAXTIME),
				medians.getMean());

		TimeSeries nbbo = model.getSIP().getNBBOSpreads();

		double[] array = nbbo.getSampledArray(0, maxTime);
		DSPlus spreads = new DSPlus(array);
		feat.addProperty(delimit("_", MEDIAN, NBBO, TIMESERIES_MAXTIME),
				spreads.getMedian());

		return feat;
	}

	/**
	 * Track liquidation-related features & profit for market makers.
	 */
	protected static JsonObject getMarketMakerInfo(MarketModel model) {
		JsonObject feat = new JsonObject();

		for (Agent ag : model.getAgents()) {
			if (!(ag instanceof MarketMaker)) continue;
			MarketMaker mm = (MarketMaker) ag;

			// append the agentID in case more than one of this type
			String suffix = Integer.toString(mm.getID());
			feat.addProperty(delimit("_", POSITION, PRE, LIQUIDATION + suffix),
					mm.getPreLiquidationPosition());
			// just to double-check, should be 0 position after liquidation
			feat.addProperty(
					delimit("_", POSITION, POST, LIQUIDATION + suffix),
					mm.getPositionBalance());
			feat.addProperty(delimit("_", PROFIT, PRE, LIQUIDATION + suffix),
					mm.getPreLiquidationProfit());
			feat.addProperty(delimit("_", PROFIT, POST, LIQUIDATION + suffix),
					mm.getRealizedProfit());
		}
		return feat;
	}

	/**
	 * Computes volatility metrics for time 0 to maxTime (inclusive), sampled at
	 * interval specified by period. Volatility is measured as:
	 * 
	 * - log of standard deviation of time series of mid-quote prices
	 * 
	 * - standard deviation of log returns
	 */
	protected static JsonObject getVolatility(MarketModel model, int period,
			long maxTime) {
		JsonObject feat = new JsonObject();

		String prefix = period == 0 ? null : PERIODICITY + period;

		DescriptiveStatistics stddev = new DescriptiveStatistics();
		DescriptiveStatistics logPriceVol = new DescriptiveStatistics();
		DescriptiveStatistics logRetVol = new DescriptiveStatistics();

		for (Market market : model.getMarkets()) {
			String suffix = MARKET + Math.abs(market.getID());

			TimeSeries mq = market.getMidQuotes();

			double[] mid = mq.getSampledArrayWithoutNaNs(period, maxTime);

			// compute log price volatility for this market
			DescriptiveStatistics mktPrices = new DescriptiveStatistics(mid);
			double stdev = mktPrices.getStandardDeviation();
			feat.addProperty(delimit("_", prefix, STDDEV, PRICE, suffix), stdev);
			stddev.addValue(stdev);
			if (stdev != 0)
			// don't add if stddev is 0
				logPriceVol.addValue(Math.log(stdev));

			// compute log-return volatility for this market
			double[] midquote = mq.getSampledArray(period, maxTime);
			int size = midquote.length == 0 ? 0 : midquote.length - 1;

			double[] logReturns = new double[size];
			DescriptiveStatistics mktLogReturns = new DescriptiveStatistics();
			// FIXME why do we ignore the first point.
			for (int i = 1; i < midquote.length; i++) {
				logReturns[i - 1] = Math.log(midquote[i] / midquote[i - 1]);
				if (!Double.isNaN(logReturns[i - 1])) {
					mktLogReturns.addValue(logReturns[i - 1]);
				}
			}
			double logStdev = mktLogReturns.getStandardDeviation();
			feat.addProperty(
					delimit("_", prefix, STDDEV, LOG + RETURN, suffix),
					logStdev);
			logRetVol.addValue(logStdev);
		}

		// average measures across all markets in this model
		feat.addProperty(delimit("_", prefix, MEAN, STDDEV + PRICE),
				stddev.getMean());
		feat.addProperty(delimit("_", prefix, MEAN, LOG + PRICE),
				logPriceVol.getMean());
		feat.addProperty(delimit("_", prefix, MEAN, LOG + RETURN),
				logRetVol.getMean());

		return feat;
	}

	public void writeToFile(File observationsFile) throws JsonIOException,
			IOException {
		Writer writer = null;
		try {
			writer = new FileWriter(observationsFile);
			gson.toJson(observations, writer);
		} finally {
			if (writer != null) writer.close();
		}
	}

}
