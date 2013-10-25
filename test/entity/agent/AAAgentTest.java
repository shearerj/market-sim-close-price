package entity.agent;

import static logger.Logger.log;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Keys;
import activity.Activity;
import activity.ProcessQuote;
import activity.SendToIP;
import activity.SubmitNMSOrder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;

import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

public class AAAgentTest {

	private static Random rand;
	private static EntityProperties agentProperties;

	private FundamentalValue fundamental = new DummyFundamental(100000);
	private Market market;
	private SIP sip;

	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, new File("simulations/unit_testing/AAAgentTest.log"));

		// Creating the setup properties
		rand = new Random(1);
//		fund = new DummyFundamental(100000);

		// Setting up agentProperties
		agentProperties = new EntityProperties();
		agentProperties.put(Keys.REENTRY_RATE, 0.25);
		agentProperties.put(Keys.MAX_QUANTITY, 10);
		agentProperties.put(Keys.DEBUG, false);
		agentProperties.put(Keys.ETA, 3);
		agentProperties.put(Keys.HISTORICAL, 5);
		agentProperties.put(Keys.AGGRESSION, 0);
		agentProperties.put(Keys.THETA, 0);
		agentProperties.put(Keys.THETA_MAX, 4);
		agentProperties.put(Keys.THETA_MIN, -4);
	}

	@Before
	public void setupTest() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}

	private AAAgent addAgent(boolean isBuyer) {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.BUYER_STATUS, isBuyer);

		AAAgent agent = new AAAgent(new TimeStamp(0), fundamental, sip, market, rand,
				testProps);

		return agent;
	}

	private void addBid(int price, int quantity,
			int time) {
		TimeStamp currentTime = new TimeStamp(time);
		// creating a dummy agent
		MockAgent agent = new MockAgent(fundamental, sip, market);
		// Having the agent submit a bid to the market
		Iterable<? extends Activity> bidActs = market.submitOrder(agent, new Price(price),
				quantity, currentTime);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
		Builder<Activity> sendActs = ImmutableList.builder();
		for (Activity act : bidActs)
			if (act instanceof SendToIP) sendActs.addAll(act.execute(currentTime));
		for (Activity act : sendActs.build())
			if (act instanceof ProcessQuote) act.execute(currentTime);
	}

	private void addTransaction(int p, int q, int time) {
		addBid(p, q, time);
		addBid(p, -q, time);
		TimeStamp currentTime = new TimeStamp(time);
		Iterable<? extends Activity> clearActs = market.clear(currentTime);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
		Builder<Activity> sendActs = ImmutableList.builder();
		for (Activity act : clearActs)
			if (act instanceof SendToIP) sendActs.addAll(act.execute(currentTime));
		for (Activity act : sendActs.build())
			if (act instanceof ProcessQuote) act.execute(currentTime);
	}
	
	private void executeAgentStrategy(Agent agent, int time) {
		TimeStamp currentTime = new TimeStamp(time);
		Iterable<? extends Activity> test = agent.agentStrategy(currentTime);

		// executing the bid submission - will go to the market
		for (Activity act : test)
			if (act instanceof SubmitNMSOrder)
				act.execute(currentTime);
	}
	
	private void assertCorrectBid(Agent agent, int low, int high,
			int quantity) {
		Collection<Order> orders = agent.activeOrders;
		// Asserting the bid is correct
		assertTrue("OrderSize is incorrect", !orders.isEmpty());
		Order order = Iterables.getFirst(orders, null);

		assertTrue("Order agent is null", order.getAgent() != null);
		assertTrue("Order agent is incorrect", order.getAgent().equals(agent));

		Price bidPrice = order.getPrice();
		assertTrue("Order price (" + bidPrice + ") less than " + low,
				bidPrice.greaterThan(new Price(low)));
		assertTrue("Order price (" + bidPrice + ") greater than " + high,
				bidPrice.lessThan(new Price(high)));

		assertTrue("Quantity is incorrect", order.getQuantity() == quantity);
	}
	
	private void assertCorrectBid(Agent agent, int quantity) {
		assertCorrectBid(agent, -1, Integer.MAX_VALUE, quantity);
	}

	@Test
	public void initialBuyer() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting buyer on empty market: Result should be price=0");
		// Creating a buyer
		AAAgent agent = addAgent(true);
		// Testing against asn empty market
		executeAgentStrategy(agent, 100);

		//Checking the bid
		assertCorrectBid(agent, 1);
	}

	@Test
	public void initialSeller() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting seller on empty market: Result should be price="
						+ Price.INF);
		// Creating a seller
		AAAgent agent = addAgent(false);
		// Testing against an empty market
		executeAgentStrategy(agent, 100);

		assertCorrectBid(agent, -1);
	}

	@Test
	public void noTransactionsBuyer() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting buyer on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "50000 < Bid price < 100000");

		// Setting up the bids
		addBid(50000, 1, 10);
		addBid(200000, -1, 10);

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = addAgent(true);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 65000, 70000, 1);
	}

	@Test
	public void noTransactionsSeller() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting seller on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "100000 < Ask price < 200000");

		// Adding setup bids
		addBid(50000, 1, 10);
		addBid(200000, -1, 10);

		// Creating the agent and running the test
		AAAgent agent = addAgent(false);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 165000, 170000, -1);
	}

	@Test
	public void IntraBuyerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(-1);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 30000, 35000, 1);
	}

	@Test
	public void IntraBuyerNegative() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(-0.5);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 36000, 41000, 1);
	}

	/**
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraBuyerActive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting active buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 15);
		addTransaction(75000, 1, 20);

		AAAgent agent = addAgent(true);
		Logger.log(Logger.Level.DEBUG, "Price ~= 58333");
		agent.setAggression(0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 57000, 59000, 1);
	}

	/**
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraBuyerPositive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(0.5);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 64000, 66000, 1);
	}

	@Test
	public void IntraBuyerAggressive() {
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing aggressive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(75000, 1, 20);

		AAAgent agent = addAgent(true);
		Logger.log(Logger.Level.DEBUG, "Price ~= 66667");
		agent.setAggression(1.0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 65000, 68000, 1);
	}

	@Test
	public void IntraSellerPassive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing passive seller on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(125000, 1, 20);

		// Testing the Agent
		AAAgent agent = addAgent(false);
		Logger.log(Logger.Level.DEBUG,
				"Price ~= " + (150000 + (Price.INF.intValue() - 150000) / 3));
		agent.setAggression(-1.0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		int low = 150000 + (Price.INF.intValue() - 150000) / 3 - 1000;
		int high = 150000 + (Price.INF.intValue() - 150000) / 3 + 1000;
		assertCorrectBid(agent, low, high, -1);
	}

	/**
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraSellerActive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting active seller on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 15);
		addTransaction(125000, 1, 20);

		AAAgent agent = addAgent(false);
		log(Logger.Level.DEBUG, "Price ~= 141667");
		agent.setAggression(0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 138000, 144000, -1);
	}

	@Test
	public void IntraSellerAggressive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing aggressive seller on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(125000, 1, 20);

		AAAgent agent = addAgent(false);
		agent.setAggression(1);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 130000, 135000, -1);
	}

	
	@Test
	public void ExtraBuyerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(-1);
		executeAgentStrategy(agent, 100);
		
		// Asserting the bid is correct
		assertCorrectBid(agent, 30000, 35000, 1);
	}

	@Test
	public void ExtraBuyerNegative() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = -0.5 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(-0.5);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 38000, 41000, 1);
	}
	
	@Test
	public void ExtraBuyerActive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = 0 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(0);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 65000, 70000, 1);
	}

	@Test
	public void ExtraBuyerPositive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting r = 0 buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(true);
		agent.setAggression(0.5);
		agent.setAdaptivness(-3.0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 65000, 70000, 1);
	}

	@Test
	public void ExtraSellerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(false);
		agent.setAggression(-1);
		executeAgentStrategy(agent, 100);
		
		// Asserting the bid is correct
		int low = 95000 + (int) (Price.INF.doubleValue() / 3);
		int high = 105000 + (int) (Price.INF.doubleValue() / 3);
		assertCorrectBid(agent, low, high, -1);
	}

	@Test
	public void ExtraSellerActive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(false);
		agent.setAggression(0);
		executeAgentStrategy(agent, 100);
		
		// Asserting the bid is correct
		assertCorrectBid(agent, 132000, 135000, -1);
	}

	@Test
	public void BuyerAggressionIncrease() {
		Logger.log(Logger.Level.DEBUG, "\nTesting aggression learning");

		// Adding Bids and Transactions
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(95000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(100000, 1, 30);
		addTransaction(100000, 1, 35);
		addTransaction(105000, 1, 40);

		AAAgent agent = addAgent(true);
		executeAgentStrategy(agent, 100);
		assertCorrectBid(agent, 50000, 100000, 1);
		assertTrue(agent.getAggression() > 0);
	}
	
	@Test
	public void SellerAggressionIncrease() {
		Logger.log(Logger.Level.DEBUG, "\nTesting aggression learning");

		// Adding Bids and Transactions
		addBid(50000, 1, 10);
		addBid(150000, -1, 10);
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(100000, 1, 30);
		addTransaction(100000, 1, 35);
		addTransaction(95000, 1, 40);

		AAAgent agent = addAgent(false);
		executeAgentStrategy(agent, 100);
		assertCorrectBid(agent, 100000, 150000, -1);
		assertTrue(agent.getAggression() > 0);
	}
	
}
