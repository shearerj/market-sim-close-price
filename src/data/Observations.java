package data;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;

import static logger.Log.log;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import logger.Log;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import systemmanager.Consts;
import systemmanager.Consts.DiscountFactor;
import systemmanager.Keys;
import systemmanager.SimulationSpec;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.HFTAgent;
import entity.agent.MarketMaker;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

/**
 * This class represents the summary of statistics after a run of the
 * simulation. The majority of the statistics that it collects are processed via
 * the EventBus, which is a message passing interface that can handle any style
 * of object. The rest, which mainly includes agent and player payoffs, is
 * processed when the call to getFeatures or getPlayerObservations is made.
 * 
 * Because this uses message passing, if you want the observation data structure
 * to get data, you must make sure to "register" it with the EventBus by calling
 * BUS.register(observations).
 * 
 * A single observation doesn't have that ability to save its results. Instead
 * you need to add it to a MultiSimulationObservation and use that class to
 * actually do the output.
 * 
 * To add statistics to the Observation:
 * <ol>
 * <li>Add the appropriate data structures to the object to record the
 * information you're interested in.</li>
 * <li>Create a listener method (located at the bottom, to tell what objects to
 * handle, and what to do with them.</li>
 * <li>Modify get features to take the aggregate data you stored, and turn it
 * into a String, double pair.</li>
 * <li>Wherever the relevant data is added simply add a line like
 * "Observations.BUS.post(dataObj);" with your appropriate data</li>
 * </ol>
 * 
 * @author erik
 * 
 */
/*
 * TODO At some point, we may want to be able to read an observation file into
 * one of these
 */
public class Observations {
	
	// static event bus to record statics messages during simulation
	public static final EventBus BUS = new EventBus();
	
	// Statistics objects filled during execution
	protected final SummaryStatistics executionSpeeds;
	protected final SummaryStatistics prices;
	protected final TimeSeries transPrices;
	protected final TimeSeries nbboSpreads;
	protected final Multiset<Class<? extends Agent>> numTrans;
	protected final Map<Market, TimeSeries> spreads;
	protected final Map<Market, TimeSeries> midQuotes;
	protected final SummaryStatistics controlFundamentalValue;
	protected final SummaryStatistics controlPrivateValue;
	
	// Static information needed for observations
	protected final Collection<? extends Player> players;
	protected final Collection<? extends Agent> agents;
	protected final Collection<? extends Market> markets;
	protected final FundamentalValue fundamental;
	protected final SimulationSpec spec;
	protected final Set<Class<? extends Agent>> agentTypes;
	protected final int simLength;

	/**
	 * Constructor needs to be called before the simulation starts, but with the
	 * final object collections.
	 */
	public Observations(SimulationSpec spec, Collection<? extends Market> markets,
			Collection<? extends Agent> agents, Collection<? extends Player> players,
			FundamentalValue fundamental) {
		this.players = players;
		this.agents = agents;
		this.markets = markets;
		this.fundamental = fundamental;
		this.spec = spec;
		
		this.simLength = spec.getSimulationProps().getAsInt(Keys.SIMULATION_LENGTH, 10000);
		
		// This is so that every agent type is output at the end, even if they
		// completed no transactions
		ImmutableSet.Builder<Class<? extends Agent>> agentTypesBuilder = ImmutableSet.builder();
		for (Agent agent : agents)
			agentTypesBuilder.add(agent.getClass());
		agentTypes = agentTypesBuilder.build();
		
		ImmutableMap.Builder<Market, TimeSeries> spreadsBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<Market, TimeSeries> midQuotesBuilder = ImmutableMap.builder();
		for (Market market : markets) {
			spreadsBuilder.put(market, TimeSeries.create());
			midQuotesBuilder.put(market, TimeSeries.create());
		}
		spreads = spreadsBuilder.build();
		midQuotes = midQuotesBuilder.build();
		
		this.executionSpeeds = new SummaryStatistics();
		this.numTrans = HashMultiset.create();
		this.prices = new SummaryStatistics();
		this.transPrices = TimeSeries.create();
		this.nbboSpreads = TimeSeries.create();
		this.controlPrivateValue = new SummaryStatistics();
		this.controlFundamentalValue = new SummaryStatistics();
	}
	
	/**
	 * Gets the player observations relevant to EGTA.
	 */
	public List<PlayerObservation> getPlayerObservations() {
		Builder<PlayerObservation> playerObservations = ImmutableList.builder();
		for (Player player : players)
			playerObservations.add(player.getObservation());
		return playerObservations.build();
	}
	
