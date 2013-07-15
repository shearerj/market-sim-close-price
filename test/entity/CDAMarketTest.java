package entity;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import logger.Logger;
import market.PQBid;
import market.Price;
import market.Transaction;
import model.DummyMarketModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.FundamentalValue;
import data.ModelProperties;
import event.TimeStamp;

public class CDAMarketTest {
	
	private static FundamentalValue fund;
	private static RandPlus rand;
	private static ModelProperties modelProperties;
	private static Map<AgentProperties, Integer> agentProperties;
	private static JsonObject playerConfig;

	private DummyMarketModel model;
	private Market market;
	private int agentIndex;

	@BeforeClass
	public static void setupClass() {
		//Setting up the log file
		Logger.setup(3, "simulations/unit_testing", "unit_tests.txt", true);
	}
	
	@Before
	public void setup() {
		model = new DummyMarketModel(1);
		market = new CDAMarket(1, model, 0); // TODO Dummy IP
		model.addMarket(market);
		agentIndex = 1;
	}
	
	@Test
	public void AddBid(){
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		DummyAgent agent = new DummyAgent(agentIndex++, model, market);
		
		//Creating and adding the bid
		PQBid testBid = new PQBid(agent, market, time);
		testBid.addPoint(1, new Price(1));
		market.addBid(testBid, time);
		
		//Testing the market
		assertTrue(market.getBids().containsValue(testBid));
		assertTrue(testBid.contains(market.getBidQuote()));
	}

	@Test
	public void AddAsk() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		DummyAgent agent = new DummyAgent(agentIndex++, model, market);
		
		//Creating and adding the bid
		PQBid testBid = new PQBid(agent, market, time);
		testBid.addPoint(-1, new Price(1));
		market.addBid(testBid, time);
		
		//Testing the market
		assertTrue(market.getBids().containsValue(testBid));
		assertTrue(testBid.contains(market.getAskQuote()));
	}
	
	@Test
	public void BasicEqualClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		DummyAgent agent1 = new DummyAgent(agentIndex++, model, market);
		DummyAgent agent2 = new DummyAgent(agentIndex++, model, market);
		
		//Creating and adding bids
		PQBid bid1 = agent1.agentStrategy(time, new Price(100), 1);
		PQBid bid2 = agent2.agentStrategy(time, new Price(100), -1);
		
		//Testing the market for the correct transaction
		market.clear(time);
		assertTrue(model.getTrans().size() == 1);
		for(Transaction tr : model.getTrans()) {
			assertTrue("Incorrect Buy Bid", tr.getBuyBid().equals(bid1));
			assertTrue("Incorrect Sell Bid", tr.getSellBid().equals(bid2));
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
		DummyAgent agent1 = new DummyAgent(agentIndex++, model, market);
		DummyAgent agent2 = new DummyAgent(agentIndex++, model, market);
		
		//Creating and adding bids
		PQBid bid1 = agent1.agentStrategy(time, new Price(200), 1);
		PQBid bid2 = agent2.agentStrategy(time, new Price(50), -1);
		
		//Testing the market for the correct transaction
		market.clear(time);
		assertTrue(model.getTrans().size() == 1);
		for(Transaction tr : model.getTrans()) {
			assertTrue("Incorrect Buy Bid", tr.getBuyBid().equals(bid1));
			assertTrue("Incorrect Sell Bid", tr.getSellBid().equals(bid2));
			assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
			assertTrue("Incorrect Seller", tr.getSeller().equals(agent2));
			assertTrue("Incorrect Price", tr.getPrice().equals(new Price(50)));
			assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		}
	}
	
	@Test
	public void MultiBidSingleClear() {
		TimeStamp time = new TimeStamp(0);
		TimeStamp time2 = new TimeStamp(10);
		
		//Creating dummy agents
		DummyAgent agent1 = new DummyAgent(agentIndex++, model, market);
		DummyAgent agent2 = new DummyAgent(agentIndex++, model, market);
		DummyAgent agent3 = new DummyAgent(agentIndex++, model, market);
		DummyAgent agent4 = new DummyAgent(agentIndex++, model, market);
		
		//Creating and adding bids
		PQBid bid1 = agent1.agentStrategy(new TimeStamp(10), new Price(200), 1);
		PQBid bid3 = agent3.agentStrategy(new TimeStamp(20), new Price(100), -1);
		PQBid bid2 = agent2.agentStrategy(new TimeStamp(30), new Price(100), 1);
		PQBid bid4 = agent4.agentStrategy(new TimeStamp(40), new Price(100), -1);
		
		for(Transaction tr : model.getTrans()) {
			assertTrue(
					tr.getBuyBid().equals(bid1) &&
					tr.getBuyer().equals(agent1) &&
					tr.getSellBid().equals(bid3) &&
					tr.getSeller().equals(agent3) &&
					tr.getPrice().equals(new Price(200)) &&
					tr.getQuantity() == 1
					||
					tr.getBuyBid().equals(bid2) &&
					tr.getBuyer().equals(agent2) &&
					tr.getSellBid().equals(bid4) &&
					tr.getSeller().equals(agent4) &&
					tr.getPrice().equals(new Price(100)) &&
					tr.getQuantity() == 1
			);
		}
	}
}
