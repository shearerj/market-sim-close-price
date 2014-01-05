package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.Activity;
import activity.SubmitNMSOrder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import systemmanager.Consts;
import systemmanager.Keys;
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

public class ZIPAgentTest {

	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	private static EntityProperties agentProperties;
	
	@BeforeClass
	public static void setUpClass(){
		// Setting up the log file
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "ZIPAgentTest.log"));

		// Creating the setup properties
		rand = new Random(1);
		
		// Setting up agentProperties
		agentProperties = new EntityProperties();
		agentProperties.put(Keys.REENTRY_RATE, 0.05);
		agentProperties.put(Keys.MAX_QUANTITY, 10);
		agentProperties.put(Keys.MARGIN_MIN, 0.05);
		agentProperties.put(Keys.MARGIN_MAX, 0.35);
		agentProperties.put(Keys.GAMMA_MIN, 0);
		agentProperties.put(Keys.GAMMA_MAX, 0.1);
		agentProperties.put(Keys.BETA_MIN, 0.1);
		agentProperties.put(Keys.BETA_MAX, 0.5);
		agentProperties.put(Keys.COEFF_A, 0.05);
		agentProperties.put(Keys.COEFF_R, 0.05);
		agentProperties.put(Keys.PRIVATE_VALUE_VAR, 0); // set as 0 for easier testing
	}
	
	@Before
	public void setup(){
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}
	

	@Test
	public void initialMarginTest() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MARGIN_MIN, 0.35);
		ZIPAgent agent = new ZIPAgent(TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		// if no transactions, margin should be initialized to properties setting
		Margin margin = agent.margin;
		assertEquals(10, margin.getMaxAbsPosition());
		int i = 0;
		for (Double value : margin.values) {
			if (i < 10)
				assertEquals(new Double(0.35), value);
			else
				assertEquals(new Double(-0.35), value);
				// buyer margins are negative
			i++;
		}
	}
	
	@Test
	public void marginRangeTest() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MARGIN_MIN, 0.25);
		ZIPAgent agent = new ZIPAgent(TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		// if no transactions, margin should be initialized to properties setting
		Margin margin = agent.margin;
		assertEquals(10, margin.getMaxAbsPosition());
		for (Double value : margin.values) {
			assertTrue(Math.abs(value) <= 0.35 && Math.abs(value) >= 0.25);
		}
	}
	
	@Test
	public void initialZIP() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.4);
		
		ZIPAgent agent = new ZIPAgent(TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		// verify beta in correct range
		assertTrue(agent.beta <= 0.5 && agent.beta >= 0.4);
		assertTrue(0 == agent.momentumChange);
		assertNull(agent.lastPrice);
	}
	
	@Test
	public void computeRTest() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.COEFF_R, 0.1);
		
		ZIPAgent agent = new ZIPAgent(TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		double testR = agent.computeRCoefficient(true);
		assertTrue("Increasing R outside correct range",
				testR >= 1 && testR <= 1.1);
		testR = agent.computeRCoefficient(false);
		assertTrue("Decreasing R outside correct range",
				testR >= 0.9 && testR <= 1);
	}
	
	@Test
	public void computeATest() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.COEFF_A, 0.1);
		
		ZIPAgent agent = new ZIPAgent(TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		double testA = agent.computeACoefficient(true);
		assertTrue("Increasing R outside correct range",
				testA >= 0 && testA <= 0.1);
		testA = agent.computeACoefficient(false);
		assertTrue("Decreasing R outside correct range",
				testA >= -0.1 && testA <= 0);
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			computeRTest();
			computeATest();
		}
	}
	
	@Test
	public void computeDeltaTest() {
		fail();
		// needs computeTargetPrice
	}
	
	@Test
	public void updateMomentumTest() {
		fail();
		// needs computeDelta
	}
	

	@Test
	public void getCurrentMarginTest() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 1);
		testProps.put(Keys.MARGIN_MAX, 1.5);
		testProps.put(Keys.MARGIN_MIN, 1.2);
		
		ZIPAgent agent = new ZIPAgent(TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		
		

	}
	
	
	@Test
	public void updateMarginTest() {
		// need to initialize some transactions
		fail();
	}

	
	public void computeOrderPrice() {
		// what if no change in transaction price? how does computed order price change?
		fail();
	}

	
	@Test
	public void computeTargetPriceTest() {
		fail();
		// needs check Increasing
	}
	
	@Test
	public void checkIncreasingTest() {
		// TODO need to figure out what best to do for check increasing
		fail();
	}
		
	public void agentStrategyTest() {
		// what happens if zero transactions
		// should execute NMS order as in ZI strategy
		fail();
	}
	
	
	
//	// Asserting that margin updated correctly
//	if (isBuyer) {
//		if (lastTransPrice.lessThanEqual(lastPrice)) 
//			assert Math.abs(oldMargin) <= Math.abs(margin); // raise margin
//		else
//			assert Math.abs(oldMargin) >= Math.abs(margin); // lower margin
//	} else {
//		if (lastTransPrice.greaterThanEqual(lastPrice))
//			assert Math.abs(oldMargin) <= Math.abs(margin); // raise margin
//		else
//			assert Math.abs(oldMargin) >= Math.abs(margin); // lower margin
//	}

	
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

	private void executeImmediateActivities(Iterable<? extends Activity> acts, TimeStamp time) {
		// FIXME Change this to use EventManager
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
}


