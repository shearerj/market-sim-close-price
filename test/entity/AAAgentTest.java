package entity;

import static entity.AAAgent.AGGRESSION_KEY;
import static entity.AAAgent.BUYERSTATUS_KEY;
import static entity.AAAgent.DEBUG_KEY;
import static entity.AAAgent.ETA_KEY;
import static entity.AAAgent.HISTORICAL_KEY;
import static entity.AAAgent.THETAMAX_KEY;
import static entity.AAAgent.THETAMIN_KEY;
import static entity.AAAgent.THETA_KEY;
import static logger.Logger.log;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import logger.Logger;
import market.Price;
import model.DummyMarketModel;
import model.MarketModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import utils.RandPlus;
import activity.Activity;
import activity.SubmitNMSBid;
import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import data.Keys;
import event.TimeStamp;


public class AAAgentTest {

	private static FundamentalValue fund;
	private static RandPlus rand;
	private static EntityProperties agentProperties;
	
	private DummyMarketModel model;
	private Market market;
	private int agentIndex;
	public SIP dummySIP;
	
	@BeforeClass
	public static void setupClass() {
		//Setting up the log file
		Logger.setup(3, "simulations/unit_testing", "unit_tests.txt", true);
		
		//Creating the setup properties
		rand = new RandPlus(1);
		fund = new DummyFundamental(100000);
		
		//Setting up agentProperties
		agentProperties = new EntityProperties();
		agentProperties.put(Keys.REENTRY_RATE, 0.25);
		agentProperties.put(Keys.MAX_QUANTITY, 10);
		agentProperties.put(DEBUG_KEY, false);
		agentProperties.put(ETA_KEY, 3);
		agentProperties.put(HISTORICAL_KEY, 5);
		agentProperties.put(AGGRESSION_KEY, 0);
		agentProperties.put(THETA_KEY, 0);
		agentProperties.put(THETAMAX_KEY, 4);
		agentProperties.put(THETAMIN_KEY, -4);
	}
	
	@Before
	public void setupTest() {
		//Creating the MarketModel
		model = new DummyMarketModel(1);

		//XXX - Fix once SIP becomes important
		dummySIP = new SIP(1,1,new TimeStamp(0));
		
		market = new CDAMarket(1, model, 0);
		model.addMarket(market);
	}
	
	private AAAgent addAgent(MarketModel model, Market market, boolean isBuyer) {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(BUYERSTATUS_KEY, isBuyer);
		
		AAAgent agent = new AAAgent(agentIndex++, new TimeStamp(0), model, 
				market, rand, testProps);
		
		//model.addAgent(agent); --not sure about this!
		return agent;
	}
	
	private void addBid(MarketModel model, Market market, Price price, int q, TimeStamp ts) {
		//creating a dummy agent
		DummyAgent agent = new DummyAgent(agentIndex++, model, market);
		//Having the agent submit a bid to the market
		agent.agentStrategy(ts, price, q);
	}
	
	private void addTransaction(MarketModel model, Market market, Price p, int q, TimeStamp time) {
		addBid(model, market, p, q, time);
		addBid(model, market, p, -q, time);
		market.clear(time);
	}
		
	@Test
	public void initialBuyerTest() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing buyer on empty market: Result should be price=0");
		//Creating a buyer
		AAAgent agent = addAgent(model, market, true);
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
		Logger.log(Logger.Level.DEBUG, "Testing seller on empty market: Result should be price=" + Price.INF);
		//Creating a seller
		AAAgent agent = addAgent(model, market, false);
		//Testing against an empty market
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		
		//Checking that the submitted ask price is Price.INF
		Activity act = new SubmitNMSBid(agent, Price.INF, -1, Consts.INF_TIME, ts);
		//Finding the bid in the activity list
		for(Activity itr : test) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().equals(agent));
				assertTrue(bid.getPrice().equals(Price.INF));
				assertTrue(bid.getQuantity() == -1);
			}
		}
	}

	@Test
	public void noTransactionsBuyerTest() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing buyer on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "50000 < Bid price < 100000");
		
		//Setting up the bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(200000), -1, new TimeStamp(10));
		
		//Testing against a market with initial bids but no transaction history
		AAAgent agent = addAgent(model, market, true);
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

		//Adding setup bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(200000), -1, new TimeStamp(10));

		//Creating the agent and running the test
		AAAgent agent = addAgent(model, market, false);
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
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		//Transaction @75000
		addTransaction(model, market, new Price(75000), 1, new TimeStamp(10));
		
		//Setting up the agent
		AAAgent agent = addAgent(model, market, true);
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
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(75000), 1, new TimeStamp(10));
		
		AAAgent agent = addAgent(model, market, true);
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
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(75000), 1, new TimeStamp(10));
		
		AAAgent agent = addAgent(model, market, true);
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
		//TODO - Checking that Aggression updated correctly
		System.out.println("Aggression >= 1, actual: " + agent.getAggression());
		assertTrue("Aggression >= 1, actual: " + agent.getAggression(), agent.getAggression() < 1);
	}
	
	@Test
	public void checkIntraSellerPassive() { //Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing passive seller on market with transactions");
		
		//Adding Transactions and Bids
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(125000), 1, new TimeStamp(10));
		
		//Testing the Agent
		AAAgent agent = addAgent(model, market, false);
		Logger.log(Logger.Level.DEBUG, "Price ~= " + (150000 + (Price.INF.getPrice()-150000)/3));
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
				Price low = new Price(150000 + (Price.INF.getPrice()-150000)/3 - 5000);
				Price high = new Price(150000 + (Price.INF.getPrice()-150000)/3 + 5000);
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
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(125000), 1, new TimeStamp(10));
		
		AAAgent agent = addAgent(model, market, false);
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
		addBid(model, market, new Price(50000), 1, new TimeStamp(10));
		addBid(model, market, new Price(150000), -1, new TimeStamp(10));
		addTransaction(model, market, new Price(125000), 1, new TimeStamp(10));
		
		AAAgent agent = addAgent(model, market, false);
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
