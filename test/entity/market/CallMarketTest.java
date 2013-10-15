package entity.market;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import data.DummyFundamental;
import data.FundamentalValue;

import entity.agent.MockAgent;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;

public class CallMarketTest {

	private FundamentalValue fundamental = new DummyFundamental(100000);
	private SIP sip;
	private Market market1;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File("simulations/unit_testing/unit_tests.txt"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		// no delay from SIP + clears every 100
		market1 = new CallMarket(sip, TimeStamp.IMMEDIATE, new Random(), 1, 0.5,
				new TimeStamp(100));
//		// no delay from SIP + clears immediately
//		market2 = new CallMarket(sip, TimeStamp.IMMEDIATE, new Random(), 1, 0.5,
//				TimeStamp.ZERO);
//		// delayed info + clears every 100
//		market2 = new CallMarket(sip, new TimeStamp(100), new Random(), 1, 0.5,
//				new TimeStamp(100));
//		// delayed info + clears immediately
//		market2 = new CallMarket(sip, new TimeStamp(100), new Random(), 1, 0.5,
//				TimeStamp.ZERO);
	}

	@Test
	public void AddBid() {
		TimeStamp time = new TimeStamp(0);

		// Creating the agent
		MockAgent agent = new MockAgent(fundamental, sip, market1);

		// Creating and adding the bid
		market1.submitOrder(agent, new Price(1), 1, time);

		Collection<Order> orders = market1.orderMapping.values();
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market1, order.getMarket());
	}

	@Test
	public void AddAsk() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		MockAgent agent = new MockAgent(fundamental, sip, market1);
		
		// Creating and adding the bid
		market1.submitOrder(agent, new Price(1), -1, time);

		Collection<Order> orders = market1.orderMapping.values();
		assertFalse(orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);
		assertEquals(new Price(1), order.getPrice());
		assertEquals(-1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market1, order.getMarket());
	}

	@Test
	public void BasicEqualClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(fundamental, sip, market1);
		MockAgent agent2 = new MockAgent(fundamental, sip, market1);
		

		// Creating and adding bids
		market1.submitOrder(agent1, new Price(100), 1, time);
		market1.submitOrder(agent2, new Price(100), -1, time);

		// Testing the market for the correct transaction
		market1.clear(time);
		assertTrue(market1.getTransactions().size() == 1);
		for (Transaction tr : market1.getTransactions()) {
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
		MockAgent agent1 = new MockAgent(fundamental, sip, market1);
		MockAgent agent2 = new MockAgent(fundamental, sip, market1);
		
		// Creating and adding bids
		market1.submitOrder(agent1, new Price(200), 1, time);
		market1.submitOrder(agent2, new Price(50), -1, time);

		// Testing the market for the correct transaction
		market1.clear(time);
		assertTrue(market1.getTransactions().size() == 1);
		for (Transaction tr : market1.getTransactions()) {
			assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
			assertTrue("Incorrect Seller", tr.getSeller().equals(agent2));
			// XXX The below line was 50, but this isn't a CDA so it should be 125, right?
			assertTrue("Incorrect Price", tr.getPrice().equals(new Price(125)));
			assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		}
	}
	
	@Test
	public void MultiBidSingleClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(fundamental, sip, market1);
		MockAgent agent2 = new MockAgent(fundamental, sip, market1);
		MockAgent agent3 = new MockAgent(fundamental, sip, market1);
		MockAgent agent4 = new MockAgent(fundamental, sip, market1);
		
		// Creating and adding bids
		market1.submitOrder(agent1, new Price(150),-1, time);
		market1.submitOrder(agent2, new Price(100),-1, time);
		market1.submitOrder(agent3, new Price(175), 1, time);
		market1.submitOrder(agent4, new Price(125), 1, time);
		market1.clear(time);
	}
	
	@Test
	public void ExtraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			MultiBidSingleClear();
		}
	}
}
