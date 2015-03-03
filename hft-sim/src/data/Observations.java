package data;

import static com.google.common.base.Preconditions.checkState;

import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import systemmanager.Keys.SimLength;
import systemmanager.SimulationSpec;
import systemmanager.SimulationSpec.PlayerSpec;
import systemmanager.SimulationSpec.SimSpecSerializer;
import utils.Maps2;
import utils.Sparse;
import utils.SummStats;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

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

	// TODO This should probably go somewhere else
	public static final String // Observation Keys
	PV_BUY1 = 					"pv_buy1",
	PV_SELL1 = 					"pv_sell1",
	PV_POSITION1_MAX_ABS =		"pv_position_max_abs1";
		
	public static enum OutputType { DEFAULT, EGTA, STDDEV };
	
	protected final Multimap<String, PlayerObservation> players; // Player information
	protected final Map<String, SummStats> features; // Other features
	protected final SimulationSpec config;
	
	private transient final long simLength;
	private transient final Iterable<Integer> periods;
	private transient final Iterable<String> whitelist;

	private Observations(SimulationSpec simspec, Iterable<String> whitelist, Iterable<Integer> periods) {
		this.features = Maps2.addDefault(Maps.<String, SummStats> newHashMap(), new Supplier<SummStats>() {
			@Override public SummStats get() { return SummStats.on(); }
		});

		this.config = simspec;
		
		ImmutableListMultimap.Builder<String, PlayerObservation> builder = ImmutableListMultimap.builder();
		for (Multiset.Entry<PlayerSpec> spec : simspec.getPlayerProps().entrySet())
			for (int i = 0; i < spec.getCount(); ++i)
				builder.put(spec.getElement().descriptor, PlayerObservation.create());
		this.players = builder.build();

		this.simLength = simspec.getSimulationProps().get(SimLength.class);
		this.periods = periods;
		this.whitelist = whitelist;
	}
	
	public static Observations create(SimulationSpec simspec, Iterable<String> whitelist, Iterable<Integer> periods) {
		return new Observations(simspec, whitelist, periods);
	}
	
	// Modify this to add custom time series statistics
	// TODO move pieces into individual methods
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
		
		// General Statistics
		for (Entry<String, SummStats> e : stats.getSummaryStats().entrySet()) {
			features.get(e.getKey() + "_sum").add(e.getValue().sum());
			features.get(e.getKey() + "_mean").add(e.getValue().mean());
			features.get(e.getKey() + "_stddev").add(e.getValue().stddev());
		}

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

	public void write(Writer writer, OutputType output) {
		GsonBuilder gson = new GsonBuilder()
				.registerTypeAdapter(new TypeToken<Map<String, SummStats>>() {}.getType(),
						new FilteredFeatureSerializer(whitelist)) // Add standard deviations
				.registerTypeAdapter(new TypeToken<Multimap<String, PlayerObservation>>() {}.getType(),
						new PlayerObservationSerializer()) // Players
				.registerTypeAdapter(SimulationSpec.class, new SimSpecSerializer()); // Simulation Spec
		
		switch (output) {
		case EGTA:
			gson = gson
			.registerTypeAdapter(new TypeToken<Map<String, SummStats>>() {}.getType(),
					new NullSerializer()) // remove features
			.registerTypeAdapter(SimulationSpec.class, new NullSerializer()); // No specification
			break;
		case STDDEV:
			gson = gson.registerTypeAdapter(new TypeToken<Map<String, SummStats>>() {}.getType(),
					new StddevFeatureSerializer(whitelist)); // Add standard deviations
			// No break, because stddev implies default
			//$FALL-THROUGH$
		case DEFAULT:
			gson = gson
				.setPrettyPrinting() // Whitespace
				.serializeSpecialFloatingPointValues(); // NaNs
			break;
		default:
			throw new AssertionError("Unknown output type: " + output);
		}
		gson.create().toJson(this, writer);
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
	private static final class PlayerObservationSerializer implements JsonSerializer<Multimap<String, PlayerObservation>> {
		@Override
		public JsonElement serialize(Multimap<String, PlayerObservation> players, Type playersType, JsonSerializationContext context) {
			JsonArray array = new JsonArray();
			for (Entry<String, PlayerObservation> player : players.entries()) {
				JsonObject obs = new JsonObject();
				String[] roleStrat = player.getKey().split(" ", 2);
				obs.addProperty("role", roleStrat[0]);
				obs.addProperty("strategy", roleStrat[1]);
				obs.addProperty("payoff", player.getValue().payoff.mean());
				if (!player.getValue().features.isEmpty())
					obs.add("features", context.serialize(player.getValue().features,
							new TypeToken<Map<String, SummStats>>() {}.getType()));
				array.add(obs);
			}
			return array;
		}
	}
	
	private static class FilteredFeatureSerializer implements JsonSerializer<Map<String, SummStats>> {
		private final Iterable<String> regexes;
	
		private FilteredFeatureSerializer(Iterable<String> regexes) {
			this.regexes = regexes;
		}
		
		/** Test if string matches any of a set of regexes */
		private static boolean matchesAnyRegex(String string, Iterable<String> regexes) {
			for (String regex : regexes)
				if (string.matches(regex))
					return true;
			return false;
		}
		
		protected Iterable<Entry<String, Double>> process(String key, SummStats stats) {
			return ImmutableList.of(Maps.immutableEntry(key, stats.mean()));
		}
		
		@Override
		public JsonElement serialize(Map<String, SummStats> stats, Type typeOfStats, JsonSerializationContext context) {
			JsonObject map = new JsonObject();
			for (Entry<String, SummStats> e : stats.entrySet())
				if (matchesAnyRegex(e.getKey(), regexes))
					for (Entry<String, Double> add : process(e.getKey(), e.getValue()))
						map.addProperty(add.getKey(), add.getValue());
			return map;
		}
	}
	
	private static final class StddevFeatureSerializer extends FilteredFeatureSerializer {
		private StddevFeatureSerializer(Iterable<String> regexes) {
			super(regexes);
		}

		@Override
		protected Iterable<Entry<String, Double>> process(String key, SummStats stats) {
			return ImmutableList.<Entry<String, Double>> builder()
					.addAll(super.process(key, stats))
					.add(Maps.immutableEntry(key + "_stddev", stats.stddev()))
					.build();
		}
	}
	
	private static final class NullSerializer implements JsonSerializer<Object> {
		@Override
		public JsonElement serialize(Object obj, Type type, JsonSerializationContext context) {
			return JsonNull.INSTANCE;
		}
	}
	
	/**
	 * Adding this serializer should allow nan serialzation as strings so that
	 * egta will parse them, but it's use is not recommended.
	 */
	@SuppressWarnings("unused")
	private static final class StringNanSerializer implements JsonSerializer<Double> {
		@Override
		public JsonElement serialize(final Double src, final Type typeOfSrc, final JsonSerializationContext context) {
			if (src.isNaN()) {
				return new JsonPrimitive("NaN");
			}
			
			return new JsonPrimitive(src);
		}
	}
	
}
