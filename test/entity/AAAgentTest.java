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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import logger.Logger;
import market.PQBid;
import market.Price;
import model.MockMarketModel;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import utils.RandPlus;
import activity.Activity;
import activity.ProcessQuote;
import activity.SendToIP;
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

	private MockMarketModel model;
	private Market market;
	private int agentIndex;

	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, new File("simulations/unit_testing/AA_unit_tests.txt"));

		// Creating the setup properties
		rand = new RandPlus(1);
		fund = new DummyFundamental(100000);

		// Setting up agentProperties
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
		// Creating the MockMarketModel
		model = new MockMarketModel(1);

		// Creating the MockMarket
		market = new MockMarket(model);

		model.addMarket(market);
	}

	private AAAgent addAgent(boolean isBuyer) {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(BUYERSTATUS_KEY, isBuyer);

		AAAgent agent = new AAAgent(agentIndex++, new TimeStamp(0), model,
				market, rand, testProps);

		return agent;
	}

	private void addBid(Price price, int quantity,
			TimeStamp time) {
		// creating a dummy agent
		MockAgent agent = new MockAgent(agentIndex++, model, market);
		// Having the agent submit a bid to the market
		Collection<? extends Activity> bidActs = market.submitBid(agent, price,
				quantity, time);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
		Collection<Activity> sendActs = new ArrayList<Activity>();
		for (Activity act : bidActs)
			if (act instanceof SendToIP) sendActs.addAll(act.execute(time));
		for (Activity act : sendActs)
			if (act instanceof ProcessQuote) act.execute(time);
	}

	private void addTransaction(Price p, int q, TimeStamp time) {
		addBid(p, q, time);
		addBid(p, -q, time);
		Collection<? extends Activity> clearActs = market.clear(time);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
		Collection<Activity> sendActs = new ArrayList<Activity>();
		for (Activity act : clearActs)
			if (act instanceof SendToIP) sendActs.addAll(act.execute(time));
		for (Activity act : sendActs)
			if (act instanceof ProcessQuote) act.execute(time);
	}
	
	private void executeAgentStrategy(Agent agent, TimeStamp time) {
		Collection<? extends Activity> test = agent.agentStrategy(time);

		// executing the bid submission - will go to the market
		for (Activity act : test) {
			if (act instanceof SubmitNMSBid) {
				act.execute(time);
			}
		}
	}
	
	private void assertCorrectBid(Agent agent,
			Price low, Price high, int quantity) {
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue("BidTreeSize is incorrect", bid.bidTreeSet.size() == 1);
		
		assertTrue("Bid agent is null", bid.getAgent() != null);
		assertTrue("Bid agent is incorrect", bid.getAgent().equals(agent));

		Price bidPrice = bid.bidTreeSet.first().getPrice();
		assertTrue("Bid price (" + bidPrice + ") less than " + low,
				bidPrice.greaterThan(low));
		assertTrue("Bid price (" + bidPrice + ") greater than " + high,
				bidPrice.lessThan(high));
		
		assertTrue("Quantity is incorrect",
				bid.bidTreeSet.first().getQuantity() == quantity);
	}
	
	private void assertCorrectBid(Agent agent,
			Price match, int quantity) {
		PQBid bid = (PQBid) market.getBids().get(agent);
		// Asserting the bid is correct
		assertTrue(bid.bidTreeSet.size() == 1);
		assertTrue(bid.getAgent() != null);
		assertTrue(bid.getAgent().equals(agent));
		assertTrue(bid.bidTreeSet.first().getPrice().equals(match));
		assertTrue(bid.bidTreeSet.first().getQuantity() == quantity);
	}

	@Test
	public void initialBuyer() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting buyer on empty market: Result should be price=0");
		// Creating a buyer
		AAAgent agent = addAgent(true);
		// Testing against an empty market
		executeAgentStrategy(agent, new TimeStamp(100));

		//Checking the bid
		assertCorrectBid(agent, Price.ZERO, 1);
	}

	@Test
	public void initialSeller() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting seller on empty market: Result should be price="
						+ Price.INF);
		// Creating a seller
		AAAgent agent = addAgent(false);
		// Testing against an empty market
		executeAgentStrategy(agent, new TimeStamp(100));

		assertCorrectBid(agent, Price.INF, -1);
	}

	@Test
	public void noTransactionsBuyer() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting buyer on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "50000 < Bid price < 100000");

		// Setting up the bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(200000), -1, new TimeStamp(10));

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = addAgent(true);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(65000);
		Price high = new Price(70000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void noTransactionsSeller() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting seller on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "100000 < Ask price < 200000");

		// Adding setup bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(200000), -1, new TimeStamp(10));

		// Creating the agent and running the test
		AAAgent agent = addAgent(false);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(165000);
		Price high = new Price(170000);
		assertCorrectBid(agent, low, high, -1);
	}

	@Test
	public void IntraBuyerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(75000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(-1);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(30000);
		Price high = new Price(35000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void IntraBuyerNegative() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(75000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(-0.5);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(36000);
		Price high = new Price(41000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void IntraBuyerActive() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing active buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(75000), 1, new TimeStamp(20));

		AAAgent agent = addAgent(true);
		Logger.log(Logger.Level.DEBUG, "Price ~= 58333");
		agent.setAggression(0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(57000);
		Price high = new Price(59000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void IntraBuyerPositive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(75000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(0.5);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(64000);
		Price high = new Price(66000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void IntraBuyerAggressive() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing aggressive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(75000), 1, new TimeStamp(20));

		AAAgent agent = addAgent(true);
		Logger.log(Logger.Level.DEBUG, "Price ~= 66667");
		agent.setAggression(1.0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(65000);
		Price high = new Price(68000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void IntraSellerPassive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing passive seller on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(125000), 1, new TimeStamp(20));

		// Testing the Agent
		AAAgent agent = addAgent(false);
		Logger.log(Logger.Level.DEBUG,
				"Price ~= " + (150000 + (Price.INF.getPrice() - 150000) / 3));
		agent.setAggression(-1.0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(
				150000 + (Price.INF.getPrice() - 150000) / 3 - 1000);
		Price high = new Price(
				150000 + (Price.INF.getPrice() - 150000) / 3 + 1000);
		assertCorrectBid(agent, low, high, -1);
	}

	@Test
	public void IntraSellerActive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG,
				"\nTesting active seller on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(125000), 1, new TimeStamp(20));

		AAAgent agent = addAgent(false);
		log(Logger.Level.DEBUG, "Price ~= 141667");
		agent.setAggression(0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(138000);
		Price high = new Price(144000);
		assertCorrectBid(agent, low, high, -1);
	}

	@Test
	public void IntraSellerAggressive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing aggressive seller on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(125000), 1, new TimeStamp(20));

		AAAgent agent = addAgent(false);
		agent.setAggression(1);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(130000);
		Price high = new Price(135000);
		assertCorrectBid(agent, low, high, -1);
	}

	
	@Test
	public void ExtraBuyerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(125000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(-1);
		TimeStamp time = new TimeStamp(100);
		executeAgentStrategy(agent, time);
		
		// Asserting the bid is correct
		Price low = new Price(30000);
		Price high = new Price(35000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void ExtraBuyerNegative() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(125000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(-0.5);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(38000);
		Price high = new Price(41000);
		assertCorrectBid(agent, low, high, 1);
	}
	
	@Test
	public void ExtraBuyerActive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = 0 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(125000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(0);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(65000);
		Price high = new Price(70000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void ExtraBuyerPositive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = 0 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(125000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(0.5);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, new TimeStamp(100));

		// Asserting the bid is correct
		Price low = new Price(65000);
		Price high = new Price(70000);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void ExtraSellerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(125000), 1, new TimeStamp(15));

		// Setting up the agent
		AAAgent agent = addAgent(false);
		agent.setAggression(-1);
		TimeStamp time = new TimeStamp(100);
		executeAgentStrategy(agent, time);
		
		// Asserting the bid is correct
		Price low = new Price(95000).plus(Price.INF.times(1.0/3.0));
		Price high = new Price(105000).plus(Price.INF.times(1.0/3.0));
		assertCorrectBid(agent, low, high, -1);
	}

	
	
	@Test
	public void AggressionLearning() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG, "Testing aggression learning");

		// Adding Bids and Transactions
		addBid(new Price(50000), 1, new TimeStamp(10));
		addBid(new Price(150000), -1, new TimeStamp(10));
		addTransaction(new Price(100000), 1, new TimeStamp(20));
		addTransaction(new Price(100000), 1, new TimeStamp(21));
		addTransaction(new Price(100000), 1, new TimeStamp(22));
		addTransaction(new Price(100000), 1, new TimeStamp(23));
		addTransaction(new Price(100000), 1, new TimeStamp(24));

		AAAgent agent = addAgent(false);
		TimeStamp ts = new TimeStamp(100);
		Collection<Activity> test = agent.agentStrategy(ts);
		System.out.println(agent.getAggression());
		// Finding the bid in the activity list
		for (Activity itr : test) {
			if (SubmitNMSBid.class.isAssignableFrom(itr.getClass())) {
				// Casting the bid
				// SubmitNMSBid bid = (SubmitNMSBid) itr;
				// System.out.println(bid.getPrice());
			}
		}
		assertTrue(agent.getAggression() < 0);
	}
	
}
