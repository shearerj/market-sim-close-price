package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkSingleOrder;
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
import utils.Rands;

import com.google.common.collect.Iterables;

import data.Props;
import entity.agent.AAAgent.Aggression;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class AAAgentTest {

	private static final Random rand = new Random();
	private static final Props defaults = Props.fromPairs(
			Keys.REENTRY_RATE, 0,
			Keys.MAX_QUANTITY, 10,
			Keys.ETA, 3,
			Keys.WITHDRAW_ORDERS, false,
			Keys.WINDOW_LENGTH, 5000,
			Keys.AGGRESSION, 0,
			Keys.THETA, 0,
			Keys.THETA_MIN, -4,
			Keys.THETA_MAX, 4,
			Keys.NUM_HISTORICAL, 5,
			Keys.LAMBDA_R, 0.05,
			Keys.LAMBDA_A, 0.02,	// x ticks/$ for Eq 10/11 
			Keys.GAMMA, 2,
			Keys.BETA_R, 0.4,
			Keys.BETA_T, 0.4,
			Keys.DEBUG, true);

	private MockSim sim;
	private Market market;
	private MarketView view;
	private Agent mockAgent;

	@Before
	public void setup() throws IOException {
		sim = MockSim.create(getClass(),
				Log.Level.NO_LOGGING, Keys.FUNDAMENTAL_SHOCK_VAR,
				0, MarketType.CDA, j.join(Keys.NUM, 1));
		market = Iterables.getOnlyElement(sim.getMarkets());
		view = market.getPrimaryView();
		mockAgent = new BackgroundAgent(sim, TimeStamp.ZERO, market, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestAgent"; }
		};
	}

	@Test
	public void tauChange() {
		double r = 0.5;
		AAAgent agent = aaAgent(BUY);
		agent.theta = 2;
		assertEquals(0.268, agent.tauChange(r), 0.001);
	}

	/**
	 * Computation of moving average for estimating the equilibrium price.
	 */
	@Test
	public void estimateEquilibrium() {
		AAAgent agent = aaAgent(BUY,
				Keys.NUM_HISTORICAL, 3,
				Keys.PRIVATE_VALUE_VAR, 5E7);
		
		assertNull(agent.estimateEquilibrium(agent.getWindowTransactions()));
		
		// Adding Transactions and Bids
		addTransaction(Price.of(75000));
		addTransaction(Price.of(90000));

		// not enough transactions, need to use only 2
		double[] weights = {1/1.9, 0.9/1.9};
		assertEquals(Math.round(weights[0]*75000 + weights[1]*90000),
				agent.estimateEquilibrium(agent.getWindowTransactions()).intValue());
		
		// sufficient transactions
		addTransaction(Price.of(100000));
		double total = 2.71;
		double[] weights2 = {1/total, 0.9/total, 0.81/total};
		assertEquals(Math.round(weights2[0]*75000 + weights2[1]*90000 + weights2[2]*100000),
				agent.estimateEquilibrium(agent.getWindowTransactions()).intValue());
	}
	
	@Test
	public void computeRShoutBuyer() {
		AAAgent buyer = aaAgent(BUY, Keys.THETA, -2);
		
		Price limit = Price.of(110000);
		Price last = Price.of(105000);
		Price equil = Price.of(105000);
		
		// Intramarginal (limit > equil)
		assertEquals(0, buyer.computeRShout(limit, last, equil), 0.001);
		last = Price.of(100000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		last = Price.of(109000);	// more aggressive (lower margin)
		assertEquals(1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		
		// Extramarginal
		equil = Price.of(111000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(buyer.computeRShout(limit, last, equil)), 0.001);
		last = limit;
		assertEquals(0, buyer.computeRShout(limit, last, equil), 0.001);
	}
	
	@Test
	public void computeRShoutSeller() {
		AAAgent seller = aaAgent(SELL, Keys.THETA, -2);

		Price limit = Price.of(105000);
		Price last = Price.of(110000);
		Price equil = Price.of(110000);
		
		// Intramarginal (limit < equil)
		assertEquals(0, seller.computeRShout(limit, last, equil), 0.001);
		last = Price.of(111000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);
		last = Price.of(109000);	// more aggressive (lower margin)
		assertEquals(1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);

		// Extramarginal
		equil = Price.of(104000);	// less aggressive (higher margin)
		assertEquals(-1, Math.signum(seller.computeRShout(limit, last, equil)), 0.001);
		last = limit;
		assertEquals(0, seller.computeRShout(limit, last, equil), 0.001);
	}

	@Test
	public void determineTargetPriceBuyer() {
		AAAgent buyer = aaAgent(BUY, Keys.THETA, 2);
		
		Price limit = Price.of(110000);
		Price equil = Price.of(105000);
		
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
		limit = Price.of(104000);
		buyer.aggression = -1;		// passive
		assertEquals(Price.ZERO, buyer.determineTargetPrice(limit, equil));
		buyer.aggression = -0.5;	// less aggressive, so lower target than equil
		assertTrue(buyer.determineTargetPrice(limit, equil).lessThan(equil));
		buyer.aggression = 0.5;		// aggressiveness capped at 0
		assertEquals(limit, buyer.determineTargetPrice(limit, equil));
	}
	
	@Test
	public void determineTargetPriceSeller() {
		AAAgent seller = aaAgent(SELL, Keys.THETA, 2);
		
		Price limit = Price.of(105000);
		Price equil = Price.of(110000);
		
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
		limit = Price.of(111000);
		seller.aggression = -1;		// passive
		assertEquals(Price.INF, seller.determineTargetPrice(limit, equil));
		seller.aggression = -0.5;	// less aggressive, so lower target than equil
		assertTrue(seller.determineTargetPrice(limit, equil).greaterThan(equil));
		seller.aggression = 0.5;	// aggressiveness capped at 0
		assertEquals(limit, seller.determineTargetPrice(limit, equil));
	}
	
	@Test
	public void biddingLayerNoTarget() {
		Price limit = Price.of(145000);
		AAAgent buyer = aaAgent(BUY);
		AAAgent seller = aaAgent(SELL);
		
		setQuote(Price.of(Rands.nextUniform(rand, 75000, 80000)), Price.of(Rands.nextUniform(rand, 81000, 100000)));
		
		buyer.biddingLayer(limit, null, 1);
		sim.executeImmediate();
		checkSingleOrderRange(buyer.activeOrders, Price.of(75000), Price.of(100000), 1);
		
		seller.biddingLayer(limit, null, 1);
		sim.executeImmediate();
		checkSingleOrderRange(seller.activeOrders, Price.of(75000), Price.of(100000), 1);
	}
	
	@Test
	public void biddingLayerBuyer() {
		Price limit = Price.of(145000);
		Price target = Price.of(175000);
		AAAgent buyer = aaAgent(BUY);
		
		buyer.biddingLayer(limit, target, 1);
		assertEquals(1, buyer.activeOrders.size());
		sim.executeImmediate();
		buyer.withdrawAllOrders();
		sim.executeImmediate();
		
		setQuote(Price.of(150000), Price.of(200000));
		
		buyer.positionBalance = buyer.privateValue.getMaxAbsPosition() + 1;
		sim.executeUntil(TimeStamp.of(20));
		buyer.biddingLayer(limit, target, 1);
		assertTrue(buyer.activeOrders.isEmpty()); // would exceed max position
		
		buyer.positionBalance = 0;
		buyer.biddingLayer(limit, target, 1);
		assertTrue(buyer.activeOrders.isEmpty()); // limit price < bid
		
		limit = Price.of(211000);
		buyer.biddingLayer(limit, null, 1);
		sim.executeUntil(TimeStamp.of(20));
		checkSingleOrder(buyer.activeOrders, Price.of(170007), 1, TimeStamp.of(20), TimeStamp.of(20));
		
		limit = Price.of(210000);
		target = Price.of(180000);
		buyer.withdrawAllOrders();
		sim.executeImmediate();
		buyer.biddingLayer(limit, target, 1);
		sim.executeUntil(TimeStamp.of(20));
		checkSingleOrder(buyer.activeOrders, Price.of(160000), 1, TimeStamp.of(20), TimeStamp.of(20));
	}
	
	@Test
	public void biddingLayerSeller() {
		
		Price limit = Price.of(210000);
		Price target = Price.of(175000);
		
		AAAgent seller = aaAgent(SELL);
		
		seller.biddingLayer(limit, target, 1);
		assertEquals(1, seller.activeOrders.size()); // ZI strat
		sim.executeImmediate();
		seller.withdrawAllOrders();
		sim.executeImmediate();
		
		setQuote(Price.of(150000), Price.of(200000));
		
		sim.executeUntil(TimeStamp.of(20));
		seller.positionBalance = seller.privateValue.getMaxAbsPosition() + 1;
		seller.biddingLayer(limit, target, 1);
		assertTrue(seller.activeOrders.isEmpty()); // would exceed max position
		
		seller.positionBalance = 0;
		seller.biddingLayer(limit, target, 1);
		assertTrue(seller.activeOrders.isEmpty()); // limit price > ask
		
		limit = Price.of(170000);
		seller.biddingLayer(limit, null, 1);
		sim.executeUntil(TimeStamp.of(20));
		checkSingleOrder(seller.activeOrders, Price.of(190000), 1, TimeStamp.of(20), TimeStamp.of(20));
		
		limit = Price.of(165000);
		target = Price.of(170000);
		seller.withdrawAllOrders();
		sim.executeImmediate();
		seller.biddingLayer(limit, target, 1);
		sim.executeUntil(TimeStamp.of(20));
		checkSingleOrder(seller.activeOrders, Price.of(190000), 1, TimeStamp.of(20), TimeStamp.of(20));
	}
	
	// The name of this test get deleted somehow...
	@Test
	public void forgottenTest() {
		AAAgent agent = aaAgent(SELL,
				Keys.NUM_HISTORICAL, 5,
				Keys.THETA_MAX, 8,
				Keys.THETA_MIN, -8,
				Keys.BETA_T, 0.25,
				Keys.GAMMA, 2,
				Keys.THETA, -4);
		
		agent.updateTheta(null, agent.getWindowTransactions());
		assertEquals(-4, agent.theta, 0.001);
		
		addTransaction(Price.of(105000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		
		Price equil = Price.of(100000);
		sim.executeUntil(TimeStamp.of(20));
		// haven't used 5 transactions yet, so keep theta fixed
		agent.updateTheta(equil, agent.getWindowTransactions());
		assertEquals(-4, agent.theta, 0.001);
		
		addTransaction(Price.of(110000));
		addTransaction(Price.of(111000));
		sim.executeUntil(TimeStamp.of(40));
		agent.updateTheta(equil, agent.getWindowTransactions());
		assertTrue(agent.theta < -4);	// decreases because high price vol
		assertEquals(-5, agent.theta, 0.001); 
		
		addTransaction(Price.of(106000));
		addTransaction(Price.of(107000));
		addTransaction(Price.of(108000));
		equil = Price.of(108000);
		sim.executeUntil(TimeStamp.of(50));
		agent.updateTheta(equil, agent.getWindowTransactions());
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
		sim.log(DEBUG, "\nTesting buyer on empty market: Result should be price=0");
		// Creating a buyer
		AAAgent agent = aaAgent(BUY);
		assertTrue(agent.type.equals(BUY));
		// Testing against an empty market
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		
		assertEquals(1, agent.activeOrders.size());
	}

	@Test
	public void initialSeller() {
		sim.log(DEBUG, "\nTesting seller on empty market: Result should be price=%s", Price.INF);
		// Creating a seller
		AAAgent agent = aaAgent(SELL);
		assertTrue(agent.type.equals(SELL));
		// Testing against an empty market
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertEquals(1, agent.activeOrders.size());
	}

	@Test
	public void noTransactionsBuyer() {
		sim.log(DEBUG, "\nTesting buyer on market with bids/asks but no transactions");
		sim.log(DEBUG, "50000 < Bid price < 100000");

		setQuote(Price.of(50000), Price.of(200000));

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = aaAgent(BUY, Keys.ETA, 4);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();

		// Asserting the bid is correct (based on EQ 10/11)
		checkSingleOrder(agent.activeOrders, Price.of(62500), 1, TimeStamp.of(100), TimeStamp.of(100));
	}

	@Test
	public void noTransactionsSeller() {
		sim.log(DEBUG, "\nTesting seller on market with bids/asks but no transactions");
		sim.log(DEBUG, "100000 < Ask price < 200000");

		setQuote(Price.of(50000), Price.of(200000));

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = aaAgent(SELL, Keys.ETA, 4);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();

		// Asserting the bid is correct (based on EQ 10/11)
		checkSingleOrder(agent.activeOrders, Price.of(175000), 1, TimeStamp.of(100), TimeStamp.of(100));
	}

	@Test
	public void IntraBuyerPassive() {
		sim.log(DEBUG, "\nTesting passive buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));
		
		AAAgent agent = aaAgent(BUY, Keys.AGGRESSION, -1);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(30000), Price.of(35000), 1);
	}

	@Test
	public void IntraBuyerNegative() {
		sim.log(DEBUG, "\nTesting r = -0.5 buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));

		AAAgent agent = aaAgent(BUY,
				Keys.THETA, -3,
				Keys.AGGRESSION, -0.5);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(36000), Price.of(41000), 1);
	}

	/**
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraBuyerActive() {
		sim.log(DEBUG, "\nTesting active buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));

		AAAgent agent = aaAgent(BUY, Keys.AGGRESSION, 0);
		sim.log(DEBUG, "Price ~= 58333");
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(57000), Price.of(59000), 1);
	}

	/**
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraBuyerPositive() {
		sim.log(DEBUG, "\nTesting r = -0.5 buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));

		AAAgent agent = aaAgent(BUY,
				Keys.THETA, -3,
				Keys.AGGRESSION, 0.5);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(64000), Price.of(66000), 1);
	}

	@Test
	public void IntraBuyerAggressive() {
		sim.log(DEBUG, "");
		sim.log(DEBUG, "Testing aggressive buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));

		AAAgent agent = aaAgent(BUY, Keys.AGGRESSION, 1);
		sim.log(DEBUG, "Price ~= 66667");
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();

		checkSingleOrderRange(agent.activeOrders, Price.of(65000), Price.of(68000), 1);
	}

	@Test
	public void IntraSellerPassive() { // Check Aggression
		sim.log(DEBUG, "");
		sim.log(DEBUG, "Testing passive seller on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(SELL, Keys.AGGRESSION, -1);
		sim.log(DEBUG, "Price ~= %d", 150000 + (Price.INF.intValue() - 150000) / 3);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		// Asserting the bid is correct
		Price low = Price.of(150000 + (Price.INF.intValue() - 150000) / 3 - 1000);
		Price high = Price.of(150000 + (Price.INF.intValue() - 150000) / 3 + 1000);
		checkSingleOrderRange(agent.activeOrders, low, high, 1);
	}

	@Test
	public void IntraSellerActive() {
		sim.log(DEBUG, "\nTesting active seller on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(SELL, Keys.AGGRESSION, 0);
		sim.log(DEBUG, "Price ~= 141667");
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(138000), Price.of(144000), 1);
	}

	@Test
	public void IntraSellerAggressive() { // Check Aggression
		sim.log(DEBUG, "");
		sim.log(DEBUG, "Testing aggressive seller on market with transactions");

		// Adding Transactions and Bids
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(SELL, Keys.AGGRESSION, 1);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(130000), Price.of(135000), 1);
	}


	@Test
	public void ExtraBuyerPassive() {
		sim.log(DEBUG, "\nTesting passive buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));
		
		AAAgent agent = aaAgent(BUY, Keys.AGGRESSION, -1);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(30000), Price.of(35000), 1);
	}

	@Test
	public void ExtraBuyerNegative() {
		sim.log(DEBUG, "\nTesting r = -0.5 buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(BUY,
				Keys.THETA, -3,
				Keys.AGGRESSION, -0.5);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(38000), Price.of(41000), 1);
	}

	@Test
	public void ExtraBuyerActive() {
		sim.log(DEBUG, "\nTesting r = 0 buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));
		
		AAAgent agent = aaAgent(BUY,
				Keys.THETA, -3,
				Keys.AGGRESSION, 0);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(65000), Price.of(70000), 1);
	}

	@Test
	public void ExtraBuyerPositive() {
		sim.log(DEBUG, "\nTesting r = 0 buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(BUY,
				Keys.THETA, -3,
				Keys.AGGRESSION, 0.5);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkSingleOrderRange(agent.activeOrders, Price.of(65000), Price.of(70000), 1);
	}

	@Test
	public void ExtraSellerPassive() {
		sim.log(DEBUG, "\nTesting passive buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));
		
		AAAgent agent = aaAgent(SELL, Keys.AGGRESSION, -1);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();

		// Asserting the bid is correct
		Price low = Price.of(95000 + (int) (Price.INF.doubleValue() / 3));
		Price high = Price.of(105000 + (int) (Price.INF.doubleValue() / 3));
		checkSingleOrderRange(agent.activeOrders, low, high, 1);
	}

	@Test
	public void ExtraSellerActive() {
		sim.log(DEBUG, "\nTesting passive buyer on market with transactions");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));
		
		AAAgent agent = aaAgent(SELL, Keys.AGGRESSION, 0);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();

		// Asserting the bid is correct
		checkSingleOrderRange(agent.activeOrders, Price.of(132000), Price.of(135000), 1);
	}

	@Test
	public void BuyerAggressionIncrease() {
		sim.log(DEBUG, "\nTesting aggression learning");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(95000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(105000));

		AAAgent agent = aaAgent(BUY);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();
		assertTrue(agent.aggression > 0);
		
		checkSingleOrderRange(agent.activeOrders, Price.of(50000), Price.of(100000), 1);
	}

	@Test
	public void SellerAggressionIncrease() {
		sim.log(DEBUG, "\nTesting aggression learning");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(105000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(95000));

		AAAgent agent = aaAgent(SELL);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();
		assertTrue(agent.aggression > 0);
		
		checkSingleOrderRange(agent.activeOrders, Price.of(100000), Price.of(150000), 1);
	}

	/**
	 * Test short-term learning (EQ 7)
	 */
	@Test
	public void updateAggressionBuyer() {
		sim.log(DEBUG, "\nTesting aggression update (buyer)");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(105000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(95000));

		double oldAggression = 0.5;
		AAAgent agent = aaAgent(BUY,
				Keys.THETA, -3,
				Keys.AGGRESSION, oldAggression);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();

		checkAggressionUpdate(BUY, agent.lastTransactionPrice, agent.targetPrice, oldAggression, agent.aggression);
	}

	/**
	 * Test short-term learning (EQ 7)
	 * Note that the value of theta affects whether or not this test will pass
	 * (e.g. -3 causes a NaN in computeRShout)
	 */
	@Test
	public void updateAggressionSeller() {
		sim.log(DEBUG, "\nTesting aggression update (seller)");

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(105000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(110000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(95000));

		
		double oldAggression = 0.2;
		AAAgent agent = aaAgent(SELL,
				Keys.THETA, 2,
				Keys.AGGRESSION, oldAggression);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkAggressionUpdate(SELL, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	/**
	 * Note that for extramarginal buyer, must have last transaction price less
	 * than the limit otherwise rShout gets set to 0.
	 */
	@Test
	public void randomizedUpdateAggressionBuyer() {
		sim.log(DEBUG, "\nTesting aggression update (buyer)");

		setQuote(Price.of(Rands.nextUniform(rand, 25000, 75000)), Price.of(Rands.nextUniform(rand, 125000, 175000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 100000, 110000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 50000, 150000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 100000, 120000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 750000, 150000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 80000, 100000)));

		
		double oldAggression = 0.5;
		AAAgent agent = aaAgent(BUY,
				Keys.THETA, -2,
				Keys.PRIVATE_VALUE_VAR, 5E7,
				Keys.AGGRESSION, oldAggression);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		sim.executeImmediate();

		checkAggressionUpdate(BUY, agent.lastTransactionPrice, agent.targetPrice, oldAggression, agent.aggression);
	}

	/**
	 * For intramarginal seller, want limit price less than equilibrium,
	 * otherwise rShout clipped at 0.
	 */
	@Test
	public void randomizedUpdateAggressionSeller() {
		sim.log(DEBUG, "\nTesting aggression update (seller)");

		setQuote(Price.of(Rands.nextUniform(rand, 25000, 75000)), Price.of(Rands.nextUniform(rand, 125000, 175000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 100000, 110000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 500000, 150000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 500000, 120000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 100000, 150000)));
		addTransaction(Price.of(Rands.nextUniform(rand, 100000, 110000)));

		double oldAggression = 0.2;
		AAAgent agent = aaAgent(SELL,
				Keys.THETA, -3,
				Keys.PRIVATE_VALUE_VAR, 5E7,
				Keys.AGGRESSION, oldAggression);
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkAggressionUpdate(SELL, agent.lastTransactionPrice, agent.targetPrice, oldAggression, agent.aggression);
	}

	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i < 100; i++) {
			setup();
			randomizedUpdateAggressionBuyer();
			setup();
			randomizedUpdateAggressionSeller();
			setup();
			biddingLayerNoTarget();
		}
	}

	/** Check aggression updating */
	private void checkAggressionUpdate(OrderType type, Price lastTransactionPrice,
			Price targetPrice, double oldAggression, double aggression) {
		assertEquals("Agression update incorrect",
				(type == BUY ? 1 : -1) * targetPrice.compareTo(lastTransactionPrice),
				Double.compare(oldAggression, aggression));
	}

	private AAAgent aaAgent(OrderType type, Object... parameters) {
		return new AAAgent(sim, TimeStamp.ZERO, market, rand,
				Props.withDefaults(Props.withDefaults(defaults,
						Keys.BUYER_STATUS, type == BUY,
						Keys.PRIVATE_VALUE_VAR, 0,	// private values all 0
						Keys.DEBUG, true), parameters));
	}

	private void addOrder(OrderType type, Price price) {
		mockAgent.submitOrder(view, type, price, 1);
		sim.executeImmediate();
	}

	private void addTransaction(Price price) {
		addOrder(BUY, price);
		addOrder(SELL, price);
	}
	
	private void setQuote(Price bid, Price ask) {
		addOrder(BUY, bid);
		addOrder(SELL, ask);
	}
	
}
