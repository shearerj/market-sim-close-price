package data;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static utils.MathUtils.quantize;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import systemmanager.Consts;
import systemmanager.Keys;
import systemmanager.SimulationSpec;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.HFTAgent;
import entity.agent.MarketMaker;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

public class Observations {
	
	public final static Joiner J = Joiner.on('_');

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
	public final static String NOPERIODICITY = "noperiod";

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
		
		Builder<List<Transaction>> transactionLists = ImmutableList.builder();
		for (Market market : markets)
			transactionLists.add(market.getTransactions());
		List<Transaction> transactions = ImmutableList.copyOf(Iterables.mergeSorted(
				transactionLists.build(), new Comparator<Transaction>() {
					@Override
					public int compare(Transaction o1, Transaction o2) {
						return o1.getExecTime().compareTo(o2.getExecTime());
					}
				}));

		observations = new JsonObject();
		observations.add(PLAYERS, playerObservations(players));

		double arrivalRate = spec.getDefaultAgentProps().getAsDouble(Keys.ARRIVAL_RATE, 0.075);
		long maxTime = Math.round(agents.size() / arrivalRate);
		maxTime = Math.max(Consts.upToTime, quantize((int) maxTime, 1000));

		JsonObject features = new JsonObject();
		observations.add(FEATURES, features);
		
		features.add("", getConfiguration(spec, observationNum, maxTime));
		features.add(SURPLUS, getSurplus(agents, players));
		features.add(EXECTIME, getExecutionTime(transactions));
		features.add(TRANSACTIONS, getTransactionInfo(agents, transactions, fundamental, maxTime));
		features.add(SPREADS, getSpread(markets, sip, maxTime));
		features.add(ROLE_MARKETMAKER, getMarketMakerInfo(agents));

		for (int period : Consts.periods)
			features.add(VOL, getVolatility(markets, period, maxTime));