	/**
	 * Gets the features, which are relevant to the non EGTA case. Features that
	 * aren't aggregated by the EventBus, like the surplus ones, are calculated
	 * at the current time, instead of being summed during the simulation.
	 */
	public Map<String, Double> getFeatures() {
		ImmutableMap.Builder<String, Double> features = ImmutableMap.builder();
		
		features.put("exectime_mean", executionSpeeds.getMean());
		features.put("trans_mean_price", prices.getMean());
		features.put("trans_stddev_price", prices.getStandardDeviation());
		
		double numAllTrans = 0;
		for (Class<? extends Agent> type : agentTypes) {
			double agentTrans = (double) numTrans.count(type);
			features.put("trans_" + type.getSimpleName().toLowerCase() + "_num", 
					agentTrans);
			numAllTrans += agentTrans;
		}
		features.put("trans_num", numAllTrans);
		
		SummaryStatistics medians = new SummaryStatistics();
		for (Entry<Market, TimeSeries> entry : spreads.entrySet()) {
			DescriptiveStatistics spreads = DSPlus.from(entry.getValue().sample(1, simLength));
			double median = DSPlus.median(spreads);
			
			features.put("spreads_median_market_" + entry.getKey().getID(), median);
			medians.addValue(median);
		}
		// average of median market spreads (for all markets in this model)
		features.put("spreads_mean_markets", medians.getMean());

		DescriptiveStatistics spreads = DSPlus.from(nbboSpreads.sample(1, simLength));
		features.put("spreads_median_nbbo", DSPlus.median(spreads));
		
		TimeSeries fundPrices = fundamental.asTimeSeries();
		for (int period : Consts.PERIODS)
			periodBased(features, fundPrices, period);
		
		// Profit and Surplus (and Private Value)
		// XXX This does't quite make sense because background agents don't liquidate...
		SummaryStatistics 
			modelProfit = new SummaryStatistics(),
			backgroundAgentProfit = new SummaryStatistics(),
			hftProfit = new SummaryStatistics(),
			marketMakerProfit = new SummaryStatistics();
		
		for (Agent agent : agents) {
			long profit = agent.getPostLiquidationProfit();
			modelProfit.addValue(profit);
			if (agent instanceof BackgroundAgent) {
				backgroundAgentProfit.addValue(profit);
			} else if (agent instanceof HFTAgent) {
				hftProfit.addValue(profit);
			} else if (agent instanceof MarketMaker) {
				marketMakerProfit.addValue(profit);
			}
		}

		features.put("profit_sum_total", modelProfit.getSum());
		features.put("profit_sum_background", backgroundAgentProfit.getSum());
		features.put("profit_sum_marketmaker", marketMakerProfit.getSum());
		features.put("profit_sum_hft", hftProfit.getSum());
		
		Map<Class<? extends Agent>, SummaryStatistics> agentSurplus = Maps.newHashMap();
		for (DiscountFactor discount : DiscountFactor.values()) {
			SummaryStatistics surplus = new SummaryStatistics();
			// go through all agents & update for each agent type
			for (Agent agent : agents)
				if (agent instanceof BackgroundAgent) {
					controlPrivateValue.addValue(((BackgroundAgent) agent).getPrivateValueMean().doubleValue());
					
					surplus.addValue(((BackgroundAgent) agent).getDiscountedSurplus(discount));
					if (!agentSurplus.containsKey(agent.getClass()))
						agentSurplus.put(agent.getClass(), new SummaryStatistics());

					agentSurplus.get(agent.getClass()).addValue(
							((BackgroundAgent) agent).getDiscountedSurplus(discount));
				}
			
			features.put("surplus_sum_" + discount, surplus.getSum());
			for (Class<? extends Agent> type : agentTypes)
				if (BackgroundAgent.class.isAssignableFrom(type))
					features.put("surplus_" + type.getSimpleName().toLowerCase() 
							+ "_sum_" + discount, agentSurplus.get(type).getSum());
		}
				
		// for control variates
		List<Double> fundTimeSeries = fundPrices.sample(1, simLength);
		for (double v : fundTimeSeries)
			controlFundamentalValue.addValue(v);
		features.put("control_mean_fund", controlFundamentalValue.getMean());
		features.put("control_var_fund", controlFundamentalValue.getVariance());
		features.put("control_mean_private", controlPrivateValue.getMean());
		
		return features.build();
	}
	
