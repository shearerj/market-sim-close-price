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

import sumstats.SumStats;
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
public class Observations {
	
	// static event bus to record statics messages during simulation
	public static final EventBus BUS = new EventBus();
	
	// Statistics objects filled during execution
	protected final SumStats executionTimes;
	protected final SumStats prices;
	protected final TimeSeries transPrices;
	protected final TimeSeries nbboSpreads;
	protected final Multiset<Class<? extends Agent>> numTrans;
	protected final Map<Market, TimeSeries> spreads;
	protected final Map<Market, TimeSeries> midQuotes;
	protected final SumStats controlFundamentalValue;
	protected final SumStats controlPrivateValue;
	
	protected final SumStats marketmakerSpreads;
	protected final SumStats marketmakerLadderCenter;
	protected final SumStats marketmakerExecutionTimes;
	
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
		
		this.executionTimes = SumStats.create();
		this.numTrans = HashMultiset.create();
		this.prices = SumStats.create();
		this.transPrices = TimeSeries.create();
		this.nbboSpreads = TimeSeries.create();
		this.controlPrivateValue = SumStats.create();
		this.controlFundamentalValue = SumStats.create();
		
		this.marketmakerExecutionTimes = SumStats.create();
		this.marketmakerLadderCenter = SumStats.create();
		this.marketmakerSpreads = SumStats.create();
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
		
		features.put("exectime_mean", executionTimes.mean());
		features.put("trans_mean_price", prices.mean());
		features.put("trans_stddev_price", prices.stddev());
		
		double numAllTrans = 0;
		for (Class<? extends Agent> type : agentTypes) {
			double agentTrans = (double) numTrans.count(type);
			features.put("trans_" + type.getSimpleName().toLowerCase() + "_num", 
					agentTrans);
			numAllTrans += agentTrans;
		}
		features.put("trans_num", numAllTrans);
		
		SumStats medians = SumStats.create();
		for (Entry<Market, TimeSeries> entry : spreads.entrySet()) {
			DescriptiveStatistics spreads = DSPlus.from(entry.getValue().sample(1, simLength));
			double median = DSPlus.median(spreads);
			
			features.put("spreads_median_market_" + entry.getKey().getID(), median);
			medians.add(median);
		}
		// average of median market spreads (for all markets in this model)
		features.put("spreads_mean_markets", medians.mean());

		DescriptiveStatistics spreads = DSPlus.from(nbboSpreads.sample(1, simLength));
		features.put("spreads_median_nbbo", DSPlus.median(spreads));
		
		TimeSeries fundPrices = fundamental.asTimeSeries();
		for (int period : Consts.PERIODS)
			periodBased(features, fundPrices, period);
		
		// Market maker
		features.put("mm_spreads_mean", marketmakerSpreads.mean());
		features.put("mm_ladder_mean", marketmakerLadderCenter.mean());
		features.put("mm_spreads_stddev", marketmakerSpreads.stddev());
		features.put("mm_exectime_mean", marketmakerExecutionTimes.mean());
		
		// Profit and Surplus (and Private Value)
		SumStats 
			modelProfit = SumStats.create(),
			backgroundAgentProfit = SumStats.create(),
			backgroundLiquidation = SumStats.create(),
			hftProfit = SumStats.create(),
			marketMakerProfit = SumStats.create();
		
		for (Agent agent : agents) {
			long profit = agent.getPostLiquidationProfit();
			modelProfit.add(profit);
			if (agent instanceof BackgroundAgent) {
				backgroundAgentProfit.add(profit);
				backgroundLiquidation.add(agent.getLiquidationProfit());
			} else if (agent instanceof HFTAgent) {
				hftProfit.add(profit);
			} else if (agent instanceof MarketMaker) {
				marketMakerProfit.add(profit);
			}
		}

		features.put("profit_sum_total", modelProfit.sum());
		features.put("profit_sum_background", backgroundAgentProfit.sum());
		features.put("profit_sum_liquidation", backgroundLiquidation.sum());
		features.put("profit_sum_marketmaker", marketMakerProfit.sum());
		features.put("profit_sum_hft", hftProfit.sum());
		
