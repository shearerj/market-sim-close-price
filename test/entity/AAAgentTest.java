package entity;

import static org.junit.Assert.*;
import activity.*;

import data.*;
import event.*;
import model.*;
import market.*;
import systemmanager.*;

import java.io.IOException;

//JUnit Dependencies
import org.junit.BeforeClass;
import org.junit.Test;


public class AAAgentTest {

	private static ObjectProperties prop;
	private static SystemData data;
	private static Log log;
	
	@BeforeClass
	public static void setup() {
		data = new SystemData();
		prop = new ObjectProperties();
		setupObjProp(prop);
		//setting up the log
		try {
			// log = new Log(Log.DEBUG, "/Users/drhurd/Developer/hft/simulations/aatesting", "unit_tests.txt", true);
			log = new Log(Log.DEBUG, "simulations/unit_testing", "unit_tests.txt", true);
		} catch (IOException e) {
			System.err.println("Error creating log");
			e.printStackTrace();
		}
		
	}
	
	@Test
	public void initialBuyerTest() {
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing buyer on empty market: Result should be price=0");
		data = createSystem();
		//Creating a buyer
		AAAgent agent = createAAAgent(true);
		//Testing against an empty market
		TimeStamp ts = new TimeStamp(100);
		ActivityHashMap test = agent.agentStrategy(ts);
		
		//Checking that the submitted bid price is 0
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() == 0);
				assertTrue(bid.getQuantity() == 1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
	}
	
	@Test
	public void initialSellerTest() {
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing seller on empty market: Result should be price=" + Consts.INF_PRICE);
		data = createSystem();
		//Creating a seller
		AAAgent agent = createAAAgent(false);
		//Testing against an empty market
		TimeStamp ts = new TimeStamp(100);
		ActivityHashMap test = agent.agentStrategy(ts);
		
		//Checking that the submitted ask price is Consts.INF_Price
		Activity act = new SubmitNMSBid(agent, Consts.INF_PRICE, -1, Consts.INF_TIME, ts);
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() == Consts.INF_PRICE);
				assertTrue(bid.getQuantity() == -1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
	}
	
	@Test
	public void noTransactionsBuyerTest() {
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing buyer on market with bids/asks but no transactions");
		log.log(Log.DEBUG, "50000 < Bid price < 100000");
		data = createSystem();
		agentIndex = 0;
		
		//Adding setup bids to the market
		addBid(data, 1, 50000, 10);
		addBid(data, -1, 200000, 10);
		
		//Testing against a market with initial bids but no transaction history
		AAAgent agent = createAAAgent(true);
		TimeStamp ts = new TimeStamp(100);
		ActivityHashMap test = agent.agentStrategy(ts);
		
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() > 50000);
				assertTrue(bid.getPrice() < 100000);
				assertTrue(bid.getQuantity() == 1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
	}
	
	@Test
	public void noTransactionsSellerTest() {
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing seller on market with bids/asks but no transactions");
		data = createSystem();
		agentIndex = 0;
		
		//Adding setup bids
		addBid(data, 1, 50000, 10);
		addBid(data, -1, 200000, 10);

		log.log(Log.DEBUG, "100000 < Ask price < 200000");
		AAAgent agent = createAAAgent(false);
		TimeStamp ts = new TimeStamp(100);
		ActivityHashMap test = agent.agentStrategy(ts);
		
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() > 100000);
				assertTrue(bid.getPrice() < 200000);
				assertTrue(bid.getQuantity() == -1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
	}
	
