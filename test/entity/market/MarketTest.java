package entity.market;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;

import logger.Logger;
import model.MockMarketModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import entity.agent.Agent;
import entity.agent.MockAgent;
import event.TimeStamp;

public class MarketTest {

	private MockMarketModel model;
	private MockMarket market;
	private int agentIndex;

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File("simulations/unit_testing/unit_tests.txt"));
	}

	@Before
	public void setup() {
		model = new MockMarketModel(1);
		market = new MockMarket(1, model);
		model.addMarket(market);
		agentIndex = 1;
	}

	@Test
	public void AddBid() {
		Quote quote;
		Order order;
		Agent agent;
		Collection<Order> orders;
		TimeStamp time;
		
		time = new TimeStamp(0);
		agent = new MockAgent(agentIndex++, model, market);
		market.submitOrder(agent, new Price(1), 1, time);

		orders = market.orderMapping.values();
		assertFalse(orders.isEmpty());
		order = orders.iterator().next();
		assertEquals(new Price(1), order.getPrice());
		assertEquals(1, order.getQuantity());
		assertEquals(time, order.getSubmitTime());
		assertEquals(agent, order.getAgent());
		assertEquals(market, order.getMarket());
		quote = market.quote; 
		assertEquals(null, quote.ask);
		assertEquals(0, quote.quantityAsk);
		assertEquals(new Price(1), quote.bid);
		assertEquals(1, quote.quantityBid);
	}

	@Test
	public void AddAsk() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		MockAgent agent = new MockAgent(agentIndex++, model, market);
		
		// Creating and adding the bid
		market.submitOrder(agent, new Price(1), -1, time);

		Collection<Order> orders = market.orderMapping.values();
		assertFalse(orders.isEmpty());
		Order order = orders.iterator().next();
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
		MockAgent agent1 = new MockAgent(agentIndex++, model, market);
		MockAgent agent2 = new MockAgent(agentIndex++, model, market);
		

		// Creating and adding bids
		market.submitOrder(agent1, new Price(100), 1, time);
		market.submitOrder(agent2, new Price(100), -1, time);

		// Testing the market for the correct transaction
		market.clear(time);
		assertTrue(model.getTrans().size() == 1);
		for (Transaction tr : model.getTrans()) {
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
		MockAgent agent1 = new MockAgent(agentIndex++, model, market);
		MockAgent agent2 = new MockAgent(agentIndex++, model, market);
		
		// Creating and adding bids
		market.submitOrder(agent1, new Price(200), 1, time);
		market.submitOrder(agent2, new Price(50), -1, time);

		// Testing the market for the correct transaction
		market.clear(time);
		assertTrue(model.getTrans().size() == 1);
		for (Transaction tr : model.getTrans()) {
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
		MockAgent agent1 = new MockAgent(agentIndex++, model, market);
		MockAgent agent2 = new MockAgent(agentIndex++, model, market);
		MockAgent agent3 = new MockAgent(agentIndex++, model, market);
		MockAgent agent4 = new MockAgent(agentIndex++, model, market);
		
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
