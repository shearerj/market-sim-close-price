package data;

import static com.google.common.base.Preconditions.checkState;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import systemmanager.Keys.MeanPrefixes;
import systemmanager.Keys.Periods;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.StddevPrefixes;
import systemmanager.Keys.SumPrefixes;
import systemmanager.SimulationSpec.PlayerSpec;
import utils.Maps2;
import utils.Sparse;
import utils.SummStats;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * This class represents all of the output data about the entire set of
 * simulations. It logs player information, and derived information from
 * simulation statistics.
 * 
 * The two main types of statistics are handled differently.
 * 
 * Timed Statistics must have the "add" method modified to properly store any
 * relevant statistics.
 * 
 * Summary statistics automatically have their mean, sum and standard deviation
 * stored. There are three white lists called sumPrefixes, meanPrefixes, and
 * stddevPrefixes that will cause the statistic to only have the corresponding
 * summary stored. IF a statistic occurs on several white lists, then it will
 * have all of the relevant statistics stored. By default, the stored summaries
 * are the ones usually required.
 * 
 * @author erik
 */
public class Observations {
	
	/*
	 * FIXME There's no guarantee that all of the summstats in features have the
	 * same n. The missing n's should probably be made up with nan's so the
	 * error is made known instead of hiding it by padding with zeros or
	 * something.
	 */

	public static final String // Observation Keys
	PV_BUY1 = 					"pv_buy1",
	PV_SELL1 = 					"pv_sell1",
	PV_POSITION1_MAX_ABS =		"pv_position_max_abs1";
	
	protected final Multimap<String, PlayerObservation> players; // Player information
	protected final Map<String, SummStats> features; // Other features
	
	private transient final long simLength;
	private transient final Iterable<Integer> periods;
	
	/*
	 * Each of these iterables lists prefixes for statistics to saved.
	 * sumPrefixes contains prefixes of every statistic that should have its
	 * summation saved. meanPrefixes for mean, etc.
	 */
	private transient final Iterable<String> sumPrefixes;
	private transient final Iterable<String> meanPrefixes;
	private transient final Iterable<String> stddevPrefixes;

	protected Observations(Multiset<PlayerSpec> playerProperties, Props simulationProps) {
		features = Maps2.addDefault(Maps.<String, SummStats> newHashMap(), new Supplier<SummStats>() {
			@Override public SummStats get() { return SummStats.on(); }
		});

		ImmutableListMultimap.Builder<String, PlayerObservation> builder = ImmutableListMultimap.builder();
		for (Multiset.Entry<PlayerSpec> spec : playerProperties.entrySet())
			for (int i = 0; i < spec.getCount(); ++i)
				builder.put(spec.getElement().descriptor, PlayerObservation.create());
		players = builder.build();

		this.simLength = simulationProps.get(SimLength.class);
		this.periods = simulationProps.get(Periods.class);

		this.sumPrefixes = simulationProps.get(SumPrefixes.class);
		this.meanPrefixes = simulationProps.get(MeanPrefixes.class);
		this.stddevPrefixes = simulationProps.get(StddevPrefixes.class);
	}
	
	public static Observations create(Multiset<PlayerSpec> playerProperties, Props simulationProps) {
		return new Observations(playerProperties, simulationProps);
	}
	
