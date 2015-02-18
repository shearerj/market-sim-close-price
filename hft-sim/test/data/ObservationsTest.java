package data;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.SimLength;
import systemmanager.SimulationSpec;
import systemmanager.SimulationSpec.PlayerSpec;
import utils.Mock;
import utils.Rand;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.AtomicDouble;

import data.Observations.OutputType;
import data.Observations.PlayerObservation;
import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.position.PrivateValues;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

public class ObservationsTest {
	private static final double eps = 1e-6;
	private static final Rand rand = Rand.create();
	private Stats stats;
	private Market one, two;
		
	@Before
	public void setup() {
		stats = Stats.create();
		one = Mock.market();
		two = Mock.market();
	}
	
	/**
	 * Test that output reflects what's expected. Makes sure to include two
	 * roles, two strategies, and two agent types per strategy.
	 * 
	 * Also tests that features, whitespace, and config are not output. This
	 * test is fragile, but probably shouldn't change much.
	 */
	@Test
	public void egtaTest() {
		PlayerSpec pspec1 = new PlayerSpec("role1", "strat1", AgentType.NOOP, Props.fromPairs());
		PlayerSpec pspec2 = new PlayerSpec("role1", "strat2", AgentType.NOOP, Props.fromPairs());
		PlayerSpec pspec3 = new PlayerSpec("role2", "strat1", AgentType.NOOP, Props.fromPairs());
		
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.of(pspec1, pspec1, pspec2, pspec3));
		Observations obs = Observations.create(spec, ImmutableList.of(".*"), Ints.asList());
		
		stats.post("test", 5);
		
		obs.add(stats, ImmutableList.of(
				mockPlayer("role1", "strat1", stats, 1, ImmutableMap.of("test", 1d)),
				mockPlayer("role1", "strat1", stats, 2, ImmutableMap.<String, Double> of()),
				mockPlayer("role1", "strat2", stats, 3, ImmutableMap.<String, Double> of()),
				mockPlayer("role2", "strat1", stats, 4, ImmutableMap.<String, Double> of())));
		
		StringWriter obsString = new StringWriter();
		
