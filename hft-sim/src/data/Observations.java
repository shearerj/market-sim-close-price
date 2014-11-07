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
import utils.Iterables2;
import utils.Maps2;
import utils.Maths;
import utils.SummStats;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
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
 * This class represents all of the final data about the simulation. Every piece
 * of information it gets must be stored in a Stats object during the simulation
 * 
 * @author erik
 */
public class Observations {
	
	public static final String // Observation Keys
	PV_BUY1 = 					"pv_buy1",
	PV_SELL1 = 					"pv_sell1",
	PV_POSITION1_MAX_ABS =		"pv_position_max_abs1";
	
	protected final Multimap<String, PlayerObservation> players;
	protected final Map<String, SummStats> features;
	
	private transient final int simLength;
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
		
		// General Statistics
		Map<String, SummStats> unseen = Maps.newHashMap(stats.getSummaryStats());
		// Sum
		for (String prefix : sumPrefixes) {
			for (Entry<String, SummStats> e : Maps2.prefix(prefix, stats.getSummaryStats()).entrySet()) {
				unseen.remove(e.getKey());
				features.get(e.getKey() + "_sum").add(e.getValue().sum());
			}
		}
		// Mean
		for (String prefix : meanPrefixes) {
			for (Entry<String, SummStats> e : Maps2.prefix(prefix, stats.getSummaryStats()).entrySet()) {
				unseen.remove(e.getKey());
				features.get(e.getKey() + "_mean").add(e.getValue().sum());
			}
		}
		// Standard Deviation
		for (String prefix : stddevPrefixes) {
			for (Entry<String, SummStats> e : Maps2.prefix(prefix, stats.getSummaryStats()).entrySet()) {
				unseen.remove(e.getKey());
				features.get(e.getKey() + "_stddev").add(e.getValue().sum());
			}
		}
		// Any unseen keys
		for (Entry<String, SummStats> e : unseen.entrySet()) {
			features.get(e.getKey() + "_sum").add(e.getValue().sum());
			features.get(e.getKey() + "_mean").add(e.getValue().mean());
			features.get(e.getKey() + "_stddev").add(e.getValue().stddev());
		}
		
		// Spreads
		SummStats spreadMedians = SummStats.on();
		for (Entry<String, TimeSeries> e : Maps2.prefix(Stats.SPREAD, stats.getTimeStats()).entrySet()) {
			double median = Maths.median(Iterables.limit(e.getValue(), simLength));
			if (!e.getKey().equals(Stats.NBBO_SPREAD))
				spreadMedians.add(median);
			features.get(e.getKey() + "_median").add(median);
		}
		features.get("spreads_mean").add(spreadMedians.mean());
		
		// RMSD
		TimeSeries fundamental = stats.getTimeStats().get(Stats.FUNDAMENTAL);
		for (int period : periods) {
			TimeSeries transPrices = stats.getTimeStats().get(Stats.TRANSACTION_PRICE);
			double rmsd = Double.NaN;
			if (transPrices != null) { // Will be null if there are no transactions
				Iterable<Double> pr = Iterables.limit(Iterables2.sample(transPrices, period, -1), simLength / period);
				Iterable<Double> fundStat = Iterables.limit(Iterables2.sample(fundamental, period, -1), simLength / period);
				rmsd = Maths.rmsd(pr, fundStat);
			}
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
				Iterable<Double> filtered = Iterables.filter(Iterables.limit(Iterables2.sample(midquote.removeNans(), period, -1), simLength / period), Predicates.notNull());
				double stdev = Maths.stddev(filtered);

				features.get(prefix + entry.getKey() + "_stddev").add(stdev);

				stddev.add(stdev);
				if (stdev != 0) // XXX ?ideal? don't add if stddev is 0
					logPriceVol.add(Math.log(stdev));

				// compute log-return volatility for this market
				// XXX Note change in log-return vol from before. Before if the ratio was
				// NaN it go thrown out. Now the previous value is used. Not sure if
				// this is correct
				double logStddev = Maths.stddev(Iterables.filter(Maths.logRatio(filtered),
						Predicates.not(Predicates.equalTo(Double.NaN))));

				features.get(prefix + "log_return_" + entry.getKey() + "_stddev").add(logStddev);
				logRetVol.add(logStddev);
			}

			// average measures across all markets in this model
			features.get(prefix + "mean_stddev_price").add(stddev.mean());
			features.get(prefix + "mean_log_price").add(logPriceVol.mean());
			features.get(prefix + "mean_log_return").add(logRetVol.mean());
		}
	}
	
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
