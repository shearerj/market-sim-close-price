package entity;

import static org.junit.Assert.assertTrue;

import java.io.File;

import logger.Logger;
import market.Price;
import market.Transaction;
import model.MockMarketModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import event.TimeStamp;

public class CDAMarketTest {

	private MockMarketModel model;
	private Market market;
	private int agentIndex;

	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, new File("simulations/unit_testing/unit_tests.txt"));
	}

	@Before
	public void setup() {
		model = new MockMarketModel(1);
		market = new CDAMarket(1, model, 0); // TODO Dummy IP
		model.addMarket(market);
		agentIndex = 1;
	}

	@Test
	public void AddBid() {
		TimeStamp time = new TimeStamp(0);

		// Creating the agent
		MockAgent agent = new MockAgent(agentIndex++, model, market);

		// Creating and adding the bid
		market.submitBid(agent, new Price(1), 1, time);

		// Testing the market
		// PQBid testBid = new PQBid(agent, market, time);
		// testBid.addPoint(1, new Price(1));
		// assertTrue(market.getBids().containsValue(testBid)); // TODO API broke this too
		// assertTrue(testBid.contains(market.getBidQuote()));
		// sure of a better way to test this.
	}

	@Test
	public void AddAsk() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		MockAgent agent = new MockAgent(agentIndex++, model, market);
		
		// Creating and adding the bid
		market.submitBid(agent, new Price(1), -1, time);

		// Testing the market
		// PQBid testBid = new PQBid(agent, market, time);
		// testBid.addPoint(-1, new Price(1));
		// assertTrue(market.getBids().containsValue(testBid)); // TODO API broke this
		// assertTrue(testBid.contains(market.getAskQuote()));
		// sure of a better way to test this.
	}

	@Test
	public void BasicEqualClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		MockAgent agent1 = new MockAgent(agentIndex++, model, market);
		MockAgent agent2 = new MockAgent(agentIndex++, model, market);
		

		// Creating and adding bids
		market.submitBid(agent1, new Price(100), 1, time);
		market.submitBid(agent2, new Price(100), -1, time);

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
		market.submitBid(agent1, new Price(200), 1, time);
		market.submitBid(agent2, new Price(50), -1, time);

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
		market.submitBid(agent1, new Price(150),-1, time);
		market.submitBid(agent2, new Price(100),-1, time);
		market.submitBid(agent3, new Price(175), 1, time);
		market.submitBid(agent4, new Price(125), 1, time);
		market.clear(time);
		System.out.println(model.getTrans().size());
	}
	
	@Test
	public void ExtraTest() {
		for(int i=0; i < 100; i++) {
			setup();
			MultiBidSingleClear();
		}
	}
}
