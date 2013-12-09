package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Logger.log;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Keys;
import activity.Activity;
import activity.SubmitNMSOrder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class AAAgentTest {

	private static Random rand;
	private static EntityProperties agentProperties;

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;

	@BeforeClass
	public static void setupClass() {
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "AAAgentTest.log"));

		// Creating the setup properties
		rand = new Random(1);

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

	private AAAgent addAgent(OrderType type) {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.BUYER_STATUS, type.equals(OrderType.BUY));
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);	// private values all 0
		
		AAAgent agent = new AAAgent(new TimeStamp(0), fundamental, sip, market, rand,
				testProps);

		return agent;
	}

	private void addOrder(OrderType type, int price, int quantity, int time) {
		TimeStamp currentTime = new TimeStamp(time);
		// creating a dummy agent
		MockBackgroundAgent agent = new MockBackgroundAgent(fundamental, sip, market);
		// Having the agent submit a bid to the market
		executeImmediateActivities(market.submitOrder(agent, type,
				new Price(price), quantity, currentTime), currentTime);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
		
	}

	private void addTransaction(int p, int q, int time) {
		addOrder(BUY, p, q, time);
		addOrder(SELL, p, q, time);
		TimeStamp currentTime = new TimeStamp(time);
		executeImmediateActivities(market.clear(currentTime), currentTime);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
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
		assertNotEquals("Num orders is incorrect", 0, orders.size());
		Order order = Iterables.getFirst(orders, null);

		assertNotEquals("Order agent is null", null, order.getAgent());
		assertEquals("Order agent is incorrect", agent, order.getAgent());

		Price bidPrice = order.getPrice();
		assertTrue("Order price (" + bidPrice + ") less than " + low,
				bidPrice.greaterThan(new Price(low)));
		assertTrue("Order price (" + bidPrice + ") greater than " + high,
				bidPrice.lessThan(new Price(high)));

		assertEquals("Quantity is incorrect", quantity, order.getQuantity());
	}
	
	private void assertCorrectBidQuantity(Agent agent, int quantity) {
		assertCorrectBid(agent, 0, Integer.MAX_VALUE, quantity);
	}

	@Test
	public void initialBuyer() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting buyer on empty market: Result should be price=0");
		// Creating a buyer
		AAAgent agent = addAgent(OrderType.BUY);
		// Testing against an empty market
		executeAgentStrategy(agent, 100);

		//Checking the bid
		assertCorrectBidQuantity(agent, 1);
	}

	@Test
	public void initialSeller() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting seller on empty market: Result should be price="
						+ Price.INF);
		// Creating a seller
		AAAgent agent = addAgent(OrderType.SELL);
		// Testing against an empty market
		executeAgentStrategy(agent, 100);

		assertCorrectBidQuantity(agent, 1);
	}

	@Test
	public void noTransactionsBuyer() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting buyer on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "50000 < Bid price < 100000");

		// Setting up the bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 200000, 1, 10);

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 200000, 1, 10);

		// Creating the agent and running the test
		AAAgent agent = addAgent(OrderType.SELL);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 165000, 170000, 1);
	}

	@Test
	public void IntraBuyerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 15);
		addTransaction(75000, 1, 20);

		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 20);

		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 20);

		// Testing the Agent
		AAAgent agent = addAgent(OrderType.SELL);
		Logger.log(Logger.Level.DEBUG,
				"Price ~= " + (150000 + (Price.INF.intValue() - 150000) / 3));
		agent.setAggression(-1.0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		int low = 150000 + (Price.INF.intValue() - 150000) / 3 - 1000;
		int high = 150000 + (Price.INF.intValue() - 150000) / 3 + 1000;
		assertCorrectBid(agent, low, high, 1);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 15);
		addTransaction(125000, 1, 20);

		AAAgent agent = addAgent(OrderType.SELL);
		log(Logger.Level.DEBUG, "Price ~= 141667");
		agent.setAggression(0);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 138000, 144000, 1);
	}

	@Test
	public void IntraSellerAggressive() { // Check Aggression
		Logger.log(Logger.Level.DEBUG, "");
		Logger.log(Logger.Level.DEBUG,
				"Testing aggressive seller on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 20);

		AAAgent agent = addAgent(OrderType.SELL);
		agent.setAggression(1);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct
		assertCorrectBid(agent, 130000, 135000, 1);
	}

	
	@Test
	public void ExtraBuyerPassive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(125000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.BUY);
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
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.SELL);
		agent.setAggression(-1);
		executeAgentStrategy(agent, 100);
		
		// Asserting the bid is correct
		int low = 95000 + (int) (Price.INF.doubleValue() / 3);
		int high = 105000 + (int) (Price.INF.doubleValue() / 3);
		assertCorrectBid(agent, low, high, 1);
	}

	@Test
	public void ExtraSellerActive() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting passive buyer on market with transactions");

		// Adding Transactions and Bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(75000, 1, 15);

		// Setting up the agent
		AAAgent agent = addAgent(OrderType.SELL);
		agent.setAggression(0);
		executeAgentStrategy(agent, 100);
		
		// Asserting the bid is correct
		assertCorrectBid(agent, 132000, 135000, 1);
	}

	@Test
	public void BuyerAggressionIncrease() {
		Logger.log(Logger.Level.DEBUG, "\nTesting aggression learning");

		// Adding Bids and Transactions
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(95000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(100000, 1, 30);
		addTransaction(100000, 1, 35);
		addTransaction(105000, 1, 40);

		AAAgent agent = addAgent(OrderType.BUY);
		executeAgentStrategy(agent, 100);
		assertTrue(agent.getAggression() > 0);
		assertCorrectBid(agent, 50000, 100000, 1);
	}
	
	@Test
	public void SellerAggressionIncrease() {
		Logger.log(Logger.Level.DEBUG, "\nTesting aggression learning");

		// Adding Bids and Transactions
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(100000, 1, 30);
		addTransaction(100000, 1, 35);
		addTransaction(95000, 1, 40);

		AAAgent agent = addAgent(OrderType.SELL);
		executeAgentStrategy(agent, 100);
		assertTrue(agent.getAggression() > 0);
		assertCorrectBid(agent, 100000, 150000, 1);
	}
	
	private void executeImmediateActivities(Iterable<? extends Activity> acts, TimeStamp time) {
		ArrayList<Activity> queue = Lists.newArrayList(filterNonImmediateAndReverse(acts));
		while (!queue.isEmpty()) {
			Activity a = queue.get(queue.size() - 1);
			queue.remove(queue.size() - 1);
			queue.addAll(filterNonImmediateAndReverse(a.execute(time)));
		}
	}
	
	private Collection<? extends Activity> filterNonImmediateAndReverse(Iterable<? extends Activity> acts) {
		ArrayList<Activity> array = Lists.newArrayList();
		for (Activity a : acts)
			if (a.getTime() == TimeStamp.IMMEDIATE)
				array.add(a);
		Collections.reverse(array);
		return array;
	}
	
	// TODO testing of effects of varying parameters
}