		obs.write(obsString, OutputType.EGTA);
		assertEquals("{\"players\":[{\"role\":\"role1\",\"strategy\":\"strat1\",\"payoff\":1.0},{\"role\":\"role1\",\"strategy\":\"strat1\",\"payoff\":2.0},{\"role\":\"role1\",\"strategy\":\"strat2\",\"payoff\":3.0},{\"role\":\"role2\",\"strategy\":\"strat1\",\"payoff\":4.0}]}",
				obsString.toString());
	}
	
	/** Test that default output includes everything it should. */
	@Test
	public void defaultTest() {
		PlayerSpec pspec1 = new PlayerSpec("role1", "strat1", AgentType.NOOP, Props.fromPairs());
		
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.of(pspec1));
		Observations obs = Observations.create(spec, ImmutableList.of("test.*"), Ints.asList());
		
		stats.post("test", 5);
		
		obs.add(stats, ImmutableList.of(mockPlayer("role1", "strat1", stats, 1, ImmutableMap.of("test", 1d))));
		
		StringWriter obsString = new StringWriter();
		
		obs.write(obsString, OutputType.DEFAULT);
		assertEquals("{\n" +
				"  \"players\": [\n" +
				"    {\n" +
				"      \"role\": \"role1\",\n" +
				"      \"strategy\": \"strat1\",\n" +
				"      \"payoff\": 1.0,\n" +
				"      \"features\": {\n" +
				"        \"test\": 1.0\n" +
				"      }\n" +
				"    }\n" +
				"  ],\n" +
				"  \"features\": {\n" +
				"    \"test_stddev\": 0.0,\n" +
				"    \"test_mean\": 5.0,\n" +
				"    \"test_sum\": 5.0\n" +
				"  },\n" +
				"  \"config\": {}\n" +
				"}",
				obsString.toString());
	}
	
	/** Test that whitelist excludes fields properly */
	@Test
	public void whitelistTest() {		
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList());
		
		stats.post("test", 5);
		
		obs.add(stats, ImmutableList.<Player> of());
		
		StringWriter obsString = new StringWriter();

		obs.write(obsString, OutputType.DEFAULT);
		assertEquals("{\n" +
				"  \"players\": [],\n" +
				"  \"features\": {},\n" +
				"  \"config\": {}\n" +
				"}",
				obsString.toString());
	}
	
	/** Test basic summary statistic funcationality */
	@Test
	public void summaryStatsTest() {
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList());
		
		stats.post("test", 5);
		stats.post("test", 15);
		
		obs.add(stats, ImmutableList.<Player> of());
		
		assertEquals(20, obs.features.get("test_sum").mean(), eps);
		assertEquals(10, obs.features.get("test_mean").mean(), eps);
		assertEquals(7.0710678, obs.features.get("test_stddev").mean(), eps);
	}
	
	@Test
	public void fundamentalStatisticsTest() {
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(
				SimLength.class, 6l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList(1, 3));
		
		List<Integer> fakeFund = ImmutableList.of(20, 50, 14, 67, 34, 90, 20);
		List<Integer> transPrices = ImmutableList.of(45, 90, 20, 1, 100, 3, 5);
		
		for (int i = 0; i < fakeFund.size(); ++i) {
			stats.postTimed(TimeStamp.of(i), Stats.FUNDAMENTAL, fakeFund.get(i));
			stats.postTimed(TimeStamp.of(i), Stats.TRANSACTION_PRICE, transPrices.get(i));
		}
		
		obs.add(stats, ImmutableList.<Player> of());
		
		assertEquals(55.59076661940662, obs.features.get("trans_freq_1_rmsd").mean(), eps);
		assertEquals(61.6644143732834, obs.features.get("trans_freq_3_rmsd").mean(), eps);
	}
	
	@Test
	public void nbboSpreadTest() {
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(SimLength.class, 100l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList());
		
		stats.postTimed(TimeStamp.ZERO, Stats.NBBO_SPREAD, Double.POSITIVE_INFINITY);
		stats.postTimed(TimeStamp.of(20), Stats.NBBO_SPREAD, 10);
		stats.postTimed(TimeStamp.of(30), Stats.NBBO_SPREAD, 40);
		stats.postTimed(TimeStamp.of(50), Stats.NBBO_SPREAD, Double.POSITIVE_INFINITY);
		stats.postTimed(TimeStamp.of(57), Stats.NBBO_SPREAD, 2);
		stats.postTimed(TimeStamp.of(60), Stats.NBBO_SPREAD, 35);
		stats.postTimed(TimeStamp.of(95), Stats.NBBO_SPREAD, 100);
		stats.postTimed(TimeStamp.of(100), Stats.NBBO_SPREAD, Double.POSITIVE_INFINITY); // Not included
		
		obs.add(stats, ImmutableList.<Player> of());
		assertEquals(40, obs.features.get(Stats.NBBO_SPREAD + "_median").mean(), eps);
	}
	
	@Test
	public void meanSpreadTest() {
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(SimLength.class, 3l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList());
		
		stats.postTimed(TimeStamp.ZERO, Stats.SPREAD + one, 1);
		stats.postTimed(TimeStamp.of(1), Stats.SPREAD + one, 3);
		stats.postTimed(TimeStamp.of(2), Stats.SPREAD + one, 6);
		
		stats.postTimed(TimeStamp.ZERO, Stats.SPREAD + two, 5);
		
		// To verify NBBO spread doesn't affect mean spread
		stats.postTimed(TimeStamp.ZERO, Stats.NBBO_SPREAD, 100);
		
		obs.add(stats, ImmutableList.<Player> of());
		
		assertEquals(3, obs.features.get(Stats.SPREAD + one + "_median").mean(), eps);
		assertEquals(5, obs.features.get(Stats.SPREAD + two + "_median").mean(), eps);
		assertEquals(4, obs.features.get("spreads_mean").mean(), eps);
	}

	@Test
	public void volatilityTest() {
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(SimLength.class, 3000l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList(1, 250));

		stats.postTimed(TimeStamp.ZERO, Stats.MIDQUOTE + one, 103);
		stats.postTimed(TimeStamp.of(1000), Stats.MIDQUOTE + one, 106.5);
		stats.postTimed(TimeStamp.of(2000), Stats.MIDQUOTE + one, 104);
		
		stats.postTimed(TimeStamp.ZERO, Stats.MIDQUOTE + two, 50);
		stats.postTimed(TimeStamp.of(1000), Stats.MIDQUOTE + two, 100);
		stats.postTimed(TimeStamp.of(2249), Stats.MIDQUOTE + two, 20);
		
		obs.add(stats, ImmutableList.<Player> of());
		
		assertEquals(1.4722055324274199, obs.features.get("vol_freq_1_" + Stats.MIDQUOTE + one + "_stddev").mean(), eps);
		assertEquals(1.5374122295716148, obs.features.get("vol_freq_250_" + Stats.MIDQUOTE + one + "_stddev").mean(), eps);
		assertEquals(33.00650085344368, obs.features.get("vol_freq_1_" + Stats.MIDQUOTE + two + "_stddev").mean(), eps);
		assertEquals(34.465617474213168, obs.features.get("vol_freq_250_" + Stats.MIDQUOTE + two + "_stddev").mean(), eps);
		
		assertEquals(17.23935319293555, obs.features.get("vol_freq_1_mean_stddev_price").mean(), eps);
		assertEquals(18.001514851892392, obs.features.get("vol_freq_250_mean_stddev_price").mean(), eps);
		
		assertEquals(1.9417330880958312, obs.features.get("vol_freq_1_mean_log_price").mean(), eps);
		assertEquals(1.9850314323837965, obs.features.get("vol_freq_250_mean_log_price").mean(), eps);
		
		assertEquals(0.00074877135839063094, obs.features.get("vol_freq_1_log_return_" + Stats.MIDQUOTE + one + "_stddev").mean(), eps);
		assertEquals(0.012932126117835861, obs.features.get("vol_freq_250_log_return_" + Stats.MIDQUOTE + one + "_stddev").mean(), eps);
		assertEquals(0.032002665476869134, obs.features.get("vol_freq_1_log_return_" + Stats.MIDQUOTE + two + "_stddev").mean(), eps);
		assertEquals(0.54721267912580351, obs.features.get("vol_freq_250_log_return_" + Stats.MIDQUOTE + two + "_stddev").mean(), eps);

		assertEquals(0.016375718417629883, obs.features.get("vol_freq_1_mean_log_return").mean(), eps);
		assertEquals(0.2800724026218197, obs.features.get("vol_freq_250_mean_log_return").mean(), eps);
	}
	
	@Test
	public void nbboSpreadsTest() {
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(SimLength.class, 3l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList());

		stats.postTimed(TimeStamp.ZERO, Stats.NBBO_SPREAD, 1);
		stats.postTimed(TimeStamp.of(1), Stats.NBBO_SPREAD, 3);
		stats.postTimed(TimeStamp.of(2), Stats.NBBO_SPREAD, 6);

		obs.add(stats, ImmutableList.<Player> of());

		assertEquals(3, obs.features.get(Stats.NBBO_SPREAD + "_median").mean(), eps);
	}
	
	/*
	 * XXX If two transactions happen at the same time with different
	 * prices, this will reflect the last transaction to be matched, which
	 * ultimately depends on how the fourheap matches orders
	 */
	@Test
	public void transPricesTest() {
		FundamentalValue.create(stats, Mock.timeline, 0, 100, 0, 1.0, rand);
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(SimLength.class, 3000l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of());
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList(1, 250));
		
		stats.postTimed(TimeStamp.ZERO, Stats.TRANSACTION_PRICE, 102);
		stats.postTimed(TimeStamp.ZERO, Stats.TRANSACTION_PRICE, 104);
		stats.postTimed(TimeStamp.of(1000), Stats.TRANSACTION_PRICE, 106);
		stats.postTimed(TimeStamp.of(2000), Stats.TRANSACTION_PRICE, 108);
		
		obs.add(stats, ImmutableList.<Player> of());
		
		assertEquals(6.2182527020592095, obs.features.get("trans_freq_1_rmsd").mean(), eps);
		assertEquals(6.2182527020592095, obs.features.get("trans_freq_250_rmsd").mean(), eps);
		
	}
	
	@Test
	public void playerTest() {
		PlayerSpec pspec = new PlayerSpec("role", "strategy", AgentType.NOOP, Props.fromPairs());
		
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(SimLength.class, 1l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of(pspec));
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList());
				
		Agent buyer = new Agent(0, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, PrivateValues.zero(),
				Iterators.singletonIterator(TimeStamp.ZERO), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override
			protected void agentStrategy() {
				submitOrder(one.getPrimaryView(), BUY, Price.of(100000), 1);
			}
		};
		new Agent(0, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, PrivateValues.zero(),
				Iterators.singletonIterator(TimeStamp.ZERO), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override
			protected void agentStrategy() {
				submitOrder(one.getPrimaryView(), SELL, Price.of(100000), 1);
			}
		};
		
		// Buyer bought good, so profit = -100000;
		Player player = new Player(pspec.descriptor, buyer);
		
		obs.add(stats, ImmutableList.of(player));
		
		PlayerObservation pobs = Iterables.getOnlyElement(obs.players.get(pspec.descriptor));
		
		assertEquals(-100000, pobs.payoff.mean(), eps);
	}

	/**
	 * Tests player-level features (statistics on private values)
	 */
	@Test
	public void privateValueFeatureTest() {
		PlayerSpec pspec = new PlayerSpec("role", "strategy", AgentType.NOOP, Props.fromPairs());
		
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(SimLength.class, 1l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of(pspec));
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList());
		
		final AtomicDouble pv1 = new AtomicDouble();
		final AtomicDouble pv_1 = new AtomicDouble();
		
		Agent agent = new BackgroundAgent(0, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, one,
				Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			private Agent setup() {
				pv1.set(getPrivateValue(BUY).doubleValue());
				pv_1.set(getPrivateValue(SELL).doubleValue());
				return this;
			}
			@Override protected void agentStrategy() { }
		}.setup();
		Player player = new Player(pspec.descriptor, agent);
		
		double maxAbsPos = Math.max(Math.abs(pv1.get()), Math.abs(pv_1.get()));
		
		obs.add(stats, ImmutableList.of(player));
		
		PlayerObservation pobs = Iterables.getOnlyElement(obs.players.get(pspec.descriptor));
		
		assertEquals(pv1.get(), pobs.features.get(Observations.PV_BUY1).mean(), eps);
		assertEquals(pv_1.get(), pobs.features.get(Observations.PV_SELL1).mean(), eps);
		assertEquals(maxAbsPos, pobs.features.get(Observations.PV_POSITION1_MAX_ABS).mean(), eps);
	}
	
	@Test
	public void playerFeatureMeanTest() {
		PlayerSpec pspec = new PlayerSpec("role", "strategy", AgentType.NOOP, Props.fromPairs());
		
		SimulationSpec spec = SimulationSpec.create(Props.fromPairs(SimLength.class, 1l),
				ImmutableMultimap.<MarketType, Props> of(),
				ImmutableMultimap.<AgentType, Props> of(),
				ImmutableMultiset.<PlayerSpec> of(pspec));
		Observations obs = Observations.create(spec, ImmutableList.<String> of(), Ints.asList());
		
		// Set up the first agent (a) and record their private values
		final AtomicDouble a_pv1 = new AtomicDouble();
		final AtomicDouble a_pv_1 = new AtomicDouble();
		
		Agent agent = new BackgroundAgent(0, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, one,
				Props.fromPairs(BackgroundReentryRate.class, 0d)) {
			private static final long serialVersionUID = 1L;
			private Agent setup() {
				a_pv1.set(getPrivateValue(BUY).doubleValue());
				a_pv_1.set(getPrivateValue(SELL).doubleValue());
				return this;
			}
		}.setup();
		Player player = new Player(pspec.descriptor, agent);
		
		double a_maxAbsPos = Math.max(Math.abs(a_pv1.get()), Math.abs(a_pv_1.get()));
		
		obs.add(stats, ImmutableList.of(player));
		
		// Set up the second agent (b) and record their private values
		final AtomicDouble b_pv1 = new AtomicDouble();
		final AtomicDouble b_pv_1 = new AtomicDouble();
		
		agent = new BackgroundAgent(0, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, one,
				Props.fromPairs(BackgroundReentryRate.class, 0d)) {
			private static final long serialVersionUID = 1L;
			private Agent setup() {
				b_pv1.set(getPrivateValue(BUY).doubleValue());
				b_pv_1.set(getPrivateValue(SELL).doubleValue());
				return this;
			}
		}.setup();
		player = new Player(pspec.descriptor, agent);
		
		double b_maxAbsPos = Math.max(Math.abs(b_pv1.get()), Math.abs(b_pv_1.get()));
		
		obs.add(stats, ImmutableList.of(player));
		
		// Test
		PlayerObservation pobs = Iterables.getOnlyElement(obs.players.get(pspec.descriptor));
		
		assertEquals((a_pv1.get() + b_pv1.get()) / 2, pobs.features.get(Observations.PV_BUY1).mean(), eps);
		assertEquals((a_pv_1.get() + b_pv_1.get()) / 2, pobs.features.get(Observations.PV_SELL1).mean(), eps);
		assertEquals((a_maxAbsPos + b_maxAbsPos) / 2, pobs.features.get(Observations.PV_POSITION1_MAX_ABS).mean(), eps);
	}
	
	private static Player mockPlayer(String role, String strategy, Stats stats, final double payoff, final Map<String, Double> features) {
		return new Player(role + ' ' + strategy, new Agent(0, stats, Mock.timeline, Log.nullLogger(), Rand.create(), Mock.sip,
				Mock.fundamental, PrivateValues.zero(), ImmutableList.<TimeStamp> of().iterator(), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public double getPayoff() { return payoff; }
			@Override public Map<String, Double> getFeatures() { return features; }
		});
	}

}
