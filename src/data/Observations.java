package data;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static utils.MathUtils.quantize;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import logger.Logger;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import systemmanager.Consts;
import systemmanager.Keys;
import systemmanager.SimulationSpec;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.HFTAgent;
import entity.agent.MarketMaker;
import entity.market.Market;
import entity.market.Transaction;
import event.TimeStamp;

public class Observations {
	
	// static event bus to record statics messages during simulation
	public static final EventBus BUS = new EventBus();
	
	// Statistics objects filled during execution
	protected final SummaryStatistics executionSpeeds;
	protected final SummaryStatistics prices;
	protected final TimeSeries transPrices;
	protected final TimeSeries fundPrices;
	protected final TimeSeries nbboSpreads;
	protected final Multiset<Class<? extends Agent>> numTrans;
	protected final Map<Market, TimeSeries> spreads;
	protected final Map<Market, TimeSeries> midQuotes;
	
	// Static information needed for observations
	protected final Collection<Player> players;
	protected final Collection<Agent> agents;
	protected final Collection<Market> markets;
	protected final FundamentalValue fundamental;
	protected final SimulationSpec spec;
	protected final Set<Class<? extends Agent>> agentTypes;
	protected final int maxTime;

	public Observations(SimulationSpec spec, Collection<Market> markets,
			Collection<Agent> agents, Collection<Player> players,
			FundamentalValue fundamental) {
		this.players = players;
		this.agents = agents;
		this.markets = markets;
		this.fundamental = fundamental;
		this.spec = spec;
		
		// TODO Change this / remove?
		double arrivalRate = spec.getDefaultAgentProps().getAsDouble(Keys.ARRIVAL_RATE, 0.075);
		int maxTime = (int) Math.round(agents.size() / arrivalRate);
		this.maxTime = Math.max(Consts.UP_TO_TIME, quantize(maxTime, 1000));
		
		ImmutableSet.Builder<Class<? extends Agent>> agentTypesBuilder = ImmutableSet.builder();
		for (Agent agent : agents)
			agentTypesBuilder.add(agent.getClass());
		agentTypes = agentTypesBuilder.build();
		
		ImmutableMap.Builder<Market, TimeSeries> spreadsBuilder = ImmutableMap.builder();
		ImmutableMap.Builder<Market, TimeSeries> midQuotesBuilder = ImmutableMap.builder();
		for (Market market : markets) {
			spreadsBuilder.put(market, new TimeSeries());
			midQuotesBuilder.put(market, new TimeSeries());
		}
		spreads = spreadsBuilder.build();
		midQuotes = midQuotesBuilder.build();
		
		this.executionSpeeds = new SummaryStatistics();
		this.numTrans = HashMultiset.create();
		this.prices = new SummaryStatistics();
		this.transPrices = new TimeSeries();
		this.fundPrices = new TimeSeries();
		this.nbboSpreads = new TimeSeries();
		
		BUS.register(this);
//		BUS.unregister(this); FIXME This should happen somewhere!!!!
	}
	
	public List<PlayerObservation> getPlayerObservations() {
		Builder<PlayerObservation> playerObservations = ImmutableList.builder();
		for (Player player : players)
			playerObservations.add(player.getObservation());
		return playerObservations.build();
	}
	
	public Map<String, Double> getFeatures() {
		ImmutableMap.Builder<String, Double> features = ImmutableMap.builder();
		
		features.put("exectime_mean", executionSpeeds.getMean());
		features.put("trans_mean_price", prices.getMean());
		features.put("trans_stddev_price", prices.getStandardDeviation());
		
		for (Class<? extends Agent> type : agentTypes)
			features.put("trans_" + type.getSimpleName().toLowerCase() + "_num", (double) numTrans.count(type));
		
		SummaryStatistics medians = new SummaryStatistics();
		for (Entry<Market, TimeSeries> entry : spreads.entrySet()) {
			DescriptiveStatistics spreads = DSPlus.from(entry.getValue().sample(1, maxTime));
			double median = DSPlus.median(spreads);
			
			features.put("spreads_median_market_" + entry.getKey().getID() + "_to_maxtime", median);
			medians.addValue(median);
		}
		// average of median market spreads (for all markets in this model)
		features.put("spreads_mean_markets_to_maxtime", medians.getMean());

		DescriptiveStatistics spreads = DSPlus.from(nbboSpreads.sample(1, (int) maxTime));
		features.put("spreads_median_nbbo_to_maxtime", DSPlus.median(spreads));
		
		for (int period : Consts.PERIODS)
			periodBased(features, period);
		
		for (double discount : Consts.DISCOUNT_FACTORS)
			discountBased(features, discount);
		
		return features.build();
	}
	
