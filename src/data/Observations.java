package data;

import static utils.MathUtils.quantize;
import static utils.StringUtils.delimit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import systemmanager.Consts;
import systemmanager.Keys;
import systemmanager.SimulationSpec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;

import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.HFTAgent;
import entity.agent.MarketMaker;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

// TODO This current version of observations doesn't round doubles to the nearest ten thousandth
public class Observations {

	// Players
	public final static String PLAYERS = "players";
	public final static String ROLE = "role";
	public final static String PAYOFF = "payoff";
	public final static String STRATEGY = "strategy";

	// Config
	public final static String NUM = "num";
	public final static String OBS_KEY = "observation";
	public final static String AGENTSETUP = "setup";
	public final static String TIMESERIES_MAXTIME = "up_to_maxtime";

	// Features
	public final static String FEATURES = "features";
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

	public Observations(SimulationSpec spec, Collection<Market> markets,
			Collection<Agent> agents, Collection<Player> players,
			FundamentalValue fundamental, SIP sip, String modelName,
			int observationNum) {
		
		List<Transaction> transactions = new ArrayList<Transaction>();
		for (Market market : markets)
			transactions.addAll(market.getTransactions());
		// FIXME Since all market transactions are sorted, this can be done in linear time
		Collections.sort(transactions, new Comparator<Transaction>() {
			public int compare(Transaction o1, Transaction o2) {
				return o1.getExecTime().compareTo(o2.getExecTime());
			}
		});
		
		observations = new JsonObject();

		observations.add(PLAYERS, playerObservations(players));

		double arrivalRate = spec.getDefaultAgentProps().getAsDouble(Keys.ARRIVAL_RATE, 0.075);
		long maxTime = Math.round(agents.size() / arrivalRate);
		maxTime = Math.max(Consts.upToTime, quantize((int) maxTime, 1000));

		JsonObject features = new JsonObject();
		observations.add(FEATURES, features);
		features.add("", getConfiguration(spec, observationNum, maxTime));

		features.add(modelName + "_" + SURPLUS, getSurplus(agents, players));

		features.add(modelName + "_" + EXECTIME, getExecutionTime(transactions));
		// FIXME this should maybe be simLength instead of maxTime...
		features.add(modelName + "_" + TRANSACTIONS,
				getTransactionInfo(agents, transactions, fundamental, maxTime));
		features.add(modelName + "_" + SPREADS, getSpread(markets, sip, maxTime));
		features.add(modelName + "_" + ROLE_MARKETMAKER,
				getMarketMakerInfo(agents));

		for (int period : Consts.periods)
			features.add(modelName + "_" + VOL,
					getVolatility(markets, period, maxTime));

		// TODO - need to remove the position balance hack
		// addFeature(modelName + ROUTING, getRegNMSRoutingInfo(model));
	}

	protected JsonArray playerObservations(Collection<Player> players) {
		JsonArray obs = new JsonArray();
		for (Player player : players)
			obs.add(getPlayerObservation(player));
		return obs;
	}

