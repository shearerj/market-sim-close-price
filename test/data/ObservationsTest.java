package data;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static systemmanager.Executer.executeImmediate;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logger.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executer;
import systemmanager.Keys;
import systemmanager.SimulationSpec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import entity.agent.Agent;
import entity.agent.BackgroundAgent;
import entity.agent.DummyPrivateValue;
import entity.agent.MockAgent;
import entity.agent.MockBackgroundAgent;
import entity.agent.PrivateValue;
import entity.infoproc.SIP;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

public class ObservationsTest {
	
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market1, market2;
	private SIP sip;
	private Observations obs;

	@BeforeClass
	public static void setupClass() throws IOException {
		Log.log = Log.create(Log.Level.DEBUG, new File(Consts.TEST_OUTPUT_DIR + "ObservationsTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market1 = new MockMarket(sip);
		market2 = new MockMarket(sip);
	}
	
	@After
	public void tearDown() {
		if (obs != null)
			Observations.BUS.unregister(obs);
	}

	/*
	 * TODO Still have some hard things to test, that should for the most part
	 * be correct. Most of these are basically fully tested elsewhere, but just
	 * need to verify that they get written correctly. These tests seem tedious
	 * and hard to write, so I'm not writing them for now.
	 */
	
	@Test
	public void spreadsTest() {
		Agent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(fundamental, sip, market1);
		setupObservations(agent1, agent2);
		
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, BUY, new Price(104), 1, TimeStamp.ZERO);
		assertEquals(Double.POSITIVE_INFINITY, obs.spreads.get(market1).sample(1,1).get(0), 0.001);
		
		market1.submitOrder(agent2, SELL, new Price(105), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(106), 1, TimeStamp.ZERO);
		assertEquals(1, obs.spreads.get(market1).sample(1,1).get(0), 0.001);
		
		// Also sets median for market1 to 3
		market1.submitOrder(agent2, SELL, new Price(103), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(3, obs.spreads.get(market1).sample(1,1).get(0), 0.001);
		
		// Sets median of market2 to 5
		market2.submitOrder(agent2, SELL, new Price(103), 1, TimeStamp.ZERO);
		market2.submitOrder(agent2, BUY, new Price(98), 1, TimeStamp.ZERO);
		market2.clear(TimeStamp.ZERO);
		assertEquals(5, obs.spreads.get(market2).sample(1,1).get(0), 0.001);
		
		// Mean of 3 and 5
		assertEquals(4, obs.getFeatures().get("spreads_mean_markets"), 0.001);
	}
	
	@Test
	public void midquotesTest() {
		Agent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(fundamental, sip, market1);
		setupObservations(agent1, agent2);
		
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, BUY, new Price(104), 1, TimeStamp.ZERO);
		assertEquals(Double.NaN, obs.midQuotes.get(market1).sample(1,1).get(0), 0.001);
		
		market1.submitOrder(agent2, SELL, new Price(106), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(107), 1, TimeStamp.ZERO);
		assertEquals(105, obs.midQuotes.get(market1).sample(1,1).get(0), 0.001);
		
		market1.submitOrder(agent2, SELL, new Price(103), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(104, obs.midQuotes.get(market1).sample(1,1).get(0), 0.001);
	}
	
	@Test
	public void nbboSpreadsTest() {
		setupObservations();
		Quote q = new Quote(market1, new Price(80), 1, new Price(100), 1, TimeStamp.ZERO);
		sip.processQuote(market1, new DummyMarketTime(TimeStamp.ZERO, -2), q, TimeStamp.ZERO);
		
		// Check that correct spread stored
		List<Double> list = obs.nbboSpreads.sample(1, 1);
		assertEquals(1, list.size());
		assertEquals(20, list.get(0), 0.001);
		
		q = new Quote(market2, new Price(70), 1, new Price(90), 1, TimeStamp.ZERO);
		sip.processQuote(market2, new DummyMarketTime(TimeStamp.ZERO, -1), q, TimeStamp.ZERO);
		
		// Check that new quote overwrites the previously stored spread at time 0
		list = obs.nbboSpreads.sample(1, 1);
		assertEquals(1, list.size());
		assertEquals(10, list.get(0), 0.001);
		
		// Test with actual agents
		Agent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(fundamental, sip, market1);
		
		executeImmediate(market1.submitOrder(agent1, BUY, new Price(80), 1, TimeStamp.ZERO));
		executeImmediate(market2.submitOrder(agent2, SELL, new Price(100), 1, TimeStamp.ZERO));
		list = obs.nbboSpreads.sample(1, 1);
		assertEquals(1, list.size());
		assertEquals(20, list.get(0), 0.001);
	}
	
	@Test
	public void numTransTest() {
		Map<String, Double> features;
		TimeStamp time = TimeStamp.ZERO;

		Agent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(fundamental, sip, market1);
		Agent agent3 = new MockAgent(fundamental, sip, market1);
		
		// Two orders from one agent
		setupObservations(agent1);
		market1.submitOrder(agent1, BUY, new Price(102), 1, time);
		market1.submitOrder(agent1, SELL, new Price(102), 1, time);
		market1.clear(time);
		// One for each order type
		features = obs.getFeatures();
		assertEquals(2, obs.numTrans.count(MockBackgroundAgent.class));
		assertEquals(0, obs.numTrans.count(MockAgent.class));
		assertEquals(2, features.get("trans_mockbackgroundagent_num"), 0.001);

		// Two orders from same agent type
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, time);
		market1.submitOrder(agent2, SELL, new Price(102), 1, time);
		market1.clear(time);
		// One for each agent
		features = obs.getFeatures();
		assertEquals(2, obs.numTrans.count(MockBackgroundAgent.class));
		assertEquals(0, obs.numTrans.count(MockAgent.class));
		assertEquals(2, features.get("trans_mockbackgroundagent_num"), 0.001);
		
		// Two orders from different agent types
		setupObservations(agent1, agent3);
		market1.submitOrder(agent1, BUY, new Price(102), 1, time);
		market1.submitOrder(agent3, SELL, new Price(102), 1, time);
		market1.clear(time);
		// One for each agent
		features = obs.getFeatures();
		assertEquals(1, obs.numTrans.count(MockBackgroundAgent.class));
		assertEquals(1, obs.numTrans.count(MockAgent.class));
		assertEquals(1, features.get("trans_mockbackgroundagent_num"), 0.001);
		assertEquals(1, features.get("trans_mockagent_num"), 0.001);
		
		// One order is split among two, so transactions is "doubled"
		setupObservations(agent1, agent2, agent3);
		market1.submitOrder(agent1, BUY, new Price(102), 2, time);
		market1.submitOrder(agent2, SELL, new Price(102), 1, time);
		market1.submitOrder(agent3, SELL, new Price(102), 1, time);
		market1.clear(time);
		// Two for agent 1's split order, 1 for each of the other agents
		features = obs.getFeatures();
		assertEquals(3, obs.numTrans.count(MockBackgroundAgent.class));
		assertEquals(1, obs.numTrans.count(MockAgent.class));
		assertEquals(3, features.get("trans_mockbackgroundagent_num"), 0.001);
		assertEquals(1, features.get("trans_mockagent_num"), 0.001);
	}
	
	@Test
	public void executionSpeedsTest() {
		Agent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(fundamental, sip, market1);
		
		// Same times
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(0, obs.executionSpeeds.getMean(), 0.001);
		
		// Same times
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.create(1));
		assertEquals(1, obs.executionSpeeds.getMean(), 0.001);

		// Quantity weighted
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.create(1));
		market1.submitOrder(agent1, BUY, new Price(102), 3, TimeStamp.create(2));
		market1.submitOrder(agent2, SELL, new Price(102), 3, TimeStamp.create(2));
		market1.clear(TimeStamp.create(4));
		assertEquals(1.75, obs.executionSpeeds.getMean(), 0.001);
		
