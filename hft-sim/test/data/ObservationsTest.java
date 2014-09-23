package data;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static utils.Tests.j;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;
import systemmanager.SimulationSpec.PlayerSpec;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;
import com.google.common.util.concurrent.AtomicDouble;

import data.Observations.PlayerObservation;
import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.market.Market;
import event.TimeStamp;

public class ObservationsTest {
	private static final double eps = 1e-6;
	private static final Random rand = new Random();
	private MockSim sim;
	private Market one, two;
	
	@Before
	public void defaultSetup() throws IOException {
		setup();
	}
	
	public void setup(Object... parameters) throws IOException {
		sim = MockSim.create(getClass(), Log.Level.NO_LOGGING, ObjectArrays.concat(parameters,
				new Object[] { MarketType.CDA, j.join(Keys.NUM_MARKETS, 2) }, Object.class));
		Iterator<Market> markets = sim.getMarkets().iterator();
		one = markets.next();
		two = markets.next();
		assertFalse(markets.hasNext());
	}
	
	// FIXME General test for summary stats + whitelist
	// FIXME Fundamental test
	// FIXME Players Test
	// FIXME Test that sampling actually happens at the end of the interval, for all period tests

	/*
	 * TODO Still have some hard things to test, that should for the most part
	 * be correct. Most of these are basically fully tested elsewhere, but just
	 * need to verify that they get written correctly. These tests seem tedious
	 * and hard to write, so I'm not writing them for now.
	 */
	
	@Test
	public void meanSpreadTest() {
		Observations obs = Observations.create(HashMultiset.<PlayerSpec> create());
		
		sim.postTimedStat(TimeStamp.ZERO, Stats.SPREAD + one, 1);
		sim.postTimedStat(TimeStamp.of(1), Stats.SPREAD + one, 3);
		sim.postTimedStat(TimeStamp.of(2), Stats.SPREAD + one, 6);
		
		sim.postTimedStat(TimeStamp.ZERO, Stats.SPREAD + two, 5);
		
		// To verify NBBO spread doesn't affect mean spread
		sim.postTimedStat(TimeStamp.ZERO, Stats.NBBO_SPREAD, 100);
		
		obs.add(sim.getStats(), ImmutableList.<Agent> of(), ImmutableList.<Player> of(), 3);
		
		assertEquals(3, obs.features.get(Stats.SPREAD + one + "_median").mean(), eps);
		assertEquals(5, obs.features.get(Stats.SPREAD + two + "_median").mean(), eps);
		assertEquals(4, obs.features.get("spreads_mean").mean(), eps);
	}

	// TODO This could probably test better numbers for volatility
	// TODO There are a lot of conditionals for "bad" data that also need to be tested
	@Test
	public void volitilityTest() {
		Observations obs = Observations.create(HashMultiset.<PlayerSpec> create());

		sim.postTimedStat(TimeStamp.ZERO, Stats.MIDQUOTE + one, 103);
		sim.postTimedStat(TimeStamp.of(1000), Stats.MIDQUOTE + one, 106.5);
		sim.postTimedStat(TimeStamp.of(2000), Stats.MIDQUOTE + one, 104);
		
		sim.postTimedStat(TimeStamp.ZERO, Stats.MIDQUOTE + two, 50);
		sim.postTimedStat(TimeStamp.of(1000), Stats.MIDQUOTE + two, 100);
		sim.postTimedStat(TimeStamp.of(2000), Stats.MIDQUOTE + two, 20);
		
		obs.add(sim.getStats(), ImmutableList.<Agent> of(), ImmutableList.<Player> of(), 3000);
		
		assertEquals(1.4722055324274199, obs.features.get("vol_freq_1_" + Stats.MIDQUOTE + one + "_stddev").mean(), eps);
		assertEquals(1.5374122295716148, obs.features.get("vol_freq_250_" + Stats.MIDQUOTE + one + "_stddev").mean(), eps);
		assertEquals(33.003817550093338, obs.features.get("vol_freq_1_" + Stats.MIDQUOTE + two + "_stddev").mean(), eps);
		assertEquals(34.465617474213168, obs.features.get("vol_freq_250_" + Stats.MIDQUOTE + two + "_stddev").mean(), eps);
		
		assertEquals(17.238011541260377, obs.features.get("vol_freq_1_mean_stddev_price").mean(), eps);
		assertEquals(18.001514851892392, obs.features.get("vol_freq_250_mean_stddev_price").mean(), eps);
		
		assertEquals(1.9416924383396004, obs.features.get("vol_freq_1_mean_log_price").mean(), eps);
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
		Observations obs = Observations.create(HashMultiset.<PlayerSpec> create());

		sim.postTimedStat(TimeStamp.ZERO, Stats.NBBO_SPREAD, 1);
		sim.postTimedStat(TimeStamp.of(1), Stats.NBBO_SPREAD, 3);
		sim.postTimedStat(TimeStamp.of(2), Stats.NBBO_SPREAD, 6);

		obs.add(sim.getStats(), ImmutableList.<Agent> of(), ImmutableList.<Player> of(), 3);

		assertEquals(3, obs.features.get(Stats.NBBO_SPREAD + "_median").mean(), eps);
	}
	