		// TODO - need to remove the position balance hack
		// addFeature(modelName + ROUTING, getRegNMSRoutingInfo(model));
	}

	protected JsonArray playerObservations(Collection<Player> players) {
		JsonArray obs = new JsonArray();
		for (Player player : players)
			obs.add(player.toJson());
		return obs;
	}

	/********************************************
	 * Data aggregation
	 *******************************************/

	protected static JsonObject getConfiguration(SimulationSpec spec,
			int observationNum, long maxTime) {
		JsonObject config = new JsonObject();

		config.addProperty(OBS_KEY, observationNum);
		config.addProperty(TIMESERIES_MAXTIME, maxTime);
		spec.getSimulationProps().copyToJson(config);
		spec.getDefaultMarketProps().copyToJson(config);
		spec.getDefaultAgentProps().copyToJson(config);

		for (AgentProperties props : spec.getAgentProps()) {
			// TODO Change to not split off number...
			int number = props.getAsInt(Keys.NUM, 0);

			config.addProperty(J.join(props.getAgentType(), NUM), number);
			config.addProperty(J.join(props.getAgentType(), AGENTSETUP), 
					props.toConfigString());
		}
		return config;
	}

	/**
	 * Extract discounted surplus for background agents, including those that
	 * are players (in the EGTA use case).
	 */
	protected static JsonObject getSurplus(Collection<Agent> agents, Collection<Player> players) {
		JsonObject feat = new JsonObject();

		for (double rho : Consts.rhos) {
			String suffix = rho == 0 ? UNDISCOUNTED : DISCOUNTED + rho;

			DescriptiveStatistics modelSurplus, background, hft, marketMaker, environment;
			modelSurplus = DSPlus.from();
			// sub-categories for surplus (roles)
			background = DSPlus.from();
			hft = DSPlus.from();
			marketMaker = DSPlus.from();
			environment = DSPlus.from();

			ImmutableSet.Builder<Agent> builder = ImmutableSet.builder();
			for (Player p : players)
				builder.add(p.getAgent());
			Set<Agent> playerSet = builder.build();

			// go through all agents & update for each agent type
			for (Agent agent : agents) {
				double agentSurplus = agent.getSurplus(rho);
				modelSurplus.addValue(agentSurplus);

				if (agent instanceof BackgroundAgent) {
					background.addValue(agentSurplus);
				} else if (agent instanceof HFTAgent) {
					// FIXME This is not necessarily correct if the agent has a net position at the end...
					hft.addValue(agentSurplus);
				} else if (agent instanceof MarketMaker) {
					marketMaker.addValue(agentSurplus);
				}
				if (!playerSet.contains(agent)) {
					environment.addValue(agentSurplus);
				}
			}

			feat.addProperty(J.join(SUM, TOTAL, suffix),
					modelSurplus.getSum());
			feat.addProperty(J.join(SUM, ROLE_BACKGROUND, suffix),
					background.getSum());
			feat.addProperty(J.join(SUM, ROLE_MARKETMAKER, suffix),
					marketMaker.getSum());
			feat.addProperty(J.join(SUM, ROLE_HFT, suffix), hft.getSum());
			feat.addProperty(J.join(SUM, TYPE_ENVIRONMENT, suffix),
					environment.getSum());
		}
		return feat;
	}

	/**
	 * Execution time metrics.
	 */
	protected static JsonObject getExecutionTime(Collection<Transaction> transactions) {
		JsonObject feat = new JsonObject();
		DescriptiveStatistics speeds = DSPlus.from();

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

		DescriptiveStatistics prices = new DescriptiveStatistics();
		DescriptiveStatistics quantity = new DescriptiveStatistics();
		DescriptiveStatistics fundamental = new DescriptiveStatistics();

		TimeSeries transPrices = new TimeSeries();
		TimeSeries fundPrices = new TimeSeries();

		// number of transactions, hashed by agent type
		Multiset<String> numTrans = HashMultiset.create();
		for (Transaction trans : transactions) {
			prices.addValue(trans.getPrice().intValue());
			quantity.addValue(trans.getQuantity());
			fundamental.addValue(fund.getValueAt(trans.getExecTime()).intValue());

			transPrices.add((int) trans.getExecTime().getInTicks(), trans.getPrice().intValue());
			fundPrices.add((int) trans.getExecTime().getInTicks(), fund.getValueAt(trans.getExecTime()).intValue());

			// update number of transactions
			numTrans.add(trans.getBuyer().getName());
			if (!trans.getBuyer().equals(trans.getSeller()))
				// If agent transacts with itself only count as one transaction 
				numTrans.add(trans.getSeller().getName());
		}
		feat.addProperty(J.join(MEAN, PRICE), prices.getMean());
		feat.addProperty(J.join(STDDEV, PRICE), prices.getStandardDeviation());

		// So that agent types with 0 transactions still get logged.
		ImmutableSet.Builder<String> names = ImmutableSet.builder();
		for (Agent agent : agents)
			names.add(agent.getName());
		for (String name : names.build())
			feat.addProperty(J.join(name.toLowerCase(), NUM), numTrans.count(name));

		// compute RMSD (for price discovery) at different sampling frequencies
		for (int period : Consts.periods) {
			String key = period > 1 ? J.join(PERIODICITY + period, RMSD) : RMSD; 
			// XXX maxTime instead of simLength?
			DescriptiveStatistics pr = DSPlus.from(transPrices.sample(period, (int) simLength));
			DescriptiveStatistics fundStat = DSPlus.from(fundPrices.sample(period, (int) simLength));

			feat.addProperty(key, DSPlus.rmsd(pr, fundStat));
		}
		return feat;
	}

	/**
	 * Computes spread metrics for the given model.
	 */
	protected static JsonObject getSpread(Collection<Market> markets, SIP sip, long maxTime) {
		JsonObject feat = new JsonObject();

		DescriptiveStatistics medians = DSPlus.from();
		for (Market market : markets) {
			TimeSeries s = market.getSpread();
			DescriptiveStatistics spreads = DSPlus.from(s.sample(1, (int) maxTime));
			double med = DSPlus.median(spreads);
			feat.addProperty(J.join(MEDIAN, MARKET + Math.abs(market.getID()),
					TIMESERIES_MAXTIME), med);
			medians.addValue(med);
		}

		// average of median market spreads (for all markets in this model)
		feat.addProperty(J.join(MEAN, MARKET, TIMESERIES_MAXTIME),
				medians.getMean());

		TimeSeries nbbo = sip.getNBBOSpreads();
		DescriptiveStatistics spreads = DSPlus.from(nbbo.sample(1, (int) maxTime));
		feat.addProperty(J.join(MEDIAN, NBBO, TIMESERIES_MAXTIME),
				DSPlus.median(spreads));

		return feat;
	}

	/**
	 * Track liquidation-related features & profit for market makers.
	 */
	protected static JsonObject getMarketMakerInfo(Collection<Agent> agents) {
		JsonObject feat = new JsonObject();

//		for (Agent ag : agents) {
//			if (!(ag instanceof MarketMaker)) continue;
//			MarketMaker mm = (MarketMaker) ag;
//
//			// append the agentID in case more than one of this type
//			String suffix = Integer.toString(mm.getID());
//			feat.addProperty(J.join(POSITION, PRE, LIQUIDATION + suffix),
//					mm.getPreLiquidationPosition());
//			// just to double-check, should be 0 position after liquidation
//			feat.addProperty(J.join(POSITION, POST, LIQUIDATION + suffix),
//					mm.getPositionBalance());
//			feat.addProperty(J.join(PROFIT, PRE, LIQUIDATION + suffix),
//					mm.getPreLiquidationProfit());
//			feat.addProperty(J.join(PROFIT, POST, LIQUIDATION + suffix),
//					mm.getSurplus(0));
//		}
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

		String prefix = period == 1 ? NOPERIODICITY : PERIODICITY + period;

		DescriptiveStatistics stddev = DSPlus.from();
		DescriptiveStatistics logPriceVol = DSPlus.from();
		DescriptiveStatistics logRetVol = DSPlus.from();

		for (Market market : markets) {
			String suffix = MARKET + Math.abs(market.getID());

			TimeSeries mq = market.getMidQuotes();
			// compute log price volatility for this market
			Iterable<Double> filtered = Iterables.filter(mq.sample(period, (int) maxTime), not(equalTo(Double.NaN)));
			DescriptiveStatistics mktPrices = DSPlus.from(filtered);
			double stdev = mktPrices.getStandardDeviation();
			feat.addProperty(J.join(prefix, STDDEV, PRICE, suffix), stdev);
			stddev.addValue(stdev);
			if (stdev != 0)
				// don't add if stddev is 0
				logPriceVol.addValue(Math.log(stdev));

			// compute log-return volatility for this market
			// XXX This change from before. Before if the ratio was NaN it go thrown out. Now the
			// previous value is used.
			DescriptiveStatistics mktLogReturns = DSPlus.fromLogRatioOf(mq.sample(period, (int) maxTime));
			double logStdev = mktLogReturns.getStandardDeviation();
			feat.addProperty(J.join(prefix, STDDEV, LOG + RETURN, suffix),
					logStdev);
			logRetVol.addValue(logStdev);
		}

		// average measures across all markets in this model
		feat.addProperty(J.join(prefix, MEAN, STDDEV + PRICE), stddev.getMean());
		feat.addProperty(J.join(prefix, MEAN, LOG + PRICE), logPriceVol.getMean());
		feat.addProperty(J.join(prefix, MEAN, LOG + RETURN), logRetVol.getMean());

		return feat;
	}

	public void writeToFile(File observationsFile) throws IOException {
		Writer writer = null;
		try {
			writer = new FileWriter(observationsFile);
			gson.toJson(observations, writer);
		} finally {
			if (writer != null) writer.close();
		}
	}

}