	// Modify this to add custom time series statistics
	/** Adds the statistics and player information gathered from the simulation to the cumulative observation */
	public void add(Stats stats, Collection<Player> playerAgents) {
		// Handle Players
		ImmutableMap.Builder<String, Iterator<PlayerObservation>> builder = ImmutableMap.builder();
		for (Entry<String, Collection<PlayerObservation>> e : players.asMap().entrySet())
			builder.put(e.getKey(), e.getValue().iterator());
		Map<String, Iterator<PlayerObservation>> playerObs = builder.build();
		for (Player player : playerAgents)
			playerObs.get(player.getDescriptor()).next().observe(player);
		// Check that all players were added
		for (Iterator<PlayerObservation> iter : playerObs.values())
			checkState(!iter.hasNext());
		
		// General Statistics		// Sum
		for (String prefix : sumPrefixes)
			for (Entry<String, SummStats> e : Maps2.prefix(prefix, stats.getSummaryStats()).entrySet())
				features.get(e.getKey() + "_sum").add(e.getValue().sum());
		// Mean
		for (String prefix : meanPrefixes)
			for (Entry<String, SummStats> e : Maps2.prefix(prefix, stats.getSummaryStats()).entrySet())
				features.get(e.getKey() + "_mean").add(e.getValue().mean());
		// Standard Deviation
		for (String prefix : stddevPrefixes)
			for (Entry<String, SummStats> e : Maps2.prefix(prefix, stats.getSummaryStats()).entrySet())
				features.get(e.getKey() + "_stddev").add(e.getValue().stddev());

		// Spreads
		SummStats spreadMedians = SummStats.on();
		for (Entry<String, TimeSeries> e : Maps2.prefix(Stats.SPREAD, stats.getTimeStats()).entrySet()) {
			double median = Sparse.median(e.getValue(), simLength);
			if (!e.getKey().equals(Stats.NBBO_SPREAD))
				spreadMedians.add(median);
			features.get(e.getKey() + "_median").add(median);
		}
		features.get("spreads_mean").add(spreadMedians.mean());
		
		// RMSD
		TimeSeries fundamental = stats.getTimeStats().get(Stats.FUNDAMENTAL);
		for (int period : periods) {
			TimeSeries transPrices = stats.getTimeStats().get(Stats.TRANSACTION_PRICE);
			double rmsd = Sparse.rmsd(transPrices, fundamental, period, simLength);
			features.get("trans_freq_" + period + "_rmsd").add(rmsd);
		}
		
		// Volatility
		for (int period : periods) {
			String prefix = "vol_freq_" + period + '_';

			SummStats stddev = SummStats.on();
			SummStats logPriceVol = SummStats.on();
			SummStats logRetVol = SummStats.on();

			for (Entry<String, TimeSeries> entry : Maps2.prefix(Stats.MIDQUOTE, stats.getTimeStats()).entrySet()) {
				TimeSeries midquote = entry.getValue();
				// compute log price volatility for this market
				double stdev = Sparse.stddev(midquote, period, simLength);

				features.get(prefix + entry.getKey() + "_stddev").add(stdev);

				stddev.add(stdev);
				if (stdev != 0) // XXX ?ideal? don't add if stddev is 0
					logPriceVol.add(Math.log(stdev));

				// compute log-return volatility for this market
				double logStddev = Sparse.logRatioStddev(midquote, period, simLength);
				features.get(prefix + "log_return_" + entry.getKey() + "_stddev").add(logStddev);
				logRetVol.add(logStddev);
			}

			// average measures across all markets in this model
			features.get(prefix + "mean_stddev_price").add(stddev.mean());
			features.get(prefix + "mean_log_price").add(logPriceVol.mean());
			features.get(prefix + "mean_log_return").add(logRetVol.mean());
		}
	}
	
	/** Represents all the necessary information on a player */
	protected static class PlayerObservation {
		public final SummStats payoff;
		public final Map<String, SummStats> features;
		
		protected PlayerObservation() {
			this.payoff = SummStats.on();
			this.features = Maps2.addDefault(Maps.<String, SummStats> newHashMap(), new Supplier<SummStats>() {
				@Override public SummStats get() { return SummStats.on(); }
			});
		}
		
		public static PlayerObservation create() {
			return new PlayerObservation();
		}
		
		public void observe(Player player) {
			payoff.add(player.getPayoff());
			for (Entry<String, Double> e : player.getFeatures().entrySet())
				features.get(e.getKey()).add(e.getValue());
		}
	}
	
	// Serializers for conversion to JSON
	public static final class SummStatsSerializer implements JsonSerializer<SummStats> {
		@Override
		public JsonElement serialize(SummStats stats, Type typeOfStats, JsonSerializationContext context) {
			return new JsonPrimitive(stats.mean());
		}
	}
	
	public static final class StddevFeaturesSerializer implements JsonSerializer<Map<String, SummStats>> {
		@Override
		public JsonElement serialize(Map<String, SummStats> stats, Type typeOfStats, JsonSerializationContext context) {
			JsonObject map = new JsonObject();
			for (Entry<String, SummStats> e : stats.entrySet()) {
				map.addProperty(e.getKey(), e.getValue().mean());
				map.addProperty(e.getKey() + "_stddev", e.getValue().stddev());
			}
			return map;
		}
	}
	
	public static final class PlayerObservationSerializer implements JsonSerializer<Multimap<String, PlayerObservation>> {
		@Override
		public JsonElement serialize(Multimap<String, PlayerObservation> players, Type typeOfPlayers, JsonSerializationContext context) {
			JsonArray array = new JsonArray();
			for (Entry<String, PlayerObservation> e : players.entries()) {
				JsonObject obs = new JsonObject();
				String[] roleStrat = e.getKey().split(" ", 2);
				obs.addProperty("role", roleStrat[0]);
				obs.addProperty("strategy", roleStrat[1]);
				obs.addProperty("payoff", e.getValue().payoff.mean());
				
				if (!e.getValue().features.isEmpty()) {
					JsonObject features = new JsonObject();
					for (Entry<String, SummStats> f : e.getValue().features.entrySet())
						features.addProperty(f.getKey(), f.getValue().mean());
					obs.add("features", features);
				}
				
				array.add(obs);
			}
			return array;
		}
	}
	
}