	/**
	 * Statistics that are based on the sampling period
	 */
	protected void periodBased(ImmutableMap.Builder<String, Double> features, 
			TimeSeries fundPrices, int period) {
		// Price discovery
		String key = period == 1 ? "trans_rmsd" : "trans_freq_" + period + "_rmsd"; 
		DescriptiveStatistics pr = DSPlus.from(transPrices.sample(period, simLength));
		DescriptiveStatistics fundStat = DSPlus.from(fundPrices.sample(period, simLength));
		features.put(key, DSPlus.rmsd(pr, fundStat));

		// Volatility
		String prefix = period == 1 ? "vol" : "vol_freq_" + period;

		SummaryStatistics stddev = new SummaryStatistics();
		SummaryStatistics logPriceVol = new SummaryStatistics();
		SummaryStatistics logRetVol = new SummaryStatistics();

		for (Entry<Market, TimeSeries> entry : midQuotes.entrySet()) {
			TimeSeries mq = entry.getValue();
			// compute log price volatility for this market
			Iterable<Double> filtered = Iterables.filter(mq.sample(period, simLength), 
					not(equalTo(Double.NaN)));
			double stdev = DSPlus.from(filtered).getStandardDeviation();

			features.put(prefix + "_stddev_price_market_" + entry.getKey().getID(), stdev);

			stddev.addValue(stdev);
			if (stdev != 0)
				// XXX ?ideal? don't add if stddev is 0
				logPriceVol.addValue(Math.log(stdev));

			// compute log-return volatility for this market
			// XXX Note change in log-return vol from before. Before if the ratio was
			// NaN it go thrown out. Now the previous value is used. Not sure if
			// this is correct
			DescriptiveStatistics mktLogReturns = DSPlus.fromLogRatioOf(mq.sample(period, simLength));
			double logStdev = mktLogReturns.getStandardDeviation();

			features.put(prefix + "_stddev_log_return_market_" + entry.getKey().getID(), logStdev);
			logRetVol.addValue(logStdev);
		}

		// average measures across all markets in this model
		features.put(prefix + "_mean_stddev_price", stddev.getMean());
		features.put(prefix + "_mean_log_price", logPriceVol.getMean());
		features.put(prefix + "_mean_log_return", logRetVol.getMean());
	}
	
	// --------------------------------------
	// Everything with an @Subscribe is a listener for objects that contain statistics.
	
	@Subscribe public void processSpread(SpreadStatistic statistic) {
		TimeSeries series = spreads.get(statistic.owner);
		series.add(statistic.time.getInTicks(), statistic.val);
	}
	
	@Subscribe public void processMidQuote(MidQuoteStatistic statistic) {
		TimeSeries series = midQuotes.get(statistic.owner);
		series.add(statistic.time.getInTicks(), statistic.val);
	}
	
	@Subscribe public void processNBBOSpread(NBBOStatistic statistic) {
		nbboSpreads.add(statistic.time.getInTicks(), statistic.val);
	}
	
	@Subscribe public void processTransaction(Transaction transaction) {
		long execTime = transaction.getExecTime().getInTicks();
		long buyerExecTime = execTime - transaction.getBuyBid().getSubmitTime().getInTicks();
		long sellerExecTime = execTime - transaction.getSellBid().getSubmitTime().getInTicks();
		for (int quantity = 0; quantity < transaction.getQuantity(); quantity++) {
			executionSpeeds.addValue((double) buyerExecTime);
			executionSpeeds.addValue((double) sellerExecTime);
		}

		prices.addValue(transaction.getPrice().doubleValue());

		transPrices.add((int) transaction.getExecTime().getInTicks(), 
				transaction.getPrice().doubleValue());
		
		// update number of transactions
		numTrans.add(transaction.getBuyer().getClass());
		numTrans.add(transaction.getSeller().getClass());
	}
	
	@Subscribe public void deadStat(DeadEvent d) {
		log.log(Log.Level.ERROR, "Unhandled Statistic: %s", d);
	}
	
	// --------------------------------------
	// These are all statistics classes that are listened for
	
	public static class NBBOStatistic {
		protected final double val;
		protected final TimeStamp time;

		public NBBOStatistic(double val, TimeStamp time) {
			this.val = val;
			this.time = time;
		}
	}
	
	public static abstract class MarketStatistic {
		protected final double val;
		protected final Market owner;
		protected final TimeStamp time;

		public MarketStatistic(Market owner, double val, TimeStamp time) {
			this.owner = owner;
			this.val = val;
			this.time = time;
		}
	}
	
	public static class SpreadStatistic extends MarketStatistic {
		public SpreadStatistic(Market market, double val, TimeStamp time) {
			super(market, val, time);
		}
	}
	
	public static class MidQuoteStatistic extends MarketStatistic {
		public MidQuoteStatistic(Market market, double val, TimeStamp time) {
			super(market, val, time);
		}
	}

	public static abstract class PriceStatistic {
		protected final double val;
		
		public PriceStatistic(Price value) {
			this.val = value.doubleValue();
		}
	}
}