	@Test
	public void checkIntraBuyerPassive() {
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing passive buyer on market with transactions");
		//Setting up the data
		data = createSystem();
		agentIndex = 0;
		
		//Adding Transactions and Bids
		addBid(data, 1, 50000, 10);
		addBid(data, -1, 150000, 10);
		addTransaction(data, 1, 75000, 10);
		
		//Setting up the agent
		AAAgent agent = createAAAgent(true);
		agent.setAggression(-1);
		log.log(Log.DEBUG, "Price ~= 33334");
		TimeStamp ts = new TimeStamp(100);
		ActivityHashMap test = agent.agentStrategy(ts);
		log.log(Log.DEBUG, "Aggression adjusted from -1: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() > 30000);
				assertTrue(bid.getPrice() < 35000);
				assertTrue(bid.getQuantity() == 1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() > -1);
	}
	
	@Test
	public void checkIntraBuyerActive() {
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing active buyer on market with transactions");
		//Setting up the data
		data = createSystem();
		agentIndex = 0;
		
		//Adding Transactions and Bids
		addBid(data, 1, 50000, 10);
		addBid(data, -1, 150000, 10);
		addTransaction(data, 1, 75000, 10);
		
		AAAgent agent = createAAAgent(true);
		log.log(Log.DEBUG, "Price ~= 58333");
		agent.setAggression(0);
		TimeStamp ts = new TimeStamp(110);
		ActivityHashMap test = agent.agentStrategy(ts);
		log.log(Log.DEBUG, "Aggression adjusted from 0: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() > 55000);
				assertTrue(bid.getPrice() < 60000);
				assertTrue(bid.getQuantity() == 1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() > 0);
	}
	
	@Test
	public void checkIntraBuyerAggressive() {
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing aggressive buyer on market with transactions");
		//Setting up the data
		data = createSystem();
		agentIndex = 0;
		
		//Adding Transactions and Bids
		addBid(data, 1, 50000, 10);
		addBid(data, -1, 150000, 10);
		addTransaction(data, 1, 75000, 10);
		
		AAAgent agent = createAAAgent(true);
		log.log(Log.DEBUG, "Price ~= 66667");
		agent.setAggression(1);
		TimeStamp ts = new TimeStamp(120);
		ActivityHashMap test = agent.agentStrategy(ts);
		log.log(Log.DEBUG, "Aggression adjusted from 1: " + agent.getAggression());		
		
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() > 65000);
				assertTrue(bid.getPrice() < 70000);
				assertTrue(bid.getQuantity() == 1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() < 1);
	}
	
	@Test
	public void checkIntraSellerPassive() { //Check Aggression
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing passive seller on market with transactions");
		//Setting up the data
		data = createSystem();
		agentIndex = 0;
		
		//Adding Transactions and Bids
		addBid(data, 1, 50000, 10);
		addBid(data, -1, 150000, 10);
		addTransaction(data, 1, 125000, 10);
		
		//Testing the Agent
		AAAgent agent = createAAAgent(false);
		log.log(Log.DEBUG, "Price ~= " + (150000 + (Consts.INF_PRICE-150000)/3));
		agent.setAggression(-1);
		TimeStamp ts = new TimeStamp(100);
		ActivityHashMap test = agent.agentStrategy(ts);
		log.log(Log.DEBUG, "Aggression adjusted from -1: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() > (150000 + (Consts.INF_PRICE-150000)/3) - 5000);
				assertTrue(bid.getPrice() < (150000 + (Consts.INF_PRICE-150000)/3) + 5000);
				assertTrue(bid.getQuantity() == -1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() > -1);
	}
	
	@Test
	public void checkIntraSellerActive() { //Check Aggression
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing active seller on market with transactions");
		//Setting up the data
		data = createSystem();
		agentIndex = 0;
		
		//Adding Transactions and Bids
		addBid(data, 1, 50000, 10);
		addBid(data, -1, 150000, 10);
		addTransaction(data, 1, 125000, 10);
		
		AAAgent agent = createAAAgent(false);
		log.log(Log.DEBUG, "Price ~= 141667");
		agent.setAggression(0);
		TimeStamp ts = new TimeStamp(110);
		ActivityHashMap test = agent.agentStrategy(ts);
		log.log(Log.DEBUG, "Aggression adjusted from 0: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() > 138000);
				assertTrue(bid.getPrice() < 144000);
				assertTrue(bid.getQuantity() == -1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() > 0);		
	}
	
	@Test
	public void checkIntraSellerAggressive() { //Check Aggression
		log.log(Log.DEBUG, "");
		log.log(Log.DEBUG, "Testing aggressive seller on market with transactions");
		//Setting up the data
		data = createSystem();
		agentIndex = 0;
		
		//Adding Transactions and Bids
		addBid(data, 1, 50000, 10);
		addBid(data, -1, 150000, 10);
		addBid(data, 1, 125000, 10);
		addBid(data, -1, 125000, 10);
		
		AAAgent agent = createAAAgent(false);
		log.log(Log.DEBUG, "Price ~= 133334");
		agent.setAggression(1);
		TimeStamp ts = new TimeStamp(120);
		ActivityHashMap test = agent.agentStrategy(ts);	
		log.log(Log.DEBUG, "Aggression adjusted from 1: " + agent.getAggression());
		
		//Finding the bid in the activity list
		for(Activity itr : test.get(ts).getActivities()) {
			if(SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				//Casting the bid
				SubmitNMSBid bid = (SubmitNMSBid) itr;
				//Asserting the bid is correct
				assertTrue(bid.getAg().ID == agent.ID);
				assertTrue(bid.getPrice() > 130000);
				assertTrue(bid.getPrice() < 135000);
				assertTrue(bid.getQuantity() == -1);
				assertTrue(bid.getTime().equals(ts));
			}
		}
		//Checking that Aggression updated correctly
		assertTrue(agent.getAggression() < 1);
	}
	
	//
	// Private Setup Methods
	//
	private int agentIndex;
	private void addBid(SystemData data, int q, int p, int time) {
		Agent ag = new AAAgent(agentIndex++, 1, data, prop, log);
		data.addAgent(ag);
		PQBid bid = new PQBid(ag.ID, 1);
		bid.addPoint(q, new Price(p));
		bid.timestamp = new TimeStamp(time);
		data.getMarket(1).addBid(bid, bid.timestamp);
		data.addPrivateValue(bid.bidID, new Price(p));
	}
	
	private void addTransaction(SystemData data, int q, int p, int time) {
		Price price = new Price(p);
		TimeStamp ts = new TimeStamp(time);
		Agent buyer = new AAAgent(agentIndex++, 1, data, prop, log);
		data.addAgent(buyer);
		Agent seller = new AAAgent(agentIndex++, 1, data, prop, log);
		data.addAgent(seller);
		PQTransaction trade = new PQTransaction(q, price, buyer.ID, seller.ID, 0, 0, ts, 1);
		data.addTransaction(trade);
	}
	
	private AAAgent createAAAgent(boolean isBuyer) {
		AAAgent agent;
		do {
			agent = new AAAgent(agentIndex, 1, data, prop, log);
		}while(isBuyer != agent.getBuyerStatus() );
		agentIndex++;
		data.addAgent(agent);
		return agent;
	}
	
	private static void setupObjProp(ObjectProperties prop) {
		//AAAgent properties
		prop.put(AAAgent.TEST_KEY, "100000"); //sets static limit at 100000
		prop.put(Agent.ARRIVAL_KEY, "0");
		prop.put(AAAgent.MAXQUANTITY_KEY, "10");
		prop.put(SMAgent.MARKETID_KEY, "1");
		prop.put(AAAgent.DEBUG_KEY, "false");
		prop.put(AAAgent.ETA_KEY, "3");
		prop.put(AAAgent.HISTORICAL_KEY, "5");
		prop.put(AAAgent.AGGRESSION_KEY, "0");
		prop.put(AAAgent.THETA_KEY, "-1");
		prop.put(AAAgent.THETAMAX_KEY, "4");
		prop.put(AAAgent.THETAMIN_KEY, "-4");
		//CDAMarket Properties
		prop.put(Consts.MODEL_CONFIG_KEY, Consts.CENTRALCDA);
		
	}
	
	private SystemData createSystem() {
		//Creating SystemData
		SystemData data = new SystemData();
		data.setSIP(new SIP(0, data, log));
		//Creating the model and market
		MarketModel model = new CentralCDA(1, prop, data);
		Market market = new CDAMarket(1, data, prop, log);
		market.modelID = 1;
		data.addModel(model);
		data.addMarket(market);
		data.marketIDModelIDMap.put(1, 1);
		return data;
	}
}
