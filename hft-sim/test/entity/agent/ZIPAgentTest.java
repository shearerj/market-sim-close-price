package entity.agent;

import static com.google.common.base.Preconditions.checkNotNull;
import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.BetaMax;
import systemmanager.Keys.BetaMin;
import systemmanager.Keys.GammaMax;
import systemmanager.Keys.GammaMin;
import systemmanager.Keys.MarginMax;
import systemmanager.Keys.MarginMin;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.RangeA;
import systemmanager.Keys.RangeR;
import utils.Mock;
import utils.Rand;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

import data.FundamentalValue;
import data.Props;
import entity.agent.position.Margin;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Transaction;
import fourheap.Order.OrderType;

public class ZIPAgentTest {

	private static final Rand rand = Rand.create();
	private static final double eps = 1e-6;
	private static final Agent mockAgent = Mock.agent();
	private static final Props defaults = Props.fromPairs(
			ArrivalRate.class,		0d,
			PrivateValueVar.class,	0d);
	
	private FundamentalValue fundamental = Mock.fundamental(100000);
	private Market market;
	private MarketView view;
	
	@Before
	public void setup() {
		market = CDAMarket.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		view = market.getPrimaryView();
	}

	@Test
	public void initialMarginTest() {
		ZIPAgent agent = zipAgent(Props.fromPairs(MarginMin.class, 0.35));

		// if no transactions, margin should be initialized to properties setting
		Margin margin = agent.margin;
		assertEquals(10, margin.getMaxAbsPosition());
		for (int position = 0; position > -10; position--)
			assertEquals(0.35, margin.getValue(position, SELL), eps);
		for (int position = 0; position < 10; position++)
			assertEquals(-0.35, margin.getValue(position, BUY), eps);
	}
	
	@Test
	public void marginRangeTest() {
		ZIPAgent agent = zipAgent(Props.fromPairs(MarginMin.class, 0.25));

		// if no transactions, margin should be initialized to properties setting
		Margin margin = agent.margin;
		assertEquals(10, margin.getMaxAbsPosition());
		Range<Double> range = Range.closed(0.25, 0.35);
		for (int position = 0; position > -10; position--)
			assertTrue(range.contains(margin.getValue(position, SELL)));
		for (int position = 0; position < 10; position++)
			assertTrue(range.contains(-margin.getValue(position, BUY)));
	}
	
	@Test
	public void initialZIP() {
		ZIPAgent agent = zipAgent(Props.fromPairs(BetaMax.class, 0.5, BetaMin.class, 0.4));

		// verify beta in correct range
		assertTrue(Range.closed(0.4, 0.5).contains(agent.beta));
		assertEquals(0, agent.momentumChange, eps);
		assertNull(agent.limitPrice);
	}
	
	@Test
	public void computeRTest() {
		ZIPAgent agent = zipAgent(Props.fromPairs(RangeR.class, 0.1));

		double testR = agent.computeRCoefficient(true);
		assertTrue("Increasing R outside correct range",
				testR >= 1 && testR <= 1.1);
		testR = agent.computeRCoefficient(false);
		assertTrue("Decreasing R outside correct range",
				testR >= 0.9 && testR <= 1);
	}
	
	@Test
	public void computeATest() {
		ZIPAgent agent = zipAgent(Props.fromPairs(RangeA.class, 0.1));

		double testA = agent.computeACoefficient(true);
		assertTrue("Increasing R outside correct range",
				testA >= 0 && testA <= 0.1);
		testA = agent.computeACoefficient(false);
		assertTrue("Decreasing R outside correct range",
				testA >= -0.1 && testA <= 0);
	}
	
