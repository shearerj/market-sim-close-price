package entity;

import static logger.Logger.log;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import utils.RandPlus;

import activity.Activity;
import activity.SubmitNMSBid;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import data.ModelProperties;
import event.TimeStamp;

import logger.Logger;
import market.Bid;
import market.PQBid;
import market.Price;
import model.*;


public class AAAgentTest {

	private static MarketModelFactory marketModelFactory;
	private static FundamentalValue fund;
	private static RandPlus rand;
	private static ModelProperties modelProperties;
	private static EntityProperties agentProperties;
	
	private MarketModel model;
	private Market market;
	private AAAgent agent;
	private int agentIndex;
	
	@BeforeClass
	public static void setupClass() {
		//Creating the setup properties
		Map<AgentProperties, Integer> props = new HashMap<AgentProperties, Integer>();
		JsonObject playerConfig = new JsonObject();
		rand = new RandPlus(1);
		fund = new DummyFundamental(0, 100000, 0, rand);
		
		//Setting up the MarketModelFactory
		marketModelFactory = new MarketModelFactory(props, playerConfig, fund, rand);
		modelProperties = new ModelProperties(Consts.ModelType.CENTRALCDA);
		
		//Setting up agentProperties
		agentProperties = new EntityProperties(
				Agent.ARRIVAL_KEY + "_0_" +
				AAAgent.MAXQUANTITY_KEY + "_10_" +
				AAAgent.DEBUG_KEY + "_false_" +
				AAAgent.ETA_KEY + "_3_" +
				AAAgent.HISTORICAL_KEY + "_5_" +
				AAAgent.AGGRESSION_KEY + "_0_" +
				AAAgent.THETA_KEY + "_-1_" +
				AAAgent.THETAMAX_KEY + "_4_" +
				AAAgent.THETAMIN_KEY +"_-4_"
				);
	}
	
	@Before
	public void setupTest() {
		//Creating the MarketModel
		model = marketModelFactory.createModel(modelProperties);
		Collection<Market> markets = model.getMarkets();
		for(Market mkt : markets) market = mkt;
	}
	
	private AAAgent addAAAgent(MarketModel model, boolean isBuyer) {
		Collection<Market> markets = model.getMarkets();
		Market market = null;
		for(Market mkt : markets) market = mkt;
		do {
			agent = new AAAgent(agentIndex, new TimeStamp(0), model, market, rand, agentProperties);
		}while(agent.getBuyerStatus() != isBuyer);
		
		++agentIndex;
		
		model.addAgent(agent);
		return agent;
	}
	
	private Bid addBid(Market market, int p, int q, int time) {
		TimeStamp bidTime = new TimeStamp(0);
		Bid bid1 = new PQBid(agent, market, bidTime);
		
		return null;
	}
	