		Map<Class<? extends Agent>, SumStats> agentSurplus = Maps.newHashMap();
		for (DiscountFactor discount : DiscountFactor.values()) {
			SumStats surplus = SumStats.create();
			// go through all agents & update for each agent type
			for (Agent agent : agents)
				if (agent instanceof BackgroundAgent) {
					controlPrivateValue.add(((BackgroundAgent) agent).getPrivateValueMean().doubleValue());
					
					surplus.add(((BackgroundAgent) agent).getDiscountedSurplus(discount));
					if (!agentSurplus.containsKey(agent.getClass()))
						agentSurplus.put(agent.getClass(), SumStats.create());

					agentSurplus.get(agent.getClass()).add(
							((BackgroundAgent) agent).getDiscountedSurplus(discount));
				}
			
			features.put("surplus_sum_" + discount, surplus.sum());
			for (Class<? extends Agent> type : agentTypes)
				if (BackgroundAgent.class.isAssignableFrom(type))
					features.put("surplus_" + type.getSimpleName().toLowerCase() 
							+ "_sum_" + discount, agentSurplus.get(type).sum());
		}
				
		// for control variates
		List<Double> fundTimeSeries = fundPrices.sample(1, simLength);
		for (double v : fundTimeSeries)
			controlFundamentalValue.add(v);
		features.put("control_mean_fund", controlFundamentalValue.mean());
		features.put("control_var_fund", controlFundamentalValue.variance());
		features.put("control_mean_private", controlPrivateValue.mean());
		
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

		SumStats stddev = SumStats.create();
		SumStats logPriceVol = SumStats.create();
		SumStats logRetVol = SumStats.create();

		for (Entry<Market, TimeSeries> entry : midQuotes.entrySet()) {
			TimeSeries mq = entry.getValue();
			// compute log price volatility for this market
			Iterable<Double> filtered = Iterables.filter(mq.sample(period, simLength), 
					not(equalTo(Double.NaN)));
			double stdev = SumStats.fromData(filtered).stddev();

			features.put(prefix + "_stddev_price_market_" + entry.getKey().getID(), stdev);

			stddev.add(stdev);
			if (stdev != 0)
				// XXX ?ideal? don't add if stddev is 0
				logPriceVol.add(Math.log(stdev));

			// compute log-return volatility for this market
			// XXX Note change in log-return vol from before. Before if the ratio was
			// NaN it go thrown out. Now the previous value is used. Not sure if
			// this is correct
			DescriptiveStatistics mktLogReturns = DSPlus.fromLogRatioOf(mq.sample(period, simLength));
			double logStdev = mktLogReturns.getStandardDeviation();

			features.put(prefix + "_stddev_log_return_market_" + entry.getKey().getID(), logStdev);
			logRetVol.add(logStdev);
		}

		// average measures across all markets in this model
		features.put(prefix + "_mean_stddev_price", stddev.mean());
		features.put(prefix + "_mean_log_price", logPriceVol.mean());
		features.put(prefix + "_mean_log_return", logRetVol.mean());
	}
	
	// --------------------------------------
	// Everything with an @Subscribe is a listener for objects that contain statistics.
	
	@Subscribe public void processMarketMaker(MarketMakerStatistic statistic) {
		marketmakerLadderCenter.add((statistic.ask.doubleValue() + statistic.bid.doubleValue())/2);
		marketmakerSpreads.add(statistic.ask.doubleValue() - statistic.bid.doubleValue());
	}
	
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
		long buyerExecTime = execTime - transaction.getBuyOrder().getSubmitTime().getInTicks();
		long sellerExecTime = execTime - transaction.getSellOrder().getSubmitTime().getInTicks();
		for (int quantity = 0; quantity < transaction.getQuantity(); quantity++) {
			// only measure execution time for background traders
			if (transaction.getBuyer() instanceof BackgroundAgent) {
				executionTimes.add((double) buyerExecTime);
			}
			if (transaction.getSeller() instanceof BackgroundAgent) {
				executionTimes.add((double) sellerExecTime);
			}
			// Also measure for market makers
			if (transaction.getBuyer() instanceof MarketMaker) {
				marketmakerExecutionTimes.add((double) buyerExecTime);
			}
			if (transaction.getSeller() instanceof MarketMaker) {
				marketmakerExecutionTimes.add((double) sellerExecTime);
			}
		}

		prices.add(transaction.getPrice().doubleValue());

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
	
	public static class MarketMakerStatistic {
		protected final MarketMaker mm;
		protected final Price bid;
		protected final Price ask;
		
		public MarketMakerStatistic(MarketMaker mm, Price ladderBid, Price ladderAsk) {
			this.mm = mm;
			this.bid = ladderBid;
			this.ask = ladderAsk;
		}
	}
	
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