	@Test
	public void agentStrategyTest() {
		ZIPAgent agent = zipAgent(Props.builder()
				.put(MaxQty.class, 1)
				.put(BetaMax.class, 0.5)
				.put(BetaMin.class, 0.5)
				.put(GammaMax.class, 0.5)
				.put(GammaMin.class, 0.5)
				.put(RangeA.class, 0.3)
				.put(RangeR.class, 0.25)
				.put(MarginMax.class, 0.05)
				.put(MarginMin.class, 0.05)
				.build());
		// now with a dummy transaction and dummy order prices
		addTransaction(Price.of(95000), 1);
		addTransaction(Price.of(90000), 1);
		agent.limitPrice = agent.getLimitPrice(BUY, 1);
		// set the margins
		agent.type = SELL;
		agent.lastOrderPrice = Price.of(105000);
		agent.lastOrderPrice = Price.of(95000);

		// FIXME (for Elaine) Don't know how to properly test what this tried to test
//		/* 
//		 * This current test is based off the random seed. Currently this means that
//		 * Type: BUY
//		 * Trans1: R 0.96, A -0.18
//		 * Trans2: R 0.79, A -0.07
//		 */
//		agent.rand.setSeed(7221);
//		agent.agentStrategy();
//		// buyer reduces margins because transaction prices are less than order
//		// prices, submitted order price will be below the last order price
//		// should also be below the most recent transaction price
//		assertEquals("Incorrect random seed", BUY, Iterables.getOnlyElement(agent.getActiveOrders()).getOrderType());
//		checkSingleOrderRange(agent.getActiveOrders(), Price.of(85000), Price.of(95000), 1);
//
//		assertEquals(Price.of(88953), Iterables.getOnlyElement(agent.getActiveOrders()).getPrice());
	}
	
	@Test
	public void getCurrentMarginTest() {
		ZIPAgent agent = zipAgent(Props.fromPairs(
				MaxQty.class, 1,
				MarginMax.class, 1.5,
				MarginMin.class, 1.2));

		assertEquals(-1.0, agent.getCurrentMargin(BUY), eps);
		assertTrue("Current margin outside range", Range.closed(1.2, 1.5).contains(agent.getCurrentMargin(SELL)));
	}
	
	@Test
	public void updateMarginZeroLimit() {
		// testing when limit price is 0
		ZIPAgent agent = zipAgent(Props.builder()
				.put(MaxQty.class, 5)
				.put(BetaMax.class, 0.5)
				.put(BetaMin.class, 0.5)
				.put(GammaMax.class, 0.5)
				.put(GammaMin.class, 0.5)
				.put(RangeA.class, 0.3)
				.put(RangeR.class, 0.25)
				.put(MarginMax.class, 0.05)
				.put(MarginMin.class, 0.05)
				.build());

		// add dummy transaction
		addTransaction(Price.of(95000), 1);
		Transaction firstTrans = Iterables.getOnlyElement(view.getTransactions());

		agent.limitPrice = Price.ZERO;
		
		// set the margins
		agent.type = BUY;
		agent.lastOrderPrice = Price.of(99000);
		
		double oldMargin = agent.margin.getValue(0, agent.type);
		assertEquals(oldMargin, agent.getCurrentMargin(agent.type), eps);
		
		agent.updateMargin(firstTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, oldMargin, eps);
	}
	