	@Test
	public void initialBuyerTest() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing buyer on empty market: Result should be price=0");
		//Creating a buyer
		AAAgent agent = addAAAgent(model, true);
		//Testing against an empty market
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		
		//Checking that the submitted bid price is 0
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().equals(new Price(0)));
				assertTrue(bid.getQuantity() == 1);
			}
		}
	}
	
	@Test
	public void initialSellerTest() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing seller on empty market: Result should be price=" + Consts.INF_PRICE);
		//Creating a seller
		AAAgent agent = addAAAgent(model, false);
		//Testing against an empty market
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		
		//Checking that the submitted ask price is Consts.INF_Price
		Activity act = new SubmitNMSBid(agent, Consts.INF_PRICE, -1, Consts.INF_TIME, ts);
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().equals(Consts.INF_PRICE));
				assertTrue(bid.getQuantity() == -1);
			}
		}
	}

	@Test
	public void noTransactionsBuyerTest() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing buyer on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "50000 < Bid price < 100000");
		
		//Testing against a market with initial bids but no transaction history
		AAAgent agent = addAAAgent(model, true);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().greaterThan(new Price(50000)));
				assertTrue(bid.getPrice().lessThan(new Price(100000)));
				assertTrue(bid.getQuantity() == 1);
			}
		}
	}
	
	@Test
	public void noTransactionsSellerTest() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing seller on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "100000 < Ask price < 200000");

		AAAgent agent = addAAAgent(model, false);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().greaterThan(new Price(100000)));
				assertTrue(bid.getPrice().lessThan(new Price(200000)));
				assertTrue(bid.getQuantity() == -1);
			}
		}
	}
	
	@Test
	public void checkIntraBuyerPassive() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing passive buyer on market with transactions");
		
		//Adding Transactions and Bids
		addBid(market, 1, 50000, 10);
		addBid(market, -1, 150000, 10);
		//TODO - addTransaction(market, 1, 75000, 10);
		
		//Setting up the agent
		AAAgent agent = addAAAgent(model, true);
		agent.setAggression(-1);
		Logger.log(Logger.Level.DEBUG, "Price ~= 33334");
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		Logger.log(Logger.Level.DEBUG, "Aggression adjusted from -1: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().greaterThan(new Price(30000)));
				assertTrue(bid.getPrice().lessThan(new Price(35000)));
				assertTrue(bid.getQuantity() == 1);
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() > -1);
	}
	
	@Test
	public void checkIntraBuyerActive() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing active buyer on market with transactions");
		
		//Adding Transactions and Bids
		addBid(market, 1, 50000, 10);
		addBid(market, -1, 150000, 10);
		//TODO - addTransaction(market, 1, 75000, 10);
		
		AAAgent agent = addAAAgent(model, true);
		Logger.log(Logger.Level.DEBUG, "Price ~= 58333");
		agent.setAggression(0);
		TimeStamp ts = new TimeStamp(110);
		Collection<Activity> test = agent.agentStrategy(ts);
		Logger.log(Logger.Level.DEBUG, "Aggression adjusted from 0: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().greaterThan(new Price(55000)));
				assertTrue(bid.getPrice().lessThan(new Price(60000)));
				assertTrue(bid.getQuantity() == 1);
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() > 0);
	}
	
	@Test
	public void checkIntraBuyerAggressive() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing aggressive buyer on market with transactions");
		
		//Adding Transactions and Bids
		addBid(market, 1, 50000, 10);
		addBid(market, -1, 150000, 10);
		//TODO - addTransaction(market, 1, 75000, 10);
		
		AAAgent agent = addAAAgent(model, true);
		Logger.log(Logger.Level.DEBUG, "Price ~= 66667");
		agent.setAggression(1);
		TimeStamp ts = new TimeStamp(120);
		Collection<Activity> test = agent.agentStrategy(ts);
		Logger.log(Logger.Level.DEBUG, "Aggression adjusted from 1: " + agent.getAggression());		
		
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().greaterThan(new Price(65000)));
				assertTrue(bid.getPrice().lessThan(new Price(70000)));
				assertTrue(bid.getQuantity() == 1);
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() < 1);
	}
	
	@Test
	public void checkIntraSellerPassive() { //Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing passive seller on market with transactions");
		
		//Adding Transactions and Bids
		addBid(market, 1, 50000, 10);
		addBid(market, -1, 150000, 10);
		//TODO - addTransaction(market, 1, 125000, 10);
		
		//Testing the Agent
		AAAgent agent = addAAAgent(model, false);
		Logger.log(Logger.Level.DEBUG, "Price ~= " + (150000 + (Consts.INF_PRICE.getPrice()-150000)/3));
		agent.setAggression(-1);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		Logger.log(Logger.Level.DEBUG, "Aggression adjusted from -1: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				Price low = new Price(150000 + (Consts.INF_PRICE.getPrice()-150000)/3 - 5000);
				Price high = new Price(150000 + (Consts.INF_PRICE.getPrice()-150000)/3 + 5000);
				assertTrue(bid.getPrice().greaterThan(low));
				assertTrue(bid.getPrice().lessThan(high));
				assertTrue(bid.getQuantity() == -1);
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() > -1);
	}
	
	@Test
	public void checkIntraSellerActive() { //Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing active seller on market with transactions");
		
		//Adding Transactions and Bids
		addBid(market, 1, 50000, 10);
		addBid(market, -1, 150000, 10);
		//TODO - addTransaction(market, 1, 125000, 10);
		
		AAAgent agent = addAAAgent(model, false);
		log(Logger.Level.DEBUG, "Price ~= 141667");
		agent.setAggression(0);
		TimeStamp ts = new TimeStamp(110);
		Collection<Activity> test = agent.agentStrategy(ts);
		log(Logger.Level.DEBUG, "Aggression adjusted from 0: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				Price low = new Price(138000);
				Price high = new Price(144000);
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().greaterThan(low));
				assertTrue(bid.getPrice().lessThan(high));
				assertTrue(bid.getQuantity() == -1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() > 0);		
	}
	
	@Test
	public void checkIntraSellerAggressive() { //Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing aggressive seller on market with transactions");
		
		//Adding Transactions and Bids
		addBid(market, 1, 50000, 10);
		addBid(market, -1, 150000, 10);
		addBid(market, 1, 125000, 10);
		addBid(market, -1, 125000, 10);
		
		AAAgent agent = addAAAgent(model, false);
		log(Logger.Level.DEBUG, "Price ~= 133334");
		agent.setAggression(1);
		TimeStamp ts = new TimeStamp(120);
		Collection<Activity> test = agent.agentStrategy(ts);	
		log(Logger.Level.DEBUG, "Aggression adjusted from 1: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				Price low = new Price(130000);
				Price high = new Price(135000);
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().greaterThan(low));
				assertTrue(bid.getPrice().lessThan(high));
				assertTrue(bid.getQuantity() == -1);
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() < 1);
	}
}