	protected static JsonObject getPlayerObservation(Player player) {
		JsonObject observation = new JsonObject();
		observation.addProperty(ROLE, player.getRole());
		observation.addProperty(STRATEGY, player.getStrategy());
		// Get surplus instead of realized profit?
		observation.addProperty(PAYOFF,
				player.getAgent().getSurplus(0));
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
		copyPropertiesToJson(config, spec.getSimulationProps());
		copyPropertiesToJson(config, spec.getDefaultMarketProps());
		copyPropertiesToJson(config, spec.getDefaultAgentProps());

		for (AgentProperties props : spec.getAgentProps()) {
			// TODO Change to not split off number...
			int number = props.getAsInt(Keys.NUM, 0);

			config.addProperty(props.getAgentType() + "_" + NUM, number);
			config.addProperty(props.getAgentType() + "_" + AGENTSETUP,
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
	protected static JsonObject getSurplus(Collection<Agent> agents, Collection<Player> players) {
		JsonObject feat = new JsonObject();

		for (double rho : Consts.rhos) {
			String suffix = rho == 0 ? UNDISCOUNTED : DISCOUNTED + rho;

			DescriptiveStatistics modelSurplus = new DescriptiveStatistics();
			// sub-categories for surplus (roles)
			DescriptiveStatistics bkgrd = new DescriptiveStatistics();
			DescriptiveStatistics hft = new DescriptiveStatistics();
			DescriptiveStatistics mm = new DescriptiveStatistics();
			DescriptiveStatistics env = new DescriptiveStatistics();

			Set<Agent> playerSet = new HashSet<Agent>();
			for (Player p : players)
				playerSet.add(p.getAgent());

			// go through all agents & update for each agent type
			for (Agent agent : agents) {
				double agentSurplus = agent.getSurplus(rho);
				modelSurplus.addValue(agentSurplus);

				if (agent instanceof BackgroundAgent) {
					bkgrd.addValue(agentSurplus);
				} else if (agent instanceof HFTAgent) {
					// FIXME This is not correct if the agent has a net position at the end...
					agentSurplus = agent.getSurplus(0);
					hft.addValue(agentSurplus);
				} else if (agent instanceof MarketMaker) {
					mm.addValue(agentSurplus);
				}
				if (!playerSet.contains(agent)) {
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
	protected static JsonObject getExecutionTime(Collection<Transaction> transactions) {
		JsonObject feat = new JsonObject();
		DescriptiveStatistics speeds = new DescriptiveStatistics();

		for (Transaction trans : transactions) {
			TimeStamp execTime = trans.getExecTime();
			TimeStamp buyerExecTime = execTime.minus(trans.getBuyBid().getSubmitTime());
			TimeStamp sellerExecTime = execTime.minus(trans.getSellBid().getSubmitTime());
			for (int quantity = 0; quantity < trans.getQuantity(); quantity++) {
				speeds.addValue((double) buyerExecTime.getInTicks());
				speeds.addValue((double) sellerExecTime.getInTicks());
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
	protected static JsonObject getTransactionInfo(Collection<Agent> agents, Collection<Transaction> transactions, FundamentalValue fund,
			long simLength) {
		JsonObject feat = new JsonObject();
		// TODO Make sure Agents is a set...

		DescriptiveStatistics prices = new DescriptiveStatistics();
		DescriptiveStatistics quantity = new DescriptiveStatistics();
		DescriptiveStatistics fundamental = new DescriptiveStatistics();

		TimeSeries transPrices = new TimeSeries();
		TimeSeries fundPrices = new TimeSeries();

		// number of transactions, hashed by agent type
		Map<String, Integer> numTrans = new HashMap<String, Integer>();

		// So that agent types with 0 transactions still get logged.
		for (Agent agent : agents)
			numTrans.put(agent.getName(), 0);

		for (Transaction trans : transactions) {
			prices.addValue(trans.getPrice().getInTicks());
			quantity.addValue(trans.getQuantity());
			fundamental.addValue(fund.getValueAt(trans.getExecTime()).getInTicks());

			transPrices.add((int) trans.getExecTime().getInTicks(), trans.getPrice().getInTicks());
			fundPrices.add((int) trans.getExecTime().getInTicks(), fund.getValueAt(trans.getExecTime()).getInTicks());

			// update number of transactions
			if (agents.contains(trans.getBuyer())) {
				String name = trans.getBuyer().getName();
				numTrans.put(name, numTrans.get(name) + 1);
			}
			if (!trans.getBuyer().equals(trans.getSeller())
					&& agents.contains(trans.getSeller())) {
				String name = trans.getSeller().getName();
				numTrans.put(name, numTrans.get(name) + 1);
			}
		}
		feat.addProperty(MEAN + "_" + PRICE, prices.getMean());
		feat.addProperty(STDDEV + "_" + PRICE, prices.getStandardDeviation());

		// number of transactions for each agent type
		for (Entry<String, Integer> e : numTrans.entrySet())
			feat.addProperty(e.getKey().toLowerCase() + "_" + NUM, e.getValue());

		// compute RMSD (for price discovery) at different sampling frequencies
		for (int period : Consts.periods) {
			String prefix = period > 1 ? PERIODICITY + period : null;
			// XXX maxTime instead of simLength?
			DSPlus pr = transPrices.getSampledStats(period, (int) simLength);
			DSPlus fundStat = fundPrices.getSampledStats(period, (int) simLength);

			feat.addProperty(prefix + "_" + RMSD, pr.getRMSD(fundStat));
		}
		return feat;
	}

	/**
	 * Computes spread metrics for the given model.
	 */
	protected static JsonObject getSpread(Collection<Market> markets, SIP sip, long maxTime) {
		JsonObject feat = new JsonObject();

		DescriptiveStatistics medians = new DescriptiveStatistics();
		for (Market market : markets) {
			TimeSeries s = market.getSpread();
			DSPlus spreads = s.getSampledStats(1, (int) maxTime);
			double med = spreads.getMedian();
			feat.addProperty(
					delimit("_", MEDIAN, MARKET + Math.abs(market.getID()),
							TIMESERIES_MAXTIME), med);
			medians.addValue(med);
		}

		// average of median market spreads (for all markets in this model)
		feat.addProperty(delimit("_", MEAN, MARKET, TIMESERIES_MAXTIME),
				medians.getMean());

		TimeSeries nbbo = sip.getNBBOSpreads();
		DSPlus spreads = nbbo.getSampledStats(1, (int) maxTime);
		feat.addProperty(delimit("_", MEDIAN, NBBO, TIMESERIES_MAXTIME),
				spreads.getMedian());

		return feat;
	}

	/**
	 * Track liquidation-related features & profit for market makers.
	 */
	protected static JsonObject getMarketMakerInfo(Collection<Agent> agents) {
		JsonObject feat = new JsonObject();

		for (Agent ag : agents) {
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
					mm.getSurplus(0));
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
	protected static JsonObject getVolatility(Collection<Market> markets, int period,
			long maxTime) {
		JsonObject feat = new JsonObject();

		String prefix = period == 0 ? null : PERIODICITY + period;

		DescriptiveStatistics stddev = new DescriptiveStatistics();
		DescriptiveStatistics logPriceVol = new DescriptiveStatistics();
		DescriptiveStatistics logRetVol = new DescriptiveStatistics();

		for (Market market : markets) {
			String suffix = MARKET + Math.abs(market.getID());

			TimeSeries mq = market.getMidQuotes();
			// compute log price volatility for this market
			DescriptiveStatistics mktPrices = mq.getSampledStatsSansNaNs(period, (int) maxTime);
			double stdev = mktPrices.getStandardDeviation();
			feat.addProperty(delimit("_", prefix, STDDEV, PRICE, suffix), stdev);
			stddev.addValue(stdev);
			if (stdev != 0)
			// don't add if stddev is 0
				logPriceVol.addValue(Math.log(stdev));

			// compute log-return volatility for this market
			// XXX This change from before. Before if the ratio was NaN it go thrown out. Now the
			// previous value is used.
			DescriptiveStatistics mktLogReturns = mq.getSampledLogRatioStatsSansNaNs(period, (int) maxTime);
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
