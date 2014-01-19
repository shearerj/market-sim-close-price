package entity.agent;

import static fourheap.Order.OrderType.*;
import static logger.Logger.log;
import static org.junit.Assert.*;

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
import systemmanager.EventManager;
import systemmanager.Keys;
import utils.Rands;
import activity.Activity;
import activity.SubmitNMSOrder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.agent.AAAgent.Aggression;
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
		agentProperties.put(Keys.REENTRY_RATE, 0);
		agentProperties.put(Keys.MAX_QUANTITY, 10);
		agentProperties.put(Keys.ETA, 3);
		agentProperties.put(Keys.WITHDRAW_ORDERS, false);
		agentProperties.put(Keys.WINDOW_LENGTH, 5000);
		agentProperties.put(Keys.AGGRESSION, 0);
		agentProperties.put(Keys.THETA, 0);
		agentProperties.put(Keys.THETA_MIN, -4);
		agentProperties.put(Keys.THETA_MAX, 4);
		agentProperties.put(Keys.NUM_HISTORICAL, 5);
		agentProperties.put(Keys.ETA, 3);
		agentProperties.put(Keys.LAMBDA_R, 0.05);
		agentProperties.put(Keys.LAMBDA_A, 0.02);	// x ticks/$ for Eq 10/11 
		agentProperties.put(Keys.GAMMA, 2);
		agentProperties.put(Keys.BETA_R, 0.4); 
		agentProperties.put(Keys.BETA_T, 0.4); 

		agentProperties.put(Keys.DEBUG, true);
	}

	@Before
	public void setupTest() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(sip);
	}

	@Test
	public void tauChange() {
		double r = 0.5;
		AAAgent agent = addAgent(OrderType.BUY);
		agent.theta = 2;

		assertEquals(0.268, agent.tauChange(r), 0.001);
	}

	/**
	 * Computation of moving average for estimating the equilibrium price.
	 */
	@Test
	public void estimateEquilibrium() {
		TimeStamp time = TimeStamp.ZERO;
		
		// Creating a buyer
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.NUM_HISTORICAL, 3);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 5E7);
		AAAgent agent = addAgent(BUY, testProps);
		assertNull(agent.estimateEquilibrium(agent.getWindowTransactions(time)));
		
		// Adding Transactions and Bids
		addTransaction(75000, 1, 20);
		addTransaction(90000, 1, 25);

		// not enough transactions, need to use only 2
		double[] weights = {1/1.9, 0.9/1.9};
		assertEquals(Math.round(weights[0]*75000 + weights[1]*90000),
				agent.estimateEquilibrium(agent.getWindowTransactions(time)).intValue());
		
		// sufficient transactions
		addTransaction(100000, 1, 25);
		double total = 2.71;
		double[] weights2 = {1/total, 0.9/total, 0.81/total};
		assertEquals(Math.round(weights2[0]*75000 + weights2[1]*90000 + weights2[2]*100000),
				agent.estimateEquilibrium(agent.getWindowTransactions(time)).intValue());
	}
	
	@Test
	public void computeRShoutBuyer() {
		AAAgent buyer = addAgent(BUY);
		buyer.theta = -2;
		
		Price limit = new Price(110000);
		Price last = new Price(105000);
		Price equil = new Price(105000);
		
		// Intramarginal (limit > equil)
		assertEquals(0, buyer.computeRShout(limit, last, equil), 0.001);
		last = new Price(100000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		last = new Price(109000);	// more aggressive (lower margin)
		assertEquals(1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		
		// Extramarginal
		equil = new Price(111000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		last = limit;
		assertEquals(0, buyer.computeRShout(limit, last, equil), 0.001);
	}
	
	@Test
	public void computeRShoutSeller() {
		AAAgent seller = addAgent(SELL);
		seller.theta = -2;

		Price limit = new Price(105000);
		Price last = new Price(110000);
		Price equil = new Price(110000);
		
		// Intramarginal (limit < equil)
		assertEquals(0, seller.computeRShout(limit, last, equil), 0.001);
		last = new Price(111000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);
		last = new Price(109000);	// more aggressive (lower margin)
		assertEquals(1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);

		// Extramarginal
		equil = new Price(104000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);
		last = limit;
		assertEquals(0, seller.computeRShout(limit, last, equil), 0.001);
	}

	@Test
	public void determineTargetPriceBuyer() {
		AAAgent buyer = addAgent(BUY);
		buyer.theta = 2;
		
		Price limit = new Price(110000);
		Price equil = new Price(105000);
		
		// Intramarginal (limit > equil)
		buyer.aggression = -1;		// passive
		assertEquals(Price.ZERO, buyer.determineTargetPrice(limit, equil));
		buyer.aggression = -0.5;	// less aggressive, so lower target than equil
		assertTrue(buyer.determineTargetPrice(limit, equil).lessThan(equil));
		buyer.aggression = 0;		// active
		assertEquals(equil, buyer.determineTargetPrice(limit, equil));
		buyer.aggression = 0.5;		// aggressive, so target exceeds equil
		assertTrue(buyer.determineTargetPrice(limit, equil).greaterThan(equil));
		buyer.aggression = 1;		// most aggressive
		assertEquals(limit, buyer.determineTargetPrice(limit, equil));
		
		// Extramarginal
		limit = new Price(104000);
		buyer.aggression = -1;		// passive
		assertEquals(Price.ZERO, buyer.determineTargetPrice(limit, equil));
		buyer.aggression = -0.5;	// less aggressive, so lower target than equil
		assertTrue(buyer.determineTargetPrice(limit, equil).lessThan(equil));
		buyer.aggression = 0.5;		// aggressiveness capped at 0
		assertEquals(limit, buyer.determineTargetPrice(limit, equil));
	}
	
	@Test
	public void determineTargetPriceSeller() {
		AAAgent seller = addAgent(SELL);
		seller.theta = 2;
		
		Price limit = new Price(105000);
		Price equil = new Price(110000);
		
		// Intramarginal (limit < equil)
		seller.aggression = -1;		// passive
		assertEquals(Price.INF, seller.determineTargetPrice(limit, equil));
		seller.aggression = -0.5;	// less aggressive, so target exceeds equil
		assertTrue(seller.determineTargetPrice(limit, equil).greaterThan(equil));
		seller.aggression = 0;		// active
		assertEquals(equil, seller.determineTargetPrice(limit, equil));
		seller.aggression = 0.5;	// aggressive, so target less than equil
		assertTrue(seller.determineTargetPrice(limit, equil).lessThan(equil));
		seller.aggression = 1;		// most aggressive
		assertEquals(limit, seller.determineTargetPrice(limit, equil));
		
		// Extramarginal
		limit = new Price(111000);
		seller.aggression = -1;		// passive
		assertEquals(Price.INF, seller.determineTargetPrice(limit, equil));
		seller.aggression = -0.5;	// less aggressive, so lower target than equil
		assertTrue(seller.determineTargetPrice(limit, equil).greaterThan(equil));
		seller.aggression = 0.5;	// aggressiveness capped at 0
		assertEquals(limit, seller.determineTargetPrice(limit, equil));
	}
	
	@Test
	public void biddingLayerBuyer() {
		TimeStamp time = TimeStamp.ZERO;
		EventManager em = new EventManager(rand);
		
		Price limit = new Price(145000);
		Price target = new Price(175000);
		
		AAAgent buyer = addAgent(BUY);
		
		Iterable<? extends Activity> acts = buyer.biddingLayer(limit, target, 1, time);
		assertEquals(1, Iterables.size(acts));
		assertTrue(Iterables.getOnlyElement(acts) instanceof SubmitNMSOrder); // ZI strat
		
		addOrder(BUY, 150000, 1, 10);
		addOrder(SELL, 200000, 1, 10);
		
		buyer.positionBalance = buyer.privateValue.getMaxAbsPosition() + 1;
		acts = buyer.biddingLayer(limit, target, 1, TimeStamp.create(20));
		assertEquals(0, Iterables.size(acts));	// would exceed max position
		
		buyer.positionBalance = 0;
		acts = buyer.biddingLayer(limit, target, 1, TimeStamp.create(20));
		assertEquals(0, Iterables.size(acts));	// limit price < bid
		
		limit = new Price(211000);
		acts = buyer.biddingLayer(limit, null, 1, TimeStamp.create(20));
		em.addActivity(Iterables.getOnlyElement(acts));
		em.executeUntil(new TimeStamp(21));
		assertCorrectBid(buyer, 170007, 1);
		
		limit = new Price(210000);
		target = new Price(180000);
		buyer.withdrawAllOrders(TimeStamp.create(20));
		acts = buyer.biddingLayer(limit, target, 1, TimeStamp.create(20));
		em.addActivity(Iterables.getOnlyElement(acts));
		em.executeUntil(new TimeStamp(21));
		assertCorrectBid(buyer, 160000, 1);
	}
	
	@Test
	public void biddingLayerSeller() {
		TimeStamp time = TimeStamp.ZERO;
		EventManager em = new EventManager(rand);
		
		Price limit = new Price(210000);
		Price target = new Price(175000);
		
		AAAgent seller = addAgent(SELL);
		
		Iterable<? extends Activity> acts = seller.biddingLayer(limit, target, 1, time);
		assertEquals(1, Iterables.size(acts));
		assertTrue(Iterables.getOnlyElement(acts) instanceof SubmitNMSOrder); // ZI strat
		
		addOrder(BUY, 150000, 1, 10);
		addOrder(SELL, 200000, 1, 10);
		
		seller.positionBalance = seller.privateValue.getMaxAbsPosition() + 1;
		acts = seller.biddingLayer(limit, target, 1, TimeStamp.create(20));
		assertEquals(0, Iterables.size(acts));	// would exceed max position
		
		seller.positionBalance = 0;
		acts = seller.biddingLayer(limit, target, 1, TimeStamp.create(20));
		assertEquals(0, Iterables.size(acts));	// limit price > ask
		
		limit = new Price(170000);
		acts = seller.biddingLayer(limit, null, 1, TimeStamp.create(20));
		em.addActivity(Iterables.getOnlyElement(acts));
		em.executeUntil(new TimeStamp(21));
		assertCorrectBid(seller, 190000, 1);
		
		limit = new Price(165000);
		target = new Price(170000);
		seller.withdrawAllOrders(TimeStamp.create(20));
		acts = seller.biddingLayer(limit, target, 1, TimeStamp.create(20));
		em.addActivity(Iterables.getOnlyElement(acts));
		em.executeUntil(new TimeStamp(21));
		assertCorrectBid(seller, 190000, 1);
	}
	
	@Test
	public void updateTheta() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties props = new EntityProperties();
		props.put(Keys.NUM_HISTORICAL, 5);
		props.put(Keys.THETA_MAX, 8);
		props.put(Keys.THETA_MIN, -8);
		props.put(Keys.BETA_T, 0.25);
		props.put(Keys.GAMMA, 2);
		AAAgent agent = addAgent(SELL, props);
		agent.theta = -4;
		
		agent.updateTheta(null, agent.getWindowTransactions(time));
		assertEquals(-4, agent.theta, 0.001);
		
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(100000, 1, 30);
		
		Price equil = new Price(100000);
		// haven't used 5 transactions yet, so keep theta fixed
		agent.updateTheta(equil, agent.getWindowTransactions(new TimeStamp(20)));
		assertEquals(-4, agent.theta, 0.001);
		
		addTransaction(110000, 1, 35);
		addTransaction(111000, 1, 40);
		agent.updateTheta(equil, agent.getWindowTransactions(new TimeStamp(40)));
		assertTrue(agent.theta < -4);	// decreases because high price vol
		assertEquals(-5, agent.theta, 0.001); 
		
		addTransaction(106000, 1, 45);
		addTransaction(107000, 1, 50);
		addTransaction(108000, 1, 50);
		equil = new Price(108000);
		agent.updateTheta(equil, agent.getWindowTransactions(new TimeStamp(50)));
		assertTrue(agent.theta > -5);	// increases because lower price vol
	}
	
	@Test
	public void testAggressionDataStructure() {
		Aggression agg = new Aggression();
		assertEquals(0, agg.getMaxAbsPosition());
		assertEquals(0, agg.values.size());
		
		agg = new Aggression(1, 0.5);
		assertEquals(2, agg.values.size());
		assertEquals(1, agg.getMaxAbsPosition());
		assertEquals(0.5, agg.values.get(0), 0.001);
		assertEquals(0.5, agg.values.get(1), 0.001);
		
		agg = new Aggression(2, 0.75);
		assertEquals(0.75, agg.getValue(0, BUY), 0.001);
		assertEquals(0.75, agg.getValue(-1, SELL), 0.001);
		assertEquals(new Double(0), agg.getValue(-2, SELL));
		assertEquals(new Double(0), agg.getValue(2, BUY));
		
		agg.setValue(0, BUY, 0.5);
		assertEquals(0.5, agg.getValue(0, BUY), 0.001);
		
		agg.setValue(-2, SELL, -0.5);
		// still 0 since outside max position
		assertEquals(0, agg.getValue(-2, SELL), 0.001);
	}
	
	@Test
	public void initialBuyer() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting buyer on empty market: Result should be price=0");
		// Creating a buyer
		AAAgent agent = addAgent(OrderType.BUY);
		assertTrue(agent.type.equals(BUY));
		// Testing against an empty market
		executeAgentStrategy(agent, 100);

		assertCorrectBidQuantity(agent, 1);
	}

	@Test
	public void initialSeller() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting seller on empty market: Result should be price="
						+ Price.INF);
		// Creating a seller
		AAAgent agent = addAgent(OrderType.SELL);
		assertTrue(agent.type.equals(SELL));
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
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.ETA, 4);
		AAAgent agent = addAgent(OrderType.BUY, testProps);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct (based on EQ 10/11)
		assertCorrectBid(agent, 62500, 1);
	}

	@Test
	public void noTransactionsSeller() {
		Logger.log(Logger.Level.DEBUG,
				"\nTesting seller on market with bids/asks but no transactions");
		Logger.log(Logger.Level.DEBUG, "100000 < Ask price < 200000");

		// Adding setup bids
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 200000, 1, 10);

		// Testing against a market with initial bids but no transaction history
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.ETA, 4);
		AAAgent agent = addAgent(OrderType.SELL, testProps);
		executeAgentStrategy(agent, 100);

		// Asserting the bid is correct (based on EQ 10/11)
		assertCorrectBid(agent, 175000, 1);
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
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, -3);
		AAAgent agent = addAgent(OrderType.BUY, testProps);
		agent.setAggression(-0.5);
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
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, -3);
		AAAgent agent = addAgent(OrderType.BUY, testProps);
		agent.setAggression(0.5);
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
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, -3);
		AAAgent agent = addAgent(OrderType.BUY, testProps);
		agent.setAggression(-0.5);
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
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, -3);
		AAAgent agent = addAgent(OrderType.BUY, testProps);
		agent.setAggression(0);
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
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, -3);
		AAAgent agent = addAgent(OrderType.BUY, testProps);
		agent.setAggression(0.5);
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
		assertTrue(agent.aggression > 0);
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
		assertTrue(agent.aggression > 0);
		assertCorrectBid(agent, 100000, 150000, 1);
	}

	/**
	 * Test short-term learning (EQ 7)
	 */
	@Test
	public void updateAggressionBuyer() {
		Logger.log(Logger.Level.DEBUG, "\nTesting aggression update (buyer)");

		// Adding Bids and Transactions
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 35);
		addTransaction(95000, 1, 40);

		// Setting up the agent
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, -3);
		AAAgent agent = addAgent(BUY, testProps);
		double oldAggression = 0.5;
		agent.setAggression(oldAggression);
		executeAgentStrategy(agent, 100);

		checkAggressionUpdate(BUY, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	/**
	 * Test short-term learning (EQ 7)
	 * Note that the value of theta affects whether or not this test will pass
	 * (e.g. -3 causes a NaN in computeRShout)
	 */
	@Test
	public void updateAggressionSeller() {
		Logger.log(Logger.Level.DEBUG, "\nTesting aggression update (seller)");

		// Adding Bids and Transactions
		addOrder(BUY, 50000, 1, 10);
		addOrder(SELL, 150000, 1, 10);
		addTransaction(105000, 1, 20);
		addTransaction(100000, 1, 25);
		addTransaction(110000, 1, 30);
		addTransaction(100000, 1, 35);
		addTransaction(95000, 1, 40);

		// Setting up the agent
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, 2);
		AAAgent agent = addAgent(SELL, testProps);
		double oldAggression = 0.2;
		agent.setAggression(oldAggression);
		executeAgentStrategy(agent, 100);

		checkAggressionUpdate(SELL, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	/**
	 * Note that for extramarginal buyer, must have last transaction price less
	 * than the limit otherwise rShout gets set to 0.
	 */
	@Test
	public void randomizedUpdateAggressionBuyer() {
		Logger.log(Logger.Level.DEBUG, "\nTesting aggression update (buyer)");

		// Adding Bids and Transactions
		addOrder(BUY, (int) Rands.nextUniform(rand, 25000, 75000), 1, 10);
		addOrder(SELL, (int) Rands.nextUniform(rand, 125000, 175000), 1, 10);
		addTransaction((int) Rands.nextUniform(rand, 100000, 110000), 1, 20);
		addTransaction((int) Rands.nextUniform(rand, 50000, 150000), 1, 25);
		addTransaction((int) Rands.nextUniform(rand, 100000, 120000), 1, 30);
		addTransaction((int) Rands.nextUniform(rand, 750000, 150000), 1, 35);
		addTransaction((int) Rands.nextUniform(rand, 80000, 100000), 1, 40);

		// Setting up the agent
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, -2);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 5E7);
		AAAgent agent = addAgent(BUY, testProps);
		double oldAggression = 0.5;
		agent.setAggression(oldAggression);
		executeAgentStrategy(agent, 100);

		checkAggressionUpdate(BUY, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	/**
	 * For intramarginal seller, want limit price less than equilibrium,
	 * otherwise rShout clipped at 0.
	 */
	@Test
	public void randomizedUpdateAggressionSeller() {
		Logger.log(Logger.Level.DEBUG, "\nTesting aggression update (seller)");

		// Adding Bids and Transactions
		addOrder(BUY, (int) Rands.nextUniform(rand, 25000, 75000), 1, 10);
		addOrder(SELL, (int) Rands.nextUniform(rand, 125000, 175000), 1, 10);
		addTransaction((int) Rands.nextUniform(rand, 100000, 110000), 1, 20);
		addTransaction((int) Rands.nextUniform(rand, 500000, 150000), 1, 25);
		addTransaction((int) Rands.nextUniform(rand, 500000, 120000), 1, 30);
		addTransaction((int) Rands.nextUniform(rand, 100000, 150000), 1, 35);
		addTransaction((int) Rands.nextUniform(rand, 100000, 110000), 1, 40);

		// Setting up the agent
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.THETA, -3);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 5E7);
		AAAgent agent = addAgent(SELL, testProps);
		double oldAggression = 0.2;
		agent.setAggression(oldAggression);
		executeAgentStrategy(agent, 100);

		checkAggressionUpdate(SELL, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setupTest();
			randomizedUpdateAggressionBuyer();
			setupTest();
			randomizedUpdateAggressionSeller();
		}
	}


	// Helper methods

	/**
	 * Check aggression updating
	 */
	private void checkAggressionUpdate(OrderType type, Price lastTransactionPrice,
			Price targetPrice, double oldAggression, double aggression) {

		// Asserting that aggression updated correctly
		if (type.equals(BUY)) {
			if (lastTransactionPrice.compareTo(targetPrice) < 0) 
				assertTrue("r_old " + oldAggression + " less than " + aggression,
						oldAggression >= aggression); // less aggressive
			else
				assertTrue("r_old " + oldAggression + " greater than " + aggression,
						oldAggression <= aggression); // more aggressive
		} else {
			if (lastTransactionPrice.compareTo(targetPrice) > 0)
				assertTrue("r_old " + oldAggression + " less than " + aggression,
						oldAggression >= aggression); // less aggressive
			else
				assertTrue("r_old " + oldAggression + " greater than " + aggression,
						oldAggression <= aggression); // more aggressive
		}
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

	private AAAgent addAgent(OrderType type) {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.BUYER_STATUS, type.equals(OrderType.BUY));
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);	// private values all 0

		return new AAAgent(new TimeStamp(0), fundamental, sip, market, rand,
				testProps);
	}

	private AAAgent addAgent(OrderType type, EntityProperties testProps) {
		testProps.put(Keys.BUYER_STATUS, type.equals(OrderType.BUY));
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);	// private values all 0
		return new AAAgent(new TimeStamp(0), fundamental, sip, market, rand,
				testProps);
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

	/**
	 * Note this method only works if there's only one order
	 * @param agent
	 * @param price
	 * @param quantity
	 */
	private void assertCorrectBid(Agent agent, int price, int quantity) {
		Collection<Order> orders = agent.activeOrders;
		// Asserting the bid is correct
		assertNotEquals("Num orders is incorrect", 0, orders.size());
		Order order = Iterables.getFirst(orders, null);

		assertNotEquals("Order agent is null", null, order.getAgent());
		assertEquals("Order agent is incorrect", agent, order.getAgent());

		Price bidPrice = order.getPrice();
		assertEquals("Price is incorrect", new Price(price), bidPrice);
		assertEquals("Quantity is incorrect", quantity, order.getQuantity());
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
		assertTrue("Order price (" + bidPrice + ") less than " + new Price(low),
				bidPrice.greaterThan(new Price(low)));
		assertTrue("Order price (" + bidPrice + ") greater than " + new Price(high),
				bidPrice.lessThan(new Price(high)));

		assertEquals("Quantity is incorrect", quantity, order.getQuantity());
	}

	private void assertCorrectBidQuantity(Agent agent, int quantity) {
		assertCorrectBid(agent, 0, Integer.MAX_VALUE, quantity);
	}
}
