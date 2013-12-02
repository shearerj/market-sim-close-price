package data;

import static org.junit.Assert.*;
import static fourheap.Order.OrderType.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import logger.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import systemmanager.Consts;
import systemmanager.SimulationSpec;

import entity.agent.Agent;
import entity.agent.MockBackgroundAgent;
import entity.infoproc.SIP;
import entity.market.DummyMarketTime;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Price;
import entity.market.Quote;
import entity.market.Transaction;
import event.TimeStamp;

public class ObservationsTest {
	
	private FundamentalValue fundamental = new DummyFundamental(100000);
	private Market market1, market2;
	private SIP sip;
	private Observations obs;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "MarketTest.log"));
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
	
	@Test
	public void depths() {
		TimeStamp time = TimeStamp.ZERO;

		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip,
				market1);
		setupObservations(agent1, agent2);
		
		market1.submitOrder(agent1, BUY, new Price(102), 1, time);
		market1.submitOrder(agent1, BUY, new Price(104), 1, time);
		assertEquals(new Double(Double.POSITIVE_INFINITY), obs.spreads.get(market1).sample(1,1).get(0));
		
		market1.submitOrder(agent2, SELL, new Price(105), 1, time);
		market1.submitOrder(agent2, SELL, new Price(106), 1, time);
		assertEquals(new Double(1), obs.spreads.get(market1).sample(1,1).get(0));
		
		market1.submitOrder(agent2, SELL, new Price(103), 1, time);
		market1.clear(time);
		assertEquals(new Double(3), obs.spreads.get(market1).sample(1,1).get(0));
	}
	
	@Test
	public void midquotes() {
		TimeStamp time = TimeStamp.ZERO;

		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market1);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market1);
		setupObservations(agent1, agent2);
		
		market1.submitOrder(agent1, BUY, new Price(102), 1, time);
		market1.submitOrder(agent1, BUY, new Price(104), 1, time);
		assertEquals(new Double(Double.NaN), obs.midQuotes.get(market1).sample(1,1).get(0));
		
		market1.submitOrder(agent2, SELL, new Price(106), 1, time);
		market1.submitOrder(agent2, SELL, new Price(107), 1, time);
		assertEquals(new Double(105), obs.midQuotes.get(market1).sample(1,1).get(0));
		
		market1.submitOrder(agent2, SELL, new Price(103), 1, time);
		market1.clear(time);
		assertEquals(new Double(104), obs.midQuotes.get(market1).sample(1,1).get(0));
	}
	
	@Test
	public void getNBBOSpreads() {
		TimeStamp time = TimeStamp.ZERO;
		setupObservations();
		Quote q1 = new Quote(market1, new Price(80), 1, new Price(100), 1, time);
		sip.processQuote(market1, new DummyMarketTime(time, 1), q1, ImmutableList.<Transaction> of(), time);
		
		// Check that correct spread stored
		List<Double> list = obs.nbboSpreads.sample(1, 1);
		assertEquals(new Double(20), list.get(0));
		assertEquals(1, list.size());
		
		Quote q2 = new Quote(market2, new Price(70), 1, new Price(90), 1, time);
		sip.processQuote(market2, new DummyMarketTime(time, 2), q2, ImmutableList.<Transaction> of(), time);
		
		// Check that new quote overwrites the previously stored spread at time 0
		list = obs.nbboSpreads.sample(1, 1);
		assertEquals(new Double(10), list.get(0));
		assertEquals(1, list.size());
	}
	
	private void setupObservations(Agent... agents) {
		if (obs != null)
			Observations.BUS.unregister(obs);
		
		obs = new Observations(new SimulationSpec(),
				ImmutableList.of(market1, market2), Arrays.asList(agents),
				ImmutableList.<Player> of(), fundamental);
		
		Observations.BUS.register(obs);
	}

}
