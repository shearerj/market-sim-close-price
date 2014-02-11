package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import static org.junit.Assert.*;
import static logger.Log.Level.*;
import static logger.Log.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;

import com.google.common.collect.Iterables;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.Market;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class ZIPAgentTest {

	private Executor exec;
	private FundamentalValue fundamental = new MockFundamental(100000);
	private Market market;
	private SIP sip;
	private static Random rand;
	private static EntityProperties agentProperties;
	
	@BeforeClass
	public static void setupClass() throws IOException{
		// Setting up the log file
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "ZIPAgentTest.log"));

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
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		// Creating the MockMarket
		market = new MockMarket(exec, sip);
	}

	
	@Test
	public void initialMarginTest() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MARGIN_MIN, 0.35);
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
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
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
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
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		// verify beta in correct range
		assertTrue(agent.beta <= 0.5 && agent.beta >= 0.4);
		assertTrue(0 == agent.momentumChange);
		assertNull(agent.limitPrice);
	}
	
	@Test
	public void computeRTest() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.COEFF_R, 0.1);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
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
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
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
			computeTargetPriceTest();
			computeDeltaTest();
			updateMomentumAdvancedTest();
		}
	}

	
	@Test
	public void agentStrategyTest() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 1);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		testProps.put(Keys.GAMMA_MAX, 0.5);
		testProps.put(Keys.GAMMA_MIN, 0.5);
		testProps.put(Keys.COEFF_A, 0.3);
		testProps.put(Keys.COEFF_R, 0.25);
		testProps.put(Keys.MARGIN_MAX, 0.05);
		testProps.put(Keys.MARGIN_MIN, 0.05);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		
		// now with a dummy transaction and dummy order prices
		addTransaction(95000, 1, 0);
		addTransaction(90000, 1, 0);
		agent.limitPrice = agent.getLimitPrice(BUY, 1, time);
		// set the margins
		agent.type = SELL;
		agent.lastOrderPrice = new Price(105000);
		agent.lastOrderPrice = new Price(95000);
		
		agent.rand.setSeed(1234); // XXX Change seed to test specific execution
		agent.agentStrategy(time);		
		// buyer reduces margins because transaction prices are less than order
		// prices, submitted order price will be below the last order price
		// should also be below the most recent transaction price
		assertCorrectBid(agent, 85000, 95000, 1);
		
		// XXX This current test is based off the random seed. Currently this means that
		// Type: BUY
		// R, A: 0.81, -0.20
		// R, A: 0.89, -0.27
		assertEquals(88953, Iterables.getOnlyElement(agent.activeOrders).getPrice().intValue());
	}
	
	@Test
	public void getCurrentMarginTest() {
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 1);
		testProps.put(Keys.MARGIN_MAX, 1.5);
		testProps.put(Keys.MARGIN_MIN, 1.2);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		// verify buyer margin within [-1, 0]
		double currentMargin = agent.getCurrentMargin(0, BUY, TimeStamp.ZERO);
		assertEquals(new Double(-1.0), new Double(currentMargin));
		// check seller margin
		currentMargin = agent.getCurrentMargin(0, SELL, TimeStamp.ZERO);
		assertTrue("Current margin outside range", 
				currentMargin <= 1.5 && currentMargin >= 1.2);
	}
	
	@Test
	public void updateMarginZeroLimit() {
		log.log(INFO, "Testing margin update when limit price is 0");
		
		// testing when limit price is 0
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		testProps.put(Keys.GAMMA_MAX, 0.5);
		testProps.put(Keys.GAMMA_MIN, 0.5);
		testProps.put(Keys.COEFF_A, 0.3);
		testProps.put(Keys.COEFF_R, 0.25);
		testProps.put(Keys.MARGIN_MAX, 0.05);
		testProps.put(Keys.MARGIN_MIN, 0.05);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		
		// add dummy transaction
		addTransaction(95000, 1, 0);
		Transaction firstTrans = market.getTransactions().get(0);

		agent.limitPrice = Price.ZERO;
		
		// set the margins
		agent.type = BUY;
		agent.lastOrderPrice = new Price(99000);
		
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertTrue(oldMargin == agent.getCurrentMargin(0, agent.type, time));
		agent.updateMargin(firstTrans, time);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, oldMargin, 0.001);
	}
	
	@Test
	public void checkIncreaseMarginBuyer() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		testProps.put(Keys.GAMMA_MAX, 0.5);
		testProps.put(Keys.GAMMA_MIN, 0.5);
		testProps.put(Keys.COEFF_A, 0.3);
		testProps.put(Keys.COEFF_R, 0.25);
		testProps.put(Keys.MARGIN_MAX, 0.05);
		testProps.put(Keys.MARGIN_MIN, 0.05);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		// add dummy transaction
		addTransaction(95000, 1, 0);
		Transaction firstTrans = market.getTransactions().get(0);
		addTransaction(97000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(1);
		Price lastTransPrice = lastTrans.getPrice();
		
		agent.limitPrice = agent.getLimitPrice(BUY, 1, time);
		// verify limit price is constant 100000
		assertEquals(new Price(100000), agent.limitPrice);
		
		// set the margins
		agent.type = BUY;
		agent.lastOrderPrice = new Price(99000);
		Price lastOrderPrice = agent.lastOrderPrice;
		agent.updateMargin(firstTrans, time);
		
		// decrease target price; for buyer, means higher margin
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type, time), 0.001);
		agent.updateMargin(lastTrans, time);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(0, agent.type, time), 0.001);
		checkMarginUpdate(BUY, lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}
	
	@Test
	public void checkIncreaseMarginSeller() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		testProps.put(Keys.GAMMA_MAX, 0.5);
		testProps.put(Keys.GAMMA_MIN, 0.5);
		testProps.put(Keys.COEFF_A, 0.3);
		testProps.put(Keys.COEFF_R, 0.25);
		testProps.put(Keys.MARGIN_MAX, 0.05);
		testProps.put(Keys.MARGIN_MIN, 0.05);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		// add dummy transaction
		addTransaction(105000, 1, 0);
		Transaction firstTrans = market.getTransactions().get(0);
		addTransaction(110000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(1);
		Price lastTransPrice = lastTrans.getPrice();
		
		agent.limitPrice = agent.getLimitPrice(BUY, 1, time);
		// verify limit price is constant 100000
		assertEquals(new Price(100000), agent.limitPrice);
		
		// set the margins
		agent.type = SELL;
		agent.lastOrderPrice = new Price(105000);
		Price lastOrderPrice = agent.lastOrderPrice;
		agent.updateMargin(firstTrans, time);
		
		// increase target price; for seller, means higher margin
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type, time), 0.001);
		agent.updateMargin(lastTrans, time);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(0, agent.type, time), 0.001);
		checkMarginUpdate(SELL, lastOrderPrice, lastTransPrice, oldMargin, newMargin);	
	}

	@Test
	public void checkDecreaseMarginBuyer() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		testProps.put(Keys.GAMMA_MAX, 0.5);
		testProps.put(Keys.GAMMA_MIN, 0.5);
		testProps.put(Keys.COEFF_A, 0.3);
		testProps.put(Keys.COEFF_R, 0.25);
		testProps.put(Keys.MARGIN_MAX, 0.05);
		testProps.put(Keys.MARGIN_MIN, 0.05);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		assertEquals(new Double(0.5), new Double(agent.beta));
		assertTrue(0 == agent.momentumChange);
		
		// add dummy transaction
		addTransaction(95000, 1, 0);
		Transaction firstTrans = market.getTransactions().get(0);
		addTransaction(100000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(1);
		Price lastTransPrice = lastTrans.getPrice();
		
		agent.limitPrice = agent.getLimitPrice(BUY, 1, time);
		// verify limit price is constant 100000
		assertEquals(new Price(100000), agent.limitPrice);
		
		// set the margins
		agent.type = BUY;
		agent.lastOrderPrice = new Price(95000);
		Price lastOrderPrice = agent.lastOrderPrice;
		agent.updateMargin(firstTrans, time);
		
		// decrease target price; for buyer, means higher margin
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type, time), 0.001);
		agent.updateMargin(lastTrans, time);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(0, agent.type, time), 0.001);
		checkMarginUpdate(BUY, lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}
	
	@Test
	public void checkDecreaseMarginSeller() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		testProps.put(Keys.GAMMA_MAX, 0.5);
		testProps.put(Keys.GAMMA_MIN, 0.5);
		testProps.put(Keys.COEFF_A, 0.3);
		testProps.put(Keys.COEFF_R, 0.25);
		testProps.put(Keys.MARGIN_MAX, 0.05);
		testProps.put(Keys.MARGIN_MIN, 0.05);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		assertEquals(new Double(0.5), new Double(agent.beta));
		assertTrue(0 == agent.momentumChange);
		
		// add dummy transaction
		addTransaction(101000, 1, 0);
		Transaction firstTrans = market.getTransactions().get(0);
		addTransaction(110000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(1);
		Price lastTransPrice = lastTrans.getPrice();
		
		agent.limitPrice = agent.getLimitPrice(BUY, 1, time);
		// verify limit price is constant 100000
		assertEquals(new Price(100000), agent.limitPrice);
		
		// set the margins
		agent.type = SELL;
		agent.lastOrderPrice = new Price(101000);
		Price lastOrderPrice = agent.lastOrderPrice;
		agent.updateMargin(firstTrans, time);
		
		// decrease target price; for buyer, means higher margin
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type, time), 0.001);
		agent.updateMargin(lastTrans, time);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(0, agent.type, time), 0.001);
		checkMarginUpdate(BUY, lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}

	@Test
	public void computeOrderPrice() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.MARGIN_MAX, 0.35);
		testProps.put(Keys.MARGIN_MIN, 0.25);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		
		// test for buy
		agent.type = BUY;
		agent.limitPrice = agent.getLimitPrice(BUY, 1, time);
		// verify limit price is constant 100000
		assertEquals(new Price(100000), agent.limitPrice);
		double currentMargin = agent.margin.getValue(0, agent.type);
		assertEquals(new Price(100000 * (1+currentMargin)), 
				agent.computeOrderPrice(currentMargin, TimeStamp.ZERO));
		
		// test for sell
		agent.type = BUY;
		agent.limitPrice = agent.getLimitPrice(BUY, 1, time);
		// verify limit price is constant 100000
		assertEquals(new Price(100000), agent.limitPrice);
		currentMargin = agent.margin.getValue(0, agent.type);
		assertEquals(new Price(100000 * (1+currentMargin)), 
				agent.computeOrderPrice(currentMargin, TimeStamp.ZERO));
	}

	@Test
	public void updateMomentumBasicTest() {
		// gamma fixed at 1
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		testProps.put(Keys.GAMMA_MAX, 1);
		testProps.put(Keys.GAMMA_MIN, 1);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		assertEquals(new Double(0.5), new Double(agent.beta));
		assertTrue(0 == agent.momentumChange);
		
		// add dummy transaction
		addTransaction(100000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(0);
		
		// change initial momentum
		agent.momentumChange = 10;
		
		// increase target price
		agent.lastOrderPrice = new Price(99000);
		agent.type = BUY;
		agent.updateMomentumChange(lastTrans, time);
		assertTrue(10 == agent.momentumChange);
		agent.lastOrderPrice = new Price(99000);
		agent.type = SELL;
		agent.updateMomentumChange(lastTrans, time);
		assertTrue(10 == agent.momentumChange);
		
		// decrease target price
		agent.lastOrderPrice = new Price(110000);
		agent.type = BUY;
		agent.updateMomentumChange(lastTrans, time);
		assertTrue(10 == agent.momentumChange);
		agent.lastOrderPrice = new Price(110000);
		agent.type = SELL;
		agent.updateMomentumChange(lastTrans, time);
		assertTrue(10 == agent.momentumChange);
	}

	@Test
	public void updateMomentumAdvancedTest() {
		// gamma fixed at 1, update entirely to delta
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		testProps.put(Keys.GAMMA_MAX, 0);
		testProps.put(Keys.GAMMA_MIN, 0);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		assertEquals(new Double(0.5), new Double(agent.beta));
		assertTrue(0 == agent.momentumChange);
		
		// add dummy transaction
		addTransaction(100000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(0);
		
		// increase target price
		agent.lastOrderPrice = new Price(99000);
		agent.type = BUY;
		agent.updateMomentumChange(lastTrans, time);
		assertTrue(agent.momentumChange > 0);
		assertTrue(agent.momentumChange < 0.5 * 99000);
		agent.lastOrderPrice = new Price(99000);
		agent.type = SELL;
		agent.updateMomentumChange(lastTrans, time);
		assertTrue(agent.momentumChange > 0);
		assertTrue(agent.momentumChange < 0.5 * 99000);
		
		// decrease target price
		agent.lastOrderPrice = new Price(110000);
		agent.type = BUY;
		agent.updateMomentumChange(lastTrans, time);
		assertTrue(agent.momentumChange < 0);
		assertTrue(-agent.momentumChange < 0.5 * 110000);
		agent.lastOrderPrice = new Price(110000);
		agent.type = SELL;
		agent.updateMomentumChange(lastTrans, time);
		assertTrue(agent.momentumChange < 0);
		assertTrue(-agent.momentumChange < 0.5 * 110000);
	}

	
	@Test
	public void computeDeltaTest() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.BETA_MAX, 0.5);
		testProps.put(Keys.BETA_MIN, 0.5);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		assertEquals(new Double(0.5), new Double(agent.beta));
		
		// add dummy transaction
		addTransaction(100000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(0);

		// increase target price, so delta positive & delta < 0.5*trans price
		agent.lastOrderPrice = new Price(99000);
		agent.type = BUY;
		double delta = agent.computeDelta(lastTrans, time);
		assertTrue(delta > 0);
		assertTrue(delta < 0.5 * 99000);

		agent.lastOrderPrice = new Price(99000);
		agent.type = SELL;
		delta = agent.computeDelta(lastTrans, time);
		assertTrue(delta > 0);
		assertTrue(delta < 0.5 * 99000);
		
		// decrease target price, so delta negative & |delta| < 0.5*trans price
		agent.lastOrderPrice = new Price(110000);
		agent.type = BUY;
		delta = agent.computeDelta(lastTrans, time);
		assertTrue(delta < 0);
		assertTrue(Math.abs(delta) < 0.5 * 110000);

		agent.lastOrderPrice = new Price(110000);
		agent.type = SELL;
		delta = agent.computeDelta(lastTrans, time);
		assertTrue(delta < 0);
		assertTrue(Math.abs(delta) < 0.5 * 110000);
	}

	
	@Test
	public void computeTargetPriceTest() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.MARGIN_MAX, 0.35);
		testProps.put(Keys.MARGIN_MIN, 0.25);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);

		// add dummy transaction
		addTransaction(100000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(0);
		Price lastTransPrice = lastTrans.getPrice();
		
		// set order prices
		// for increase target price, R & A will cause target price to exceed last trans
		// buyer wants to decrease margin and therefore increase target price
		agent.lastOrderPrice = new Price(99000);
		agent.type = BUY;
		assertTrue(agent.computeTargetPrice(lastTrans, time).greaterThan(lastTransPrice));
		// seller wants to increase margin and therefore increase target price
		agent.lastOrderPrice = new Price(99000);
		agent.type = SELL;
		assertTrue(agent.computeTargetPrice(lastTrans, time).greaterThan(lastTransPrice));		
		
		// for decrease target price, R & A will cause target price to be less than last trans
		// buyer wants to increase margin and therefore decrease target price
		agent.lastOrderPrice = new Price(110000);
		agent.type = BUY;
		assertTrue(agent.computeTargetPrice(lastTrans, time).lessThan(lastTransPrice));
		// seller wants to decrease margin and therefore decrease target price
		agent.lastOrderPrice = new Price(110000);
		agent.type = SELL;
		assertTrue(agent.computeTargetPrice(lastTrans, time).lessThan(lastTransPrice));		
	}
	
	@Test
	public void checkIncreaseMarginInitialTest() {
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.MARGIN_MAX, 0.35);
		testProps.put(Keys.MARGIN_MIN, 0.25);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		
		// check that initial order price null
		assertNull(agent.lastOrderPrice);
		
		// now test with a dummy transaction
		addTransaction(100000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(0);
		agent.type = BUY;
		agent.limitPrice = agent.getLimitPrice(BUY, 1, time);
		// verify limit price is constant 100000
		assertEquals(new Price(100000), agent.limitPrice);
		double currentMargin = agent.margin.getValue(0, agent.type);
		assertEquals(new Price(100000 * (1+currentMargin)), 
				agent.computeOrderPrice(currentMargin, TimeStamp.ZERO));
		// check that last trans of 100000 must be greater than order price
		// since window, assume order price is submitted before the window
		// therefore buyer should not increase margin
		assertFalse(agent.checkIncreaseMargin(lastTrans, TimeStamp.ZERO));
		
		// check for sell
		agent.lastOrderPrice = null;
		agent.type = SELL;
		currentMargin = agent.margin.getValue(0, agent.type);
		assertEquals(new Price(100000 * (1+currentMargin)), 
				agent.computeOrderPrice(currentMargin, TimeStamp.ZERO));
		// last trans price of 100000 must be less than the sell order price
		// since always sell above limit price; seller should not increase margin
		assertFalse(agent.checkIncreaseMargin(lastTrans, TimeStamp.ZERO));
	}
	
	@Test
	public void advancedIncreaseMarginTest() {
		// test with other order prices already set
		TimeStamp time = TimeStamp.ZERO;
		EntityProperties testProps = new EntityProperties(agentProperties);
		testProps.put(Keys.MAX_QUANTITY, 5);
		testProps.put(Keys.PRIVATE_VALUE_VAR, 0);
		testProps.put(Keys.MARGIN_MAX, 0.35);
		testProps.put(Keys.MARGIN_MIN, 0.25);
		
		ZIPAgent agent = new ZIPAgent(exec, TimeStamp.ZERO, fundamental, sip, market, rand,
				testProps);
		
		// now test with a dummy transaction
		agent.positionBalance = 0;
		addTransaction(100000, 1, 0);
		Transaction lastTrans = market.getTransactions().get(0);
		// set order prices
		agent.lastOrderPrice = new Price(99000);
		assertEquals(new Price(99000), agent.lastOrderPrice);
		
		// buyer order price < trans price, therefore no increase
		agent.type = BUY;
		assertFalse(agent.checkIncreaseMargin(lastTrans, time));
		// seller order price < trans price, therefore increase
		agent.type = SELL;
		assertTrue(agent.checkIncreaseMargin(lastTrans, time));


		// different order prices
		agent.lastOrderPrice = new Price(110000);
		// buyer order price > trans price, therefore increase
		agent.type = BUY;
		assertTrue(agent.checkIncreaseMargin(lastTrans, time));
		// seller order price > trans price, therefore no increase
		agent.type = SELL;
		assertFalse(agent.checkIncreaseMargin(lastTrans, time));

	}
	
	
	// Helper methods
	
	/**
	 * Check margin updating
	 */
	private void checkMarginUpdate(OrderType type, Price lastPrice, 
			Price lastTransPrice, double oldMargin, double newMargin) {
		
		// Asserting that margin updated correctly
		if (type == BUY) {
			if (lastTransPrice.lessThanEqual(lastPrice)) 
				assertTrue(oldMargin >= newMargin); // raise margin (more negative)
			else
				assertTrue(oldMargin <= newMargin); // lower margin
		}
		if (type == SELL) {
			if (lastTransPrice.greaterThanEqual(lastPrice))
				assertTrue(oldMargin <= newMargin); // raise margin (more positive)
			else
				assertTrue(oldMargin >= newMargin); // lower margin
		}
	}
	
	
	private void addOrder(OrderType type, int price, int quantity, int time) {
		TimeStamp currentTime = TimeStamp.create(time);
		// creating a dummy agent
		MockBackgroundAgent agent = new MockBackgroundAgent(exec, fundamental, sip, market);
		// Having the agent submit a bid to the market
		market.submitOrder(agent, type, new Price(price), quantity, currentTime);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
		
	}

	private void addTransaction(int p, int q, int time) {
		addOrder(BUY, p, q, time);
		addOrder(SELL, p, q, time);
		TimeStamp currentTime = TimeStamp.create(time);
		market.clear(currentTime);

		// Added this so that the SIP would updated with the transactions, so expecting knowledge of
		// the transaction would work
	}
		
	private void assertCorrectBid(Agent agent, int low, int high,
			int quantity) {
		Collection<Order> orders = agent.activeOrders;
		// Asserting the bid is correct
		assertNotEquals("Num orders is incorrect", 0, orders.size());
		Order order = Iterables.getOnlyElement(orders);
		assertEquals("Order agent is incorrect", agent, order.getAgent());

		Price bidPrice = order.getPrice();
		assertTrue("Order price (" + bidPrice + ") less than " + low,
				bidPrice.greaterThan(new Price(low)));
		assertTrue("Order price (" + bidPrice + ") greater than " + high,
				bidPrice.lessThan(new Price(high)));

		assertEquals("Quantity is incorrect", quantity, order.getQuantity());
	}

}


