package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkSingleOrderRange;
import static utils.Tests.j;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Transaction;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class ZIPAgentTest {

	private static final Random rand = new Random();
	private static final double eps = 1e-6;
	private static final Props defaults = Props.fromPairs(
			Keys.REENTRY_RATE, 0.05,
			Keys.MAX_QUANTITY, 10,
			Keys.MARGIN_MIN, 0.05,
			Keys.MARGIN_MAX, 0.35,
			Keys.GAMMA_MIN, 0,
			Keys.GAMMA_MAX, 0.1,
			Keys.BETA_MIN, 0.1,
			Keys.BETA_MAX, 0.5,
			Keys.COEFF_A, 0.05,
			Keys.COEFF_R, 0.05,
			Keys.PRIVATE_VALUE_VAR, 0);
	
	private MockSim sim;
	private Market market;
	private MarketView view;
	private Agent mockAgent;
	
	@Before
	public void setup() throws IOException{
		sim = MockSim.create(getClass(),
				Log.Level.NO_LOGGING, Keys.FUNDAMENTAL_MEAN,
				100000, Keys.FUNDAMENTAL_SHOCK_VAR,
				0, MarketType.CDA, j.join(Keys.NUM_MARKETS, 1));
		market = Iterables.getOnlyElement(sim.getMarkets());
		view = market.getPrimaryView();
		mockAgent = mockAgent();
	}

	@Test
	public void initialMarginTest() {
		ZIPAgent agent = zipAgent(Keys.MARGIN_MIN, 0.35);

		// if no transactions, margin should be initialized to properties setting
		Margin margin = agent.margin;
		assertEquals(10, margin.getMaxAbsPosition());
		for (double value : margin.values.subList(0, 10))
			assertEquals(0.35, value, eps);
		for (double value : margin.values.subList(10, margin.values.size()))
			assertEquals(-0.35, value, eps);
	}
	
	@Test
	public void marginRangeTest() {
		ZIPAgent agent = zipAgent(Keys.MARGIN_MIN, 0.25);

		// if no transactions, margin should be initialized to properties setting
		Margin margin = agent.margin;
		assertEquals(10, margin.getMaxAbsPosition());
		Range<Double> range = Range.closed(0.25, 0.35);
		for (double value : margin.values.subList(0, 10))
			assertTrue(range.contains(value));
		for (double value : margin.values.subList(10, margin.values.size()))
			assertTrue(range.contains(-value));
	}
	
	@Test
	public void initialZIP() {
		ZIPAgent agent = zipAgent(Keys.BETA_MAX, 0.5, Keys.BETA_MIN, 0.4);

		// verify beta in correct range
		assertTrue(Range.closed(0.4, 0.5).contains(agent.beta));
		assertEquals(0, agent.momentumChange, eps);
		assertNull(agent.limitPrice);
	}
	
	@Test
	public void computeRTest() {
		ZIPAgent agent = zipAgent(Keys.COEFF_R, 0.1);

		double testR = agent.computeRCoefficient(true);
		assertTrue("Increasing R outside correct range",
				testR >= 1 && testR <= 1.1);
		testR = agent.computeRCoefficient(false);
		assertTrue("Decreasing R outside correct range",
				testR >= 0.9 && testR <= 1);
	}
	
	@Test
	public void computeATest() {
		ZIPAgent agent = zipAgent(Keys.COEFF_A, 0.1);

		double testA = agent.computeACoefficient(true);
		assertTrue("Increasing R outside correct range",
				testA >= 0 && testA <= 0.1);
		testA = agent.computeACoefficient(false);
		assertTrue("Decreasing R outside correct range",
				testA >= -0.1 && testA <= 0);
	}
	
	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i < 100; i++) {
			setup();
			computeRTest();
			setup();
			computeATest();
			setup();
			computeTargetPriceTest();
			setup();
			computeDeltaTest();
			setup();
			updateMomentumAdvancedTest();
		}
	}
	
	@Test
	public void agentStrategyTest() throws IOException {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 1,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5,
				Keys.GAMMA_MAX, 0.5,
				Keys.GAMMA_MIN, 0.5,
				Keys.COEFF_A, 0.3,
				Keys.COEFF_R, 0.25,
				Keys.MARGIN_MAX, 0.05,
				Keys.MARGIN_MIN, 0.05);

		// now with a dummy transaction and dummy order prices
		addTransaction(Price.of(95000), 1);
		addTransaction(Price.of(90000), 1);
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// set the margins
		agent.type = SELL;
		agent.lastOrderPrice = Price.of(105000);
		agent.lastOrderPrice = Price.of(95000);

		/* 
		 * This current test is based off the random seed. Currently this means that
		 * Type: BUY
		 * Trans1: R 0.96, A -0.18
		 * Trans2: R 0.79, A -0.07
		 */
		agent.rand.setSeed(7221);
		agent.agentStrategy();
		// buyer reduces margins because transaction prices are less than order
		// prices, submitted order price will be below the last order price
		// should also be below the most recent transaction price
		assertEquals("Incorrect random seed", BUY, Iterables.getOnlyElement(agent.activeOrders).getOrderType());
		checkSingleOrderRange(agent.activeOrders, Price.of(85000), Price.of(95000), 1);

		assertEquals(Price.of(88953), Iterables.getOnlyElement(agent.activeOrders).getPrice());
	}
	
	@Test
	public void getCurrentMarginTest() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 1,
				Keys.MARGIN_MAX, 1.5,
				Keys.MARGIN_MIN, 1.2);

		// FIXME verify buyer margin within [-1, 0]
		assertEquals(-1.0, agent.getCurrentMargin(0, BUY), eps);
		
		// check seller margin
		assertTrue("Current margin outside range", Range.closed(1.2, 1.5).contains(agent.getCurrentMargin(0, BUY)));
	}
	
	@Test
	public void updateMarginZeroLimit() {
		sim.log(INFO, "Testing margin update when limit price is 0");
		// testing when limit price is 0
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5,
				Keys.GAMMA_MAX, 0.5,
				Keys.GAMMA_MIN, 0.5,
				Keys.COEFF_A, 0.3,
				Keys.COEFF_R, 0.25,
				Keys.MARGIN_MAX, 0.05,
				Keys.MARGIN_MIN, 0.05);

		// add dummy transaction
		addTransaction(Price.of(95000), 1);
		Transaction firstTrans = Iterables.getOnlyElement(view.getTransactions());

		agent.limitPrice = Price.ZERO;
		
		// set the margins
		agent.type = BUY;
		agent.lastOrderPrice = Price.of(99000);
		
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type), eps);
		
		agent.updateMargin(firstTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, oldMargin, eps);
	}
	
	@Test
	public void checkIncreaseMarginBuyer() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5,
				Keys.GAMMA_MAX, 0.5,
				Keys.GAMMA_MIN, 0.5,
				Keys.COEFF_A, 0.3,
				Keys.COEFF_R, 0.25,
				Keys.MARGIN_MAX, 0.05,
				Keys.MARGIN_MIN, 0.05);
		agent.rand.setSeed(1); // FIXME Seed set for some reason
		
		// add dummy transaction
		Transaction firstTrans = addTransaction(Price.of(99000), 1);
		Transaction lastTrans = addTransaction(Price.of(80000), 1);
		Price lastTransPrice = lastTrans.getPrice();
		
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// verify limit price is constant 100000
		assertEquals(Price.of(100000), agent.limitPrice);
		
		// set the margins
		agent.type = BUY;
		agent.lastOrderPrice = Price.of(99000);
		Price lastOrderPrice = agent.lastOrderPrice;
		agent.updateMargin(firstTrans);
		
		// decrease target price; for buyer, means higher margin
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type), 0.001);
		agent.updateMargin(lastTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(0, agent.type), 0.001);
		checkMarginUpdate(lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}
	
	@Test
	public void checkIncreaseMarginSeller() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5,
				Keys.GAMMA_MAX, 0.5,
				Keys.GAMMA_MIN, 0.5,
				Keys.COEFF_A, 0.3,
				Keys.COEFF_R, 0.25,
				Keys.MARGIN_MAX, 0.05,
				Keys.MARGIN_MIN, 0.05);
		agent.rand.setSeed(1);

		// add dummy transaction
		Transaction firstTrans = addTransaction(Price.of(105000), 1);
		Transaction lastTrans = addTransaction(Price.of(110000), 1);
		Price lastTransPrice = lastTrans.getPrice();
		
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// verify limit price is constant 100000
		assertEquals(Price.of(100000), agent.limitPrice);
		
		// set the margins
		agent.type = SELL;
		agent.lastOrderPrice = Price.of(105000);
		Price lastOrderPrice = agent.lastOrderPrice;
		agent.updateMargin(firstTrans);
		
		// increase target price; for seller, means higher margin
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type), 0.001);
		agent.updateMargin(lastTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(0, agent.type), 0.001);
		checkMarginUpdate(lastOrderPrice, lastTransPrice, oldMargin, newMargin);	
	}

	@Test
	public void checkDecreaseMarginBuyer() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5,
				Keys.GAMMA_MAX, 0.5,
				Keys.GAMMA_MIN, 0.5,
				Keys.COEFF_A, 0.3,
				Keys.COEFF_R, 0.25,
				Keys.MARGIN_MAX, 0.05,
				Keys.MARGIN_MIN, 0.05);
		agent.rand.setSeed(1);
		
		assertEquals(0.5, agent.beta, 0);
		assertEquals(agent.momentumChange, 0, 0);
		
		// add dummy transaction
		Transaction firstTrans = addTransaction(Price.of(90000), 1);
		Transaction lastTrans = addTransaction(Price.of(105000), 1);
		Price lastTransPrice = lastTrans.getPrice();
		
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// verify limit price is constant 100000
		assertEquals(Price.of(100000), agent.limitPrice);
		
		// set the margins
		agent.type = BUY;
		agent.lastOrderPrice = Price.of(90000);
		Price lastOrderPrice = agent.lastOrderPrice;
		agent.updateMargin(firstTrans);
		
		// decrease target price; for buyer, means higher margin
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type), 0.001);
		agent.updateMargin(lastTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(0, agent.type), 0.001);
		checkMarginUpdate(lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}
	
	@Test
	public void checkDecreaseMarginSeller() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5,
				Keys.GAMMA_MAX, 0.5,
				Keys.GAMMA_MIN, 0.5,
				Keys.COEFF_A, 0.3,
				Keys.COEFF_R, 0.25,
				Keys.MARGIN_MAX, 0.05,
				Keys.MARGIN_MIN, 0.05);
		agent.rand.setSeed(1);
		
		assertEquals(0.5, agent.beta, 0);
		assertEquals(0, agent.momentumChange, 0);
		
		// add dummy transaction
		Transaction firstTrans = addTransaction(Price.of(101000), 1);
		Transaction lastTrans = addTransaction(Price.of(110000), 1);
		Price lastTransPrice = lastTrans.getPrice();
		
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// verify limit price is constant 100000
		assertEquals(Price.of(100000), agent.limitPrice);
		
		// set the margins
		agent.type = SELL;
		agent.lastOrderPrice = Price.of(101000);
		Price lastOrderPrice = agent.lastOrderPrice;
		agent.updateMargin(firstTrans);
		
		// decrease target price; for buyer, means higher margin
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(0, agent.type), 0.001);
		agent.updateMargin(lastTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(0, agent.type), 0.001);
		checkMarginUpdate(lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}

	@Test
	public void computeOrderPrice() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.MARGIN_MAX, 0.35,
				Keys.MARGIN_MIN, 0.25);
		
		// test for buy
		agent.type = BUY;
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// verify limit price is constant 100000
		assertEquals(Price.of(100000), agent.limitPrice);
		double currentMargin = agent.margin.getValue(0, agent.type);
		assertEquals(Price.of(100000 * (1+currentMargin)), 
				agent.computeOrderPrice(currentMargin));
		
		// test for sell
		agent.type = BUY;
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// verify limit price is constant 100000
		assertEquals(Price.of(100000), agent.limitPrice);
		currentMargin = agent.margin.getValue(0, agent.type);
		assertEquals(Price.of(100000 * (1+currentMargin)), 
				agent.computeOrderPrice(currentMargin));
	}

	@Test
	public void updateMomentumBasicTest() {
		// gamma fixed at 1
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5,
				Keys.GAMMA_MAX, 1,
				Keys.GAMMA_MIN, 1);

		assertEquals(0.5, agent.beta, 0);
		assertEquals(0, agent.momentumChange, 0);
		
		// add dummy transaction
		Transaction transaction = addTransaction(Price.of(100000), 1);
		
		// change initial momentum
		agent.momentumChange = 10;
		
		// increase target price
		agent.lastOrderPrice = Price.of(99000);
		agent.type = BUY;
		agent.updateMomentumChange(transaction);
		assertTrue(10 == agent.momentumChange);
		agent.lastOrderPrice = Price.of(99000);
		agent.type = SELL;
		agent.updateMomentumChange(transaction);
		assertTrue(10 == agent.momentumChange);
		
		// decrease target price
		agent.lastOrderPrice = Price.of(110000);
		agent.type = BUY;
		agent.updateMomentumChange(transaction);
		assertTrue(10 == agent.momentumChange);
		agent.lastOrderPrice = Price.of(110000);
		agent.type = SELL;
		agent.updateMomentumChange(transaction);
		assertTrue(10 == agent.momentumChange);
	}

	@Test
	public void updateMomentumAdvancedTest() {
		// gamma fixed at 1, update entirely to delta
		ZIPAgent agent = zipAgent(Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5,
				Keys.GAMMA_MAX, 0,
				Keys.GAMMA_MIN, 0);

		assertEquals(0.5, agent.beta, 0);
		assertEquals(0, agent.momentumChange, 0);
		
		// add dummy transaction
		Transaction transaction = addTransaction(Price.of(100000), 1);
		
		// increase target price
		agent.lastOrderPrice = Price.of(99000);
		agent.type = BUY;
		agent.updateMomentumChange(transaction);
		assertTrue(agent.momentumChange > 0);
		assertTrue(agent.momentumChange < 0.5 * 99000);
		agent.lastOrderPrice = Price.of(99000);
		agent.type = SELL;
		agent.updateMomentumChange(transaction);
		assertTrue(agent.momentumChange > 0);
		assertTrue(agent.momentumChange < 0.5 * 99000);
		
		// decrease target price
		agent.lastOrderPrice = Price.of(110000);
		agent.type = BUY;
		agent.updateMomentumChange(transaction);
		assertTrue(agent.momentumChange < 0);
		assertTrue(-agent.momentumChange < 0.5 * 110000);
		agent.lastOrderPrice = Price.of(110000);
		agent.type = SELL;
		agent.updateMomentumChange(transaction);
		assertTrue(agent.momentumChange < 0);
		assertTrue(-agent.momentumChange < 0.5 * 110000);
	}

	
	@Test
	public void computeDeltaTest() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.BETA_MAX, 0.5,
				Keys.BETA_MIN, 0.5);
		
		assertEquals(0.5, agent.beta, 0);
		
		// add dummy transaction
		Transaction transaction = addTransaction(Price.of(100000), 1);

		// increase target price, so delta positive & delta < 0.5*trans price
		agent.lastOrderPrice = Price.of(99000);
		agent.type = BUY;
		double delta = agent.computeDelta(transaction);
		assertTrue(delta > 0);
		assertTrue(delta < 0.5 * 99000);

		agent.lastOrderPrice = Price.of(99000);
		agent.type = SELL;
		delta = agent.computeDelta(transaction);
		assertTrue(delta > 0);
		assertTrue(delta < 0.5 * 99000);
		
		// decrease target price, so delta negative & |delta| < 0.5*trans price
		agent.lastOrderPrice = Price.of(110000);
		agent.type = BUY;
		delta = agent.computeDelta(transaction);
		assertTrue(delta < 0);
		assertTrue(Math.abs(delta) < 0.5 * 110000);

		agent.lastOrderPrice = Price.of(110000);
		agent.type = SELL;
		delta = agent.computeDelta(transaction);
		assertTrue(delta < 0);
		assertTrue(Math.abs(delta) < 0.5 * 110000);
	}

	
	@Test
	public void computeTargetPriceTest() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.MARGIN_MAX, 0.35,
				Keys.MARGIN_MIN, 0.25);

		// add dummy transaction
		Transaction transaction = addTransaction(Price.of(100000), 1);
		Price transactionPrice = transaction.getPrice();
		
		// set order prices
		// for increase target price, R & A will cause target price to exceed last trans
		// buyer wants to decrease margin and therefore increase target price
		agent.lastOrderPrice = Price.of(99000);
		agent.type = BUY;
		assertTrue(agent.computeTargetPrice(transaction).greaterThan(transactionPrice));
		// seller wants to increase margin and therefore increase target price
		agent.lastOrderPrice = Price.of(99000);
		agent.type = SELL;
		assertTrue(agent.computeTargetPrice(transaction).greaterThan(transactionPrice));		
		
		// for decrease target price, R & A will cause target price to be less than last trans
		// buyer wants to increase margin and therefore decrease target price
		agent.lastOrderPrice = Price.of(110000);
		agent.type = BUY;
		assertTrue(agent.computeTargetPrice(transaction).lessThan(transactionPrice));
		// seller wants to decrease margin and therefore decrease target price
		agent.lastOrderPrice = Price.of(110000);
		agent.type = SELL;
		assertTrue(agent.computeTargetPrice(transaction).lessThan(transactionPrice));		
	}
	
	@Test
	public void checkIncreaseMarginInitialTest() {
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.MARGIN_MAX, 0.35,
				Keys.MARGIN_MIN, 0.25);
		
		// check that initial order price null
		assertNull(agent.lastOrderPrice);
		
		// now test with a dummy transaction
		Transaction transaction = addTransaction(Price.of(100000), 1);

		agent.type = BUY;
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// verify limit price is constant 100000
		assertEquals(Price.of(100000), agent.limitPrice);
		double currentMargin = agent.margin.getValue(0, agent.type);
		assertEquals(Price.of(100000 * (1+currentMargin)), 
				agent.computeOrderPrice(currentMargin));
		// check that last trans of 100000 must be greater than order price
		// since window, assume order price is submitted before the window
		// therefore buyer should not increase margin
		assertFalse(agent.checkIncreaseMargin(transaction));
		
		// check for sell
		agent.lastOrderPrice = null;
		agent.type = SELL;
		currentMargin = agent.margin.getValue(0, agent.type);
		assertEquals(Price.of(100000 * (1+currentMargin)), 
				agent.computeOrderPrice(currentMargin));
		// last trans price of 100000 must be less than the sell order price
		// since always sell above limit price; seller should not increase margin
		assertFalse(agent.checkIncreaseMargin(transaction));
	}
	
	@Test
	public void advancedIncreaseMarginTest() {
		// test with other order prices already set
		ZIPAgent agent = zipAgent(
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 0,
				Keys.MARGIN_MAX, 0.35,
				Keys.MARGIN_MIN, 0.25);
		
		// now test with a dummy transaction
		Transaction transaction = addTransaction(Price.of(100000), 1);
		
		// set order prices
		agent.lastOrderPrice = Price.of(99000);
		assertEquals(Price.of(99000), agent.lastOrderPrice);
		
		// buyer order price < trans price, therefore no increase
		agent.type = BUY;
		assertFalse(agent.checkIncreaseMargin(transaction));
		// seller order price < trans price, therefore increase
		agent.type = SELL;
		assertTrue(agent.checkIncreaseMargin(transaction));


		// different order prices
		agent.lastOrderPrice = Price.of(110000);
		// buyer order price > trans price, therefore increase
		agent.type = BUY;
		assertTrue(agent.checkIncreaseMargin(transaction));
		// seller order price > trans price, therefore no increase
		agent.type = SELL;
		assertFalse(agent.checkIncreaseMargin(transaction));

	}
	
	/** Check margin updating correctly */
	private static void checkMarginUpdate(Price lastPrice, Price lastTransPrice, double oldMargin, double newMargin) {
		assertEquals(lastPrice.compareTo(lastTransPrice), Double.compare(oldMargin, newMargin));
	}
	
	
	private void addOrder(OrderType type, Price price, int quantity) {
		mockAgent.submitOrder(view, type, price, quantity);
		sim.executeImmediate();
	}

	private Transaction addTransaction(Price price, int quantity) {
		addOrder(BUY, price, quantity);
		addOrder(SELL, price, quantity);
		return checkNotNull(Iterables.getFirst(view.getTransactions(), null));
	}

	private ZIPAgent zipAgent(Object... parameters) {
		return ZIPAgent.create(sim, TimeStamp.ZERO, market, new Random(rand.nextLong()),
				Props.withDefaults(defaults, parameters));
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}

}