	protected void periodBased(ImmutableMap.Builder<String, Double> features, int period) {
		// Price discovery
		String key = period == 1 ? "trans_rmsd" : "trans_freq_" + period + "_rmsd"; 
		DescriptiveStatistics pr = DSPlus.from(transPrices.sample(period, (int) maxTime));
		DescriptiveStatistics fundStat = DSPlus.from(fundPrices.sample(period, (int) maxTime));
		features.put(key, DSPlus.rmsd(pr, fundStat));

		// Volatility
		String prefix = period == 1 ? "vol" : "vol_freq_" + period;

		SummaryStatistics stddev = new SummaryStatistics();
		SummaryStatistics logPriceVol = new SummaryStatistics();
		SummaryStatistics logRetVol = new SummaryStatistics();

		for (Entry<Market, TimeSeries> entry : midQuotes.entrySet()) {
			TimeSeries mq = entry.getValue();
			// compute log price volatility for this market
			Iterable<Double> filtered = Iterables.filter(mq.sample(period, (int) maxTime), not(equalTo(Double.NaN)));
			double stdev = DSPlus.from(filtered).getStandardDeviation();

			features.put(prefix + "_stddev_price_market_" + entry.getKey().getID(), stdev);

			stddev.addValue(stdev);
			if (stdev != 0)
				// don't add if stddev is 0
				logPriceVol.addValue(Math.log(stdev));

			// compute log-return volatility for this market
			// FIXME: Elaine - This changed from before. Before if the ratio was
			// NaN it go thrown out. Now the previous value is used. Not sure if
			// this is correct
			DescriptiveStatistics mktLogReturns = DSPlus.fromLogRatioOf(mq.sample(period, (int) maxTime));
			double logStdev = mktLogReturns.getStandardDeviation();

			features.put(prefix + "_stddev_log_return_market_" + entry.getKey().getID(), logStdev);
			logRetVol.addValue(logStdev);
		}

		// average measures across all markets in this model
		features.put(prefix + "_mean_stddev_price", stddev.getMean());
		features.put(prefix + "_mean_log_price", logPriceVol.getMean());
		features.put(prefix + "_mean_log_return", logRetVol.getMean());
	}
	
	protected void discountBased(ImmutableMap.Builder<String, Double> features, double discount) {
		String suffix = discount == 0 ? "no_disc" : "disc_" + discount;

		DescriptiveStatistics modelSurplus, background, hft, marketMaker;
		modelSurplus = DSPlus.from();
		// sub-categories for surplus (roles)
		background = DSPlus.from();
		hft = DSPlus.from();
		marketMaker = DSPlus.from();

		// go through all agents & update for each agent type
		for (Agent agent : agents) {
			double agentSurplus = agent.getSurplus(discount);
			modelSurplus.addValue(agentSurplus);

			if (agent instanceof BackgroundAgent) {
				background.addValue(agentSurplus);
			} else if (agent instanceof HFTAgent) {
				// FIXME This is not necessarily correct if the agent has a net position at the end...
				hft.addValue(agentSurplus);
			} else if (agent instanceof MarketMaker) {
				marketMaker.addValue(agentSurplus);
			}
		}

		features.put("surplus_sum_total_" + suffix, modelSurplus.getSum());
		features.put("surplus_sum_background_" + suffix, background.getSum());
		features.put("surplus_sum_marketmaker_" + suffix, marketMaker.getSum());
		features.put("surplus_sum_hft_" + suffix, hft.getSum());
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
		long buyerExecTime = execTime - transaction.getBuyBid().getSubmitTime().getInTicks();
		long sellerExecTime = execTime - transaction.getSellBid().getSubmitTime().getInTicks();
		for (int quantity = 0; quantity < transaction.getQuantity(); quantity++) {
			executionSpeeds.addValue((double) buyerExecTime);
			executionSpeeds.addValue((double) sellerExecTime);
		}

		prices.addValue(transaction.getPrice().intValue());

		transPrices.add((int) transaction.getExecTime().getInTicks(), transaction.getPrice().intValue());
		fundPrices.add((int) transaction.getExecTime().getInTicks(), fundamental.getValueAt(transaction.getExecTime()).intValue());

		// update number of transactions
		numTrans.add(transaction.getBuyer().getClass());
		numTrans.add(transaction.getSeller().getClass());
	}
	
	@Subscribe public void deadStat(DeadEvent d) {
		Logger.log(Logger.Level.ERROR, "Unhandled Statistic: " + d);
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

}
