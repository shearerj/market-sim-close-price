package entity.market;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import data.DummyFundamental;
import data.FundamentalValue;

import entity.agent.MockAgent;
import entity.infoproc.SIP;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

public class CDAMarketTest {

	private FundamentalValue fundamental = new DummyFundamental(100000);
	private SIP sip;
	private Market market;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File("simulations/unit_testing/unit_tests.txt"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new CDAMarket(sip, TimeStamp.IMMEDIATE);
	}

	@Test
	public void AddBid() {
		TimeStamp time = new TimeStamp(0);

		// Creating the agent
		MockAgent agent = new MockAgent(fundamental, sip, market);

		// Creating and adding the bid
		market.submitOrder(agent, new Price(1), 1, time);

		Collection<Order> orders = market.orderMapping.values();
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
	}

	@Test
	public void AddAsk() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		MockAgent agent = new MockAgent(fundamental, sip, market);
		
		// Creating and adding the bid
		market.submitOrder(agent, new Price(1), -1, time);

		Collection<Order> orders = market.orderMapping.values();
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(-1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
	}

	@Test
	public void BasicEqualClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		

		// Creating and adding bids
		market.submitOrder(agent1, new Price(100), 1, time);
		market.submitOrder(agent2, new Price(100), -1, time);

		// Testing the market for the correct transaction
		market.clear(time);
		assertTrue(market.getTransactions().size() == 1);
		for (Transaction tr : market.getTransactions()) {
			assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
			assertTrue("Incorrect Seller", tr.getSeller().equals(agent2));
			assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
			assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		}
	}

	@Test
	public void BasicOverlapClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		
		// Creating and adding bids
		market.submitOrder(agent1, new Price(200), 1, time);
		market.submitOrder(agent2, new Price(50), -1, time);

		// Testing the market for the correct transaction
		market.clear(time);
		assertTrue(market.getTransactions().size() == 1);
		for (Transaction tr : market.getTransactions()) {
			assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
			assertTrue("Incorrect Seller", tr.getSeller().equals(agent2));
			assertTrue("Incorrect Price", tr.getPrice().equals(new Price(50)));
			assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		}
	}

	@Test
	public void MultiBidSingleClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(fundamental, sip, market);
		MockAgent agent2 = new MockAgent(fundamental, sip, market);
		MockAgent agent3 = new MockAgent(fundamental, sip, market);
		MockAgent agent4 = new MockAgent(fundamental, sip, market);
		
		// Creating and adding bids
		market.submitOrder(agent1, new Price(150),-1, time);
		market.submitOrder(agent2, new Price(100),-1, time);
		market.submitOrder(agent3, new Price(175), 1, time);
		market.submitOrder(agent4, new Price(125), 1, time);
		market.clear(time);
	}
	
	@Test
	public void ExtraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			MultiBidSingleClear();
		}
	}
}