	/*
	 * XXX If two transactions happen at the same time with different
	 * prices, this will reflect the last transaction to be matched, which
	 * ultimately depends on how the fourheap matches orders
	 */
	@Test
	public void transPricesTest() throws IOException {
		setup(
				Keys.FUNDAMENTAL_MEAN, 100,
				Keys.FUNDAMENTAL_SHOCK_VAR, 0);
		Observations obs = Observations.create(HashMultiset.<PlayerSpec> create());
		
		sim.postTimedStat(TimeStamp.ZERO, Stats.TRANSACTION_PRICE, 102);
		sim.postTimedStat(TimeStamp.ZERO, Stats.TRANSACTION_PRICE, 104);
		sim.postTimedStat(TimeStamp.of(1000), Stats.TRANSACTION_PRICE, 106);
		sim.postTimedStat(TimeStamp.of(2000), Stats.TRANSACTION_PRICE, 108);
		
		obs.add(sim.getStats(), ImmutableList.<Agent> of(), ImmutableList.<Player> of(), 3000);
		
		assertEquals(6.2182527020592095, obs.features.get("trans_freq_1_rmsd").mean(), eps);
		assertEquals(6.2182527020592095, obs.features.get("trans_freq_250_rmsd").mean(), eps);
		
	}
	
	@Test
	public void playerTest() {
		PlayerSpec spec = new PlayerSpec("role", "strategy", AgentType.NOOP, Props.fromPairs());
		Observations obs = Observations.create(HashMultiset.<PlayerSpec> create(ImmutableList.of(spec)));
		
		Agent agent = new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {			
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
			private Agent setProfit() {
				profit = -100000;
				return this;
			}
		}.setProfit();
		Player player = new Player(spec.descriptor, agent);
		
		obs.add(sim.getStats(), ImmutableList.of(agent), ImmutableList.of(player), 1);
		
		PlayerObservation pobs = Iterables.getOnlyElement(obs.players.get(spec.descriptor));
		
		assertEquals(-100000, pobs.payoff.mean(), eps);
	}

	// FIXME Change from BackgroundAgent to Agent once private value is moved
	@Test
	public void privateValueFeatureTest() {
		PlayerSpec spec = new PlayerSpec("role", "strategy", AgentType.NOOP, Props.fromPairs());
		Observations obs = Observations.create(HashMultiset.<PlayerSpec> create(ImmutableList.of(spec)));
		
		final AtomicDouble pv1 = new AtomicDouble();
		final AtomicDouble pv_1 = new AtomicDouble();
		
		Agent agent = new BackgroundAgent(sim, TimeStamp.ZERO, one, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestAgent " + id; }
			private Agent setup() {
				pv1.set(this.privateValue.getValue(0, BUY).doubleValue());
				pv_1.set(this.privateValue.getValue(0, SELL).doubleValue());
				return this;
			}
		}.setup();
		Player player = new Player(spec.descriptor, agent);
		
		double maxAbsPos = Math.max(Math.abs(pv1.get()), Math.abs(pv_1.get()));
		
		obs.add(sim.getStats(), ImmutableList.of(agent), ImmutableList.of(player), 1);
		
		PlayerObservation pobs = Iterables.getOnlyElement(obs.players.get(spec.descriptor));
		
		assertEquals(pv1.get(), pobs.features.get(Keys.PV_BUY1).mean(), eps);
		assertEquals(pv_1.get(), pobs.features.get(Keys.PV_SELL1).mean(), eps);
		assertEquals(maxAbsPos, pobs.features.get(Keys.PV_POSITION1_MAX_ABS).mean(), eps);
	}
	
	// FIXME Change from BackgroundAgent to Agent once private value is moved
	@Test
	public void playerFeatureMeanTest() {
		PlayerSpec spec = new PlayerSpec("role", "strategy", AgentType.NOOP, Props.fromPairs());
		Observations obs = Observations.create(HashMultiset.<PlayerSpec> create(ImmutableList.of(spec)));
		
		// Set up the first agent (a) and record their private values
		final AtomicDouble a_pv1 = new AtomicDouble();
		final AtomicDouble a_pv_1 = new AtomicDouble();
		
		Agent agent = new BackgroundAgent(sim, TimeStamp.ZERO, one, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestAgent " + id; }
			private Agent setup() {
				a_pv1.set(this.privateValue.getValue(0, BUY).doubleValue());
				a_pv_1.set(this.privateValue.getValue(0, SELL).doubleValue());
				return this;
			}
		}.setup();
		Player player = new Player(spec.descriptor, agent);
		
		double a_maxAbsPos = Math.max(Math.abs(a_pv1.get()), Math.abs(a_pv_1.get()));
		
		obs.add(sim.getStats(), ImmutableList.of(agent), ImmutableList.of(player), 1);
		
		// Set up the second agent (b) and record their private values
		final AtomicDouble b_pv1 = new AtomicDouble();
		final AtomicDouble b_pv_1 = new AtomicDouble();
		
		agent = new BackgroundAgent(sim, TimeStamp.ZERO, one, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestAgent " + id; }
			private Agent setup() {
				b_pv1.set(this.privateValue.getValue(0, BUY).doubleValue());
				b_pv_1.set(this.privateValue.getValue(0, SELL).doubleValue());
				return this;
			}
		}.setup();
		player = new Player(spec.descriptor, agent);
		
		double b_maxAbsPos = Math.max(Math.abs(b_pv1.get()), Math.abs(b_pv_1.get()));
		
		obs.add(sim.getStats(), ImmutableList.of(agent), ImmutableList.of(player), 1);
		
		// Test
		PlayerObservation pobs = Iterables.getOnlyElement(obs.players.get(spec.descriptor));
		
		assertEquals((a_pv1.get() + b_pv1.get()) / 2, pobs.features.get(Keys.PV_BUY1).mean(), eps);
		assertEquals((a_pv_1.get() + b_pv_1.get()) / 2, pobs.features.get(Keys.PV_SELL1).mean(), eps);
		assertEquals((a_maxAbsPos + b_maxAbsPos) / 2, pobs.features.get(Keys.PV_POSITION1_MAX_ABS).mean(), eps);
	}

}