	@Test
	public void checkIncreaseMarginBuyer() {
		ZIPAgent agent = zipAgent(Props.builder()
				.put(MaxQty.class, 5)
				.put(BetaMax.class, 0.5)
				.put(BetaMin.class, 0.5)
				.put(GammaMax.class, 0.5)
				.put(GammaMin.class, 0.5)
				.put(RangeA.class, 0.3)
				.put(RangeR.class, 0.25)
				.put(MarginMax.class, 0.05)
				.put(MarginMin.class, 0.05)
				.build());
		/*
		 * FIXME(for Elaine) Agent seed set to 1 for unknown reason, but it seems required
		 * to get test to work. Seed is rightly no longer accessible, so what is
		 * the proper way to test this?
		 */
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
		assertEquals(oldMargin, agent.getCurrentMargin(agent.type), 0.001);
		agent.updateMargin(lastTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(agent.type), 0.001);
		checkMarginUpdate(lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}
	
	@Test
	public void checkIncreaseMarginSeller() {
		ZIPAgent agent = zipAgent(Props.builder()
				.put(MaxQty.class, 5)
				.put(BetaMax.class, 0.5)
				.put(BetaMin.class, 0.5)
				.put(GammaMax.class, 0.5)
				.put(GammaMin.class, 0.5)
				.put(RangeA.class, 0.3)
				.put(RangeR.class, 0.25)
				.put(MarginMax.class, 0.05)
				.put(MarginMin.class, 0.05)
				.build());
		// FIXME Agent seed set to 1 for unknown reason

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
		assertEquals(oldMargin, agent.getCurrentMargin(agent.type), 0.001);
		agent.updateMargin(lastTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(agent.type), 0.001);
		checkMarginUpdate(lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}

	@Test
	public void checkDecreaseMarginBuyer() {
		ZIPAgent agent = zipAgent(Props.builder()
				.put(MaxQty.class, 5)
				.put(BetaMax.class, 0.5)
				.put(BetaMin.class, 0.5)
				.put(GammaMax.class, 0.5)
				.put(GammaMin.class, 0.5)
				.put(RangeA.class, 0.3)
				.put(RangeR.class, 0.25)
				.put(MarginMax.class, 0.05)
				.put(MarginMin.class, 0.05)
				.build());
		// FIXME Agent seed set to 1 for unknown reason
		
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
		assertEquals(oldMargin, agent.getCurrentMargin(agent.type), 0.001);
		agent.updateMargin(lastTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(agent.type), 0.001);
		checkMarginUpdate(lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}
	
	@Test
	public void checkDecreaseMarginSeller() {
		ZIPAgent agent = zipAgent(Props.builder()
				.put(MaxQty.class, 5)
				.put(BetaMax.class, 0.5)
				.put(BetaMin.class, 0.5)
				.put(GammaMax.class, 0.5)
				.put(GammaMin.class, 0.5)
				.put(RangeA.class, 0.3)
				.put(RangeR.class, 0.25)
				.put(MarginMax.class, 0.05)
				.put(MarginMin.class, 0.05)
				.build());
		// FIXME Agent seed set to 1 for unknown reason
		
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
		assertEquals(oldMargin, agent.getCurrentMargin(agent.type), 0.001);
		agent.updateMargin(lastTrans);
		double newMargin = agent.margin.getValue(0, agent.type);
		assertEquals(newMargin, agent.getCurrentMargin(agent.type), 0.001);
		checkMarginUpdate(lastOrderPrice, lastTransPrice, oldMargin, newMargin);
	}

	@Test
	public void computeOrderPrice() {
		ZIPAgent agent = zipAgent(Props.fromPairs(
				MaxQty.class, 5,
				MarginMax.class, 0.35,
				MarginMin.class, 0.25));
		
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
		ZIPAgent agent = zipAgent(Props.fromPairs(
				MaxQty.class, 5,
				BetaMax.class, 0.5,
				BetaMin.class, 0.5,
				GammaMax.class, 1d,
				GammaMin.class, 1d));

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
		ZIPAgent agent = zipAgent(Props.fromPairs(
				MaxQty.class, 5,
				BetaMax.class, 0.5,
				BetaMin.class, 0.5,
				GammaMax.class, 0d,
				GammaMin.class, 0d));

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
		ZIPAgent agent = zipAgent(Props.fromPairs(
				MaxQty.class, 5,
				BetaMax.class, 0.5,
				BetaMin.class, 0.5));
		
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
		ZIPAgent agent = zipAgent(Props.fromPairs(
				MaxQty.class, 5,
				MarginMax.class, 0.35,
				MarginMin.class, 0.25));

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
		ZIPAgent agent = zipAgent(Props.fromPairs(
				MaxQty.class, 5,
				MarginMax.class, 0.35,
				MarginMin.class, 0.25));
		
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
		ZIPAgent agent = zipAgent(Props.fromPairs(
				MaxQty.class, 5,
				MarginMax.class, 0.35,
				MarginMin.class, 0.25));
		
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
	
	@Test
	public void randomTest() {
		for (int i = 0; i < 1000; ++i) {
			setup();
			initialMarginTest();
			setup();
			marginRangeTest();
			setup();
			initialZIP();
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
			setup();
			updateMarginZeroLimit();
			setup();
			checkDecreaseMarginBuyer();
			setup();
			checkDecreaseMarginSeller();
			setup();
			checkIncreaseMarginBuyer();
			setup();
			checkIncreaseMarginSeller();
		}
	}
	
	/** Check margin updating correctly */
	private static void checkMarginUpdate(Price lastPrice, Price lastTransPrice, double oldMargin, double newMargin) {
		assertEquals(lastPrice.compareTo(lastTransPrice), Double.compare(oldMargin, newMargin));
	}
	
	
	private void addOrder(OrderType type, Price price, int quantity) {
		mockAgent.submitOrder(view, type, price, quantity);
	}

	private Transaction addTransaction(Price price, int quantity) {
		addOrder(BUY, price, quantity);
		addOrder(SELL, price, quantity);
		return checkNotNull(Iterables.getFirst(view.getTransactions(), null));
	}

	private ZIPAgent zipAgent(Props parameters) {
		return ZIPAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market,
				Props.merge(defaults, parameters));
	}

}


