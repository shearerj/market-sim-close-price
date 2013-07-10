package entity;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import logger.Logger;
import market.PQBid;
import market.Price;
import model.CentralCDA;
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

	private MarketModel model;
	private Market market;

	@BeforeClass
	public static void setupClass() {
		//Setting up the log file
		Logger.setup(3, "simulations/unit_testing", "unit_tests.txt", true);
		
		//Creating the setup properties
		playerConfig = new JsonObject();
		rand = new RandPlus(1);
		fund = new DummyFundamental(0, 100000, 0, rand);
		modelProperties = new ModelProperties(Consts.ModelType.CENTRALCDA);
		agentProperties = new HashMap<AgentProperties, Integer>();
	}
	
	@Before
	public void setup() {
		model = new CentralCDA(1, fund, agentProperties, modelProperties, playerConfig, rand);
		market = model.getMarkets().iterator().next();
		for(Market mkt : model.getMarkets()) market = mkt;
		assertTrue("Error setting up marketModel", market != null);
	}
	
	//TODO - more stringent verification
	@Test
	public void testAddBid(){
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		SIP sip = new SIP(1, 1);
		Agent agent = new ZIAgent(0, time, model, market, new RandPlus(), sip, 0, 0);
		
		//Creating and adding the bid
		PQBid testBid = new PQBid(agent, market, time);
		testBid.addPoint(1, new Price(1));
		market.addBid(testBid, time);
		
		//Testing the market
		assertTrue(market.getBids().containsValue(testBid));
	}

	@Test
	public void testAddAsk() {
		TimeStamp time = new TimeStamp(0);
		
		//Creating the agent
		SIP sip = new SIP(1, 1);
		Agent agent = new ZIAgent(0, time, model, market, new RandPlus(), sip, 0, 0);
		
		//Creating and adding the bid
		PQBid testBid = new PQBid(agent, market, time);
		testBid.addPoint(-1, new Price(1));
		market.addBid(testBid, time);
		
		//Testing the market
		assertTrue(market.getBids().containsValue(testBid));
	}
}
