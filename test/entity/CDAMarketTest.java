package entity;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import logger.Logger;
import market.PQBid;
import market.Price;
import market.Transaction;
import model.CentralCDA;
import model.DummyMarketModel;
import model.MarketModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.DummyFundamental;
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
		for(Market mkt : model.getMarkets()) market = mkt;
		assertTrue("Error setting up marketModel", market != null);
	}
	
	@Test
	public void AddBid(){
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		//XXX - If/when sip becomes critical to agent function, must fix this (switch to agentfactory?)
		SIP sip = new SIP(1, 1, new TimeStamp(0));
		Agent agent = new ZIAgent(0, time, model, market, new RandPlus(), 0, 0);
		
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
		SIP sip = new SIP(1, 1, new TimeStamp(0));
		Agent agent = new ZIAgent(0, time, model, market, new RandPlus(), 0, 0);
		
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
		SIP sip = new SIP(1, 1, new TimeStamp(0));
		Agent agent1 = new ZIAgent(1, time, model, market, new RandPlus(), 0, 0);
		Agent agent2 = new ZIAgent(2, time, model, market, new RandPlus(), 0, 0);
		
		//Creating and adding bids
		PQBid testBid1 = new PQBid(agent1, market, time);
		testBid1.addPoint(1, new Price(100));
		market.addBid(testBid1, time);
		PQBid testBid2 = new PQBid(agent2, market, time);
		testBid1.addPoint(1, new Price(100));
		market.addBid(testBid2, time);
		
		//Testing the market for the correct transaction
		market.clear(time);
		for(Transaction tr : model.getTrans()) {
			assertTrue("Incorrect Buy Bid", tr.getBuyBid().equals(testBid1));
			assertTrue("Incorrect Sell Bid", tr.getSellBid().equals(testBid2));
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
		SIP sip = new SIP(1, 1, new TimeStamp(0));
		Agent agent1 = new ZIAgent(1, time, model, market, new RandPlus(), 0, 0);
		Agent agent2 = new ZIAgent(2, time, model, market, new RandPlus(), 0, 0);
		
		//Creating and adding bids
		PQBid testBid1 = new PQBid(agent1, market, time);
		testBid1.addPoint(1, new Price(50));
		market.addBid(testBid1, time);
		PQBid testBid2 = new PQBid(agent2, market, time);
		testBid1.addPoint(1, new Price(200));
		market.addBid(testBid2, time);
		
		//Testing the market for the correct transaction
		market.clear(time);
		for(Transaction tr : model.getTrans()) {
			assertTrue("Incorrect Buy Bid", tr.getBuyBid().equals(testBid1));
			assertTrue("Incorrect Sell Bid", tr.getSellBid().equals(testBid2));
			assertTrue("Incorrect Buyer", tr.getBuyer().equals(agent1));
			assertTrue("Incorrect Seller", tr.getSeller().equals(agent2));
			assertTrue("Incorrect Price", tr.getPrice().equals(new Price(100)));
			assertTrue("Incorrect Quantity", tr.getQuantity() == 1);
		}
	}
	
	@Test
	public void MultiClear() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating dummy agents
		SIP sip = new SIP(1, 1, new TimeStamp(0));
		Agent agent1 = new ZIAgent(1, time, model, market, new RandPlus(), 0, 0);
		Agent agent2 = new ZIAgent(2, time, model, market, new RandPlus(), 0, 0);
		Agent agent3 = new ZIAgent(3, time, model, market, new RandPlus(), 0, 0);
		Agent agent4 = new ZIAgent(4, time, model, market, new RandPlus(), 0, 0);
		
		//Creating and adding bids
		PQBid testBid1 = new PQBid(agent1, market, time);
		testBid1.addPoint(1, new Price(50));
		market.addBid(testBid1, time);
		PQBid testBid2 = new PQBid(agent2, market, time);
		testBid1.addPoint(1, new Price(200));
		market.addBid(testBid2, time);
		
	}
}
