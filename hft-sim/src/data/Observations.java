package data;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import systemmanager.Consts;
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

import entity.agent.Agent;

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
	
	public static final String // Observation Keys
	PV_BUY1 = 					"pv_buy1",
	PV_SELL1 = 					"pv_sell1",
	PV_POSITION1_MAX_ABS =		"pv_position_max_abs1";
	
	// FIXME Change how multimap is output with a type converter
	// FIXME Change how summstats is output with a type converter. Allow different kinds (e.g. variance...)
	protected final Multimap<String, PlayerObservation> players;
	protected final Map<String, SummStats> features;

	// FIXME Add white list to dictate how to handle standard stats
	protected Observations(Multiset<PlayerSpec> playerProperties) {
		features = Maps2.addDefault(Maps.<String, SummStats> newHashMap(), new Supplier<SummStats>() {
			@Override public SummStats get() { return SummStats.on(); }
		});
		
		ImmutableListMultimap.Builder<String, PlayerObservation> builder = ImmutableListMultimap.builder();
		for (Multiset.Entry<PlayerSpec> spec : playerProperties.entrySet())
			for (int i = 0; i < spec.getCount(); ++i)
				builder.put(spec.getElement().descriptor, PlayerObservation.create());
		players = builder.build();
	}
	
	public static Observations create(Multiset<PlayerSpec> playerProperties) {
		return new Observations(playerProperties);
	}
	
	public void add(Stats stats, Collection<Agent> agents, Collection<Player> playerAgents, int simLength) {
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
			double median = Maths.median(Iterables.limit(e.getValue(), simLength));
			if (!e.getKey().equals(Stats.NBBO_SPREAD))
				spreadMedians.add(median);
			features.get(e.getKey() + "_median").add(median);
		}
		features.get("spreads_mean").add(spreadMedians.mean());
		
		// FIXME Make periods a simspec parameter
		// RMSD
		TimeSeries fundamental = stats.getTimeStats().get(Stats.FUNDAMENTAL);
		for (int period : Consts.PERIODS) {
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
		for (int period : Consts.PERIODS) {
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
	
}