		// Split order
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 2, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.create(1));
		market1.clear(TimeStamp.create(1));
		assertEquals(0.75, obs.executionSpeeds.getMean(), 0.001);

		// Same agent
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.create(0));
		market1.submitOrder(agent1, SELL, new Price(102), 1, TimeStamp.create(1));
		market1.clear(TimeStamp.create(2));
		assertEquals(1.5, obs.executionSpeeds.getMean(), 0.001);
	}
	
	@Test
	public void pricesTest() {
		Agent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(fundamental, sip, market1);
		
		// Basic case
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(102, obs.prices.getMean(), 0.001);

		// Same agent
		setupObservations(agent1);
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, SELL, new Price(102), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(102, obs.prices.getMean(), 0.001);
		
		// Multi quantity
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(100), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(100), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		market1.submitOrder(agent1, BUY, new Price(200), 3, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(200), 3, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(150, obs.prices.getMean(), 0.001);
		
		// Split order NOTE: Clearing price is buy order
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, BUY, new Price(100), 4, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(80), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(60), 3, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(100, obs.prices.getMean(), 0.001);

		// Split order NOTE: Clearing price is buy order
		setupObservations(agent1, agent2);
		market1.submitOrder(agent1, SELL, new Price(50), 4, TimeStamp.ZERO);
		market1.submitOrder(agent2, BUY, new Price(80), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, BUY, new Price(60), 3, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(70, obs.prices.getMean(), 0.001);
	}
	
	@Test
	public void transPricesTest() {
		Agent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		Agent agent2 = new MockBackgroundAgent(fundamental, sip, market1);
		setupObservations(agent1, agent2);
		
		// Basic stuff from same agent 
		market1.submitOrder(agent1, BUY, new Price(102), 1, TimeStamp.ZERO);
		market1.submitOrder(agent1, SELL, new Price(102), 1, TimeStamp.ZERO);
		assertEquals(Double.NaN, obs.transPrices.sample(1,1).get(0), 0.001);
		market1.clear(TimeStamp.ZERO);
		assertEquals(102, obs.transPrices.sample(1,1).get(0), 0.001);
		
		// Now with different agent, check overwriting
		market1.submitOrder(agent1, BUY, new Price(104), 1, TimeStamp.ZERO);
		market1.submitOrder(agent2, SELL, new Price(104), 1, TimeStamp.ZERO);
		market1.clear(TimeStamp.ZERO);
		assertEquals(104, obs.transPrices.sample(1,1).get(0), 0.001);

		/*
		 * XXX What if two transactions happen at the same time with different
		 * prices. What price should be reflected? I believe it will reflect the
		 * last transaction to be matched, which ultimately depends on how the
		 * fourheap matches orders
		 */
		
		// Now add a transaction at a new time with multi quantity in a different market
		market2.submitOrder(agent1, BUY, new Price(106), 2, TimeStamp.ZERO);
		market2.submitOrder(agent2, SELL, new Price(106), 2, TimeStamp.ZERO);
		market2.clear(TimeStamp.create(1));
		assertEquals(ImmutableList.of(104d, 106d), obs.transPrices.sample(1,2));
		
		// Overwrite that with a split order
		market1.submitOrder(agent1, BUY, new Price(108), 1, TimeStamp.create(1));
		market1.submitOrder(agent2, SELL, new Price(108), 2, TimeStamp.create(1));
		market1.submitOrder(agent2, BUY, new Price(108), 1, TimeStamp.create(1));
		market1.clear(TimeStamp.create(1));
		assertEquals(ImmutableList.of(104d, 108d), obs.transPrices.sample(1,2));
	}
	
	@Test
	public void controlPVTest() {
		DummyPrivateValue pv = new DummyPrivateValue(2, ImmutableList.of(
				new Price(1200), new Price(1000), new Price(-200), new Price(-500)));
		Agent agent1 = new MockBackgroundAgent(fundamental, sip, market1, pv, 0, 5000);
		Agent agent2 = new MockBackgroundAgent(fundamental, sip, market1, pv, 0, 5000);
		setupObservations(agent1, agent2);
		assertEquals(375, obs.getFeatures().get("control_mean_private"), 0.001);
	}
	
	@Test
	public void controlFundamentalTest() {
		Random rand = new Random(1);
		FundamentalValue fund = FundamentalValue.create(0.5, 100000, 100000000, rand);
		fund.computeFundamentalTo(5);
		double tot = 0, lastVal = 0;
		for (double v : fund.meanRevertProcess) {
			tot += v;
			lastVal = v;
		}
		
		Agent agent1 = new MockBackgroundAgent(fund, sip, market1);
		Agent agent2 = new MockBackgroundAgent(fund, sip, market1);
		setupObservations(fund, agent1, agent2);
		
		assertEquals((tot + (obs.simLength-6)*lastVal) / obs.simLength, 
				obs.getFeatures().get("control_mean_fund"), 0.001);
	}
	
	@Test
	public void playerTest() {
		Agent backgroundAgent, agent;
		Player backgroundPlayer, player;
		
		// Basic case
		backgroundAgent = new MockBackgroundAgent(fundamental, sip, market1);
		agent = new MockAgent(fundamental, sip, market1);
		backgroundPlayer = new Player("background", "a", backgroundAgent);
		player = new Player("foreground", "b", agent);
		setupObservations(backgroundPlayer, player);
		
		// Double fundamental
		market1.submitOrder(agent, BUY, new Price(200000), 1, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 1, TimeStamp.ZERO);
		// To make sure agent1 sees clear
		Executer.execute(market1.clear(TimeStamp.ZERO));
		agent.liquidateAtFundamental(TimeStamp.ZERO);
		for (PlayerObservation po : obs.getPlayerObservations()) {
			if (po.role.equals("foreground")) {
				assertEquals("b", po.strategy);
				assertEquals(-100000, po.payoff, 0.001);
			} else { // Background
				assertEquals("a", po.strategy);
				assertEquals(100000, po.payoff, 0.001);
			}
		}
		
		// Multiple Quantity
		backgroundAgent = new MockBackgroundAgent(fundamental, sip, market1);
		agent = new MockAgent(fundamental, sip, market1);
		backgroundPlayer = new Player("background", "a", backgroundAgent);
		player = new Player("foreground", "b", agent);
		setupObservations(backgroundPlayer, player);
		
		market1.submitOrder(agent, BUY, new Price(200000), 2, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 2, TimeStamp.ZERO);
		Executer.execute(market1.clear(TimeStamp.ZERO));
		agent.liquidateAtFundamental(TimeStamp.ZERO);
		for (PlayerObservation po : obs.getPlayerObservations()) {
			if (po.role.equals("foreground")) {
				assertEquals("b", po.strategy);
				assertEquals(-200000, po.payoff, 0.001);
			} else { // Background
				assertEquals("a", po.strategy);
				assertEquals(200000, po.payoff, 0.001);
			}
		}
		
		// Split Orders
		backgroundAgent = new MockBackgroundAgent(fundamental, sip, market1);
		agent = new MockAgent(fundamental, sip, market1);
		backgroundPlayer = new Player("background", "a", backgroundAgent);
		player = new Player("foreground", "b", agent);
		setupObservations(backgroundPlayer, player);
		
		market1.submitOrder(agent, BUY, new Price(200000), 2, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 1, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 1, TimeStamp.ZERO);
		Executer.execute(market1.clear(TimeStamp.ZERO));
		agent.liquidateAtFundamental(TimeStamp.ZERO);
		for (PlayerObservation po : obs.getPlayerObservations()) {
			if (po.role.equals("foreground")) {
				assertEquals("b", po.strategy);
				assertEquals(-200000, po.payoff, 0.001);
			} else { // Background
				assertEquals("a", po.strategy);
				assertEquals(200000, po.payoff, 0.001);
			}
		}
		
		// Liquidate at different price
		backgroundAgent = new MockBackgroundAgent(fundamental, sip, market1);
		agent = new MockAgent(fundamental, sip, market1);
		backgroundPlayer = new Player("background", "a", backgroundAgent);
		player = new Player("foreground", "b", agent);
		setupObservations(backgroundPlayer, player);
		
		market1.submitOrder(agent, BUY, new Price(200000), 1, TimeStamp.ZERO);
		market1.submitOrder(backgroundAgent, SELL, new Price(200000), 1, TimeStamp.ZERO);
		Executer.execute(market1.clear(TimeStamp.ZERO));
		agent.liquidateAtPrice(new Price(300000), TimeStamp.ZERO);
		for (PlayerObservation po : obs.getPlayerObservations()) {
			if (po.role.equals("foreground")) {
				assertEquals("b", po.strategy);
				assertEquals(100000, po.payoff, 0.001);
			} else { // Background
				assertEquals("a", po.strategy);
				assertEquals(100000, po.payoff, 0.001);
			}
		}
	}
	
	@Test
	public void privateValueFeatureTest() {
		PrivateValue pv = new PrivateValue(5, 1000000, new Random());
		FundamentalValue randFundamental = new FundamentalValue(0.2, 100000, 10000, new Random());
		BackgroundAgent backgroundAgent = new MockBackgroundAgent(randFundamental, sip, market1, pv, 0, 1000);

		// Get valuation for various positionBalances
		int pv1 = pv.getValue(0, BUY).intValue();
		int pv_1 = pv.getValue(0, SELL).intValue();
		
		Player backgroundPlayer = new Player("background", "a", backgroundAgent);
		setupObservations(backgroundPlayer);
		
		for (PlayerObservation po : obs.getPlayerObservations()) {
			assertEquals(new Double(pv1), po.features.get(Keys.PV_BUY1));
			assertEquals(new Double(pv_1), po.features.get(Keys.PV_SELL1));
			assertEquals(new Double(Math.max(Math.abs(pv1), Math.abs(pv_1))), 
					po.features.get(Keys.PV_POSITION1_MAX_ABS));
		}
	}
	
	private void setupObservations() {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), ImmutableList.<Agent> of(),
				ImmutableList.<Player> of(), fundamental);
		
		Observations.BUS.register(obs);
	}
	
	private void setupObservations(Agent... agents) {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), Arrays.asList(agents),
				ImmutableList.<Player> of(), fundamental);
		
		Observations.BUS.register(obs);
	}
	
	private void setupObservations(FundamentalValue fundamental, Agent... agents) {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), Arrays.asList(agents),
				ImmutableList.<Player> of(), fundamental);
		
		Observations.BUS.register(obs);
	}
	
	private void setupObservations(Player... players) {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		Builder<Agent> agentList = ImmutableList.builder();
		for (Player player : players)
			agentList.add(player.getAgent());
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), agentList.build(),
				Arrays.asList(players), fundamental);
		
		Observations.BUS.register(obs);
	}

}
