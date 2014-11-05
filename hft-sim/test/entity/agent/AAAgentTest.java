package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertSingleOrderRange;
import static utils.Tests.assertSingleOrder;

import java.io.IOException;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.BetaT;
import systemmanager.Keys.BuyerStatus;
import systemmanager.Keys.Debug;
import systemmanager.Keys.Eta;
import systemmanager.Keys.Gamma;
import systemmanager.Keys.InitAggression;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.NumHistorical;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.Theta;
import systemmanager.Keys.ThetaMax;
import systemmanager.Keys.ThetaMin;
import systemmanager.Keys.WindowLength;
import systemmanager.Keys.WithdrawOrders;
import utils.Mock;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class AAAgentTest {

	private static final Rand rand = Rand.create();
	private static final Agent mockAgent = Mock.agent();
	private static final FundamentalValue fundamental = Mock.fundamental(100000);
	private static final Props defaults = Props.builder()
			.put(ArrivalRate.class,		0d)
			.put(ReentryRate.class,		0d)
			.put(PrivateValueVar.class,	0d)
			.put(MaxQty.class,			1)
			.put(WithdrawOrders.class,	false)
			.put(WindowLength.class,	TimeStamp.of(5000))
			.put(Theta.class,			0d)
			.put(ThetaMin.class,		-4d)
			.put(ThetaMax.class,		4d)
			.put(NumHistorical.class,	5)
			.put(Debug.class,			true)
			.build();

	
	private Market market;
	private MarketView view;

	@Before
	public void setup() throws IOException {
		market = Mock.market();
		view = market.getPrimaryView();
	}

	@Test
	public void tauChange() {
		double r = 0.5;
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY));
		agent.theta = 2;
		assertEquals(0.268, agent.tauChange(r), 0.001);
	}

	/**
	 * Computation of moving average for estimating the equilibrium price.
	 */
	@Test
	public void estimateEquilibrium() {
		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, BUY,
				NumHistorical.class, 3,
				PrivateValueVar.class, 5E7));
		
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
		AAAgent buyer = aaAgent(Props.fromPairs(BuyerStatus.class, BUY, Theta.class, -2d));
		
		Price limit = Price.of(110000);
		Price last = Price.of(105000);
		Price equil = Price.of(105000);
		
		// Intramarginal (limit > equal)
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
		AAAgent seller = aaAgent(Props.fromPairs(BuyerStatus.class, SELL, Theta.class, -2d));

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
		AAAgent buyer = aaAgent(Props.fromPairs(BuyerStatus.class, BUY, Theta.class, 2d));
		
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
		AAAgent seller = aaAgent(Props.fromPairs(BuyerStatus.class, SELL, Theta.class, 2d));
		
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
		AAAgent buyer = aaAgent(Props.fromPairs(BuyerStatus.class, BUY));
		AAAgent seller = aaAgent(Props.fromPairs(BuyerStatus.class, SELL));
		
		setQuote(Price.of(rand.nextUniform(75000, 80000)), Price.of(rand.nextUniform(81000, 100000)));
		
		buyer.biddingLayer(limit, null, 1);
		assertSingleOrderRange(buyer.getActiveOrders(), Price.of(75000), Price.of(100000), 1);
		
		seller.biddingLayer(limit, null, 1);
		assertSingleOrderRange(seller.getActiveOrders(), Price.of(75000), Price.of(100000), 1);
	}
	
	@Test
	public void biddingLayerBuyer() {
		Price limit = Price.of(145000);
		Price target = Price.of(175000);
		AAAgent buyer = aaAgent(Props.fromPairs(BuyerStatus.class, BUY));
		
		buyer.biddingLayer(limit, target, 1);
		assertEquals(1, buyer.getActiveOrders().size());
		buyer.withdrawAllOrders();
		
		setPosition(buyer, buyer.getMaxAbsPosition() + 1);
		setQuote(Price.of(150000), Price.of(200000));
		
//		sim.executeUntil(TimeStamp.of(20));
		buyer.biddingLayer(limit, target, 1);
		assertTrue(buyer.getActiveOrders().isEmpty()); // would exceed max position
		
		buyer.liquidateAtPrice(Price.ZERO); // Resets Position
		buyer.biddingLayer(limit, target, 1);
		assertTrue(buyer.getActiveOrders().isEmpty()); // limit price < bid
		
		limit = Price.of(211000);
		buyer.biddingLayer(limit, null, 1);
		assertSingleOrder(buyer.getActiveOrders(), Price.of(170007), 1, TimeStamp.of(20), TimeStamp.of(20));
		
		limit = Price.of(210000);
		target = Price.of(180000);
		buyer.withdrawAllOrders();
		buyer.biddingLayer(limit, target, 1);
		assertSingleOrder(buyer.getActiveOrders(), Price.of(160000), 1, TimeStamp.of(20), TimeStamp.of(20));
	}
	
	@Test
	public void biddingLayerSeller() {
		
		Price limit = Price.of(210000);
		Price target = Price.of(175000);
		
		AAAgent seller = aaAgent(Props.fromPairs(BuyerStatus.class, SELL));
		
		seller.biddingLayer(limit, target, 1);
		assertEquals(1, seller.getActiveOrders().size()); // ZI strat
		seller.withdrawAllOrders();
		
		setPosition(seller, seller.getMaxAbsPosition() + 1);
		setQuote(Price.of(150000), Price.of(200000));
		
//		sim.executeUntil(TimeStamp.of(20));
		seller.biddingLayer(limit, target, 1);
		assertTrue(seller.getActiveOrders().isEmpty()); // would exceed max position
		
		seller.liquidateAtPrice(Price.ZERO); // Resets Position
		seller.biddingLayer(limit, target, 1);
		assertTrue(seller.getActiveOrders().isEmpty()); // limit price > ask
		
		limit = Price.of(170000);
		seller.biddingLayer(limit, null, 1);
		assertSingleOrder(seller.getActiveOrders(), Price.of(190000), 1, TimeStamp.of(20), TimeStamp.of(20));
		
		limit = Price.of(165000);
		target = Price.of(170000);
		seller.withdrawAllOrders();
		seller.biddingLayer(limit, target, 1);
		assertSingleOrder(seller.getActiveOrders(), Price.of(190000), 1, TimeStamp.of(20), TimeStamp.of(20));
	}
	
	// The name of this test get deleted somehow...
	@Test
	public void forgottenTest() {
		AAAgent agent = aaAgent(Props.builder()
				.put(BuyerStatus.class, BUY)
				.put(NumHistorical.class, 5)
				.put(ThetaMax.class, 8d)
				.put(ThetaMin.class, -8d)
				.put(BetaT.class, 0.25)
				.put(Gamma.class, 2d)
				.put(Theta.class, -4d)
				.build());
		
		agent.updateTheta(null, agent.getWindowTransactions());
		assertEquals(-4, agent.theta, 0.001);
		
		addTransaction(Price.of(105000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		
		Price equil = Price.of(100000);
		// haven't used 5 transactions yet, so keep theta fixed
		agent.updateTheta(equil, agent.getWindowTransactions());
		assertEquals(-4, agent.theta, 0.001);
		
		addTransaction(Price.of(110000));
		addTransaction(Price.of(111000));
		agent.updateTheta(equil, agent.getWindowTransactions());
		assertTrue(agent.theta < -4);	// decreases because high price vol
		assertEquals(-5, agent.theta, 0.001); 
		
		addTransaction(Price.of(106000));
		addTransaction(Price.of(107000));
		addTransaction(Price.of(108000));
		equil = Price.of(108000);
		agent.updateTheta(equil, agent.getWindowTransactions());
		assertTrue(agent.theta > -5);	// increases because lower price vol
	}
	
	/** Testing buyer on empty market: Result should be price=0 */
	@Test
	public void initialBuyer() {
		// Creating a buyer
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY));
		assertTrue(agent.type.equals(BUY));
		// Testing against an empty market
		agent.agentStrategy();
		
		assertEquals(1, agent.getActiveOrders().size());
	}

	/** Testing seller on empty market: Result should be price=Inf */
	@Test
	public void initialSeller() {
		// Creating a seller
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, SELL));
		assertTrue(agent.type.equals(SELL));
		// Testing against an empty market
		agent.agentStrategy();

		assertEquals(1, agent.getActiveOrders().size());
	}

	/** Testing buyer on market with bids/asks but no transactions 50000 < Bid price < 100000 */
	@Test
	public void noTransactionsBuyer() {
		setQuote(Price.of(50000), Price.of(200000));

		// Testing against a market with initial bids but no transaction history
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY, Eta.class, 4));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		// Asserting the bid is correct (based on EQ 10/11)
		assertSingleOrder(agent.getActiveOrders(), Price.of(62500), 1, TimeStamp.of(100), TimeStamp.of(100));
	}

	/** Testing seller on market with bids/asks but no transactions 100000 < Ask price < 200000 */
	@Test
	public void noTransactionsSeller() {
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, SELL, Eta.class, 4));
		setQuote(Price.of(50000), Price.of(200000));

		// Testing against a market with initial bids but no transaction history
//		timeline.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		// Asserting the bid is correct (based on EQ 10/11)
		assertSingleOrder(agent.getActiveOrders(), Price.of(175000), 1, TimeStamp.of(100), TimeStamp.of(100));
	}

	/** Testing passive buyer on market with transactions */
	@Test
	public void IntraBuyerPassive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));
		
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY, InitAggression.class, -1d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(30000), Price.of(35000), 1);
	}

	/** Testing r = -0.5 buyer on market with transactions */
	@Test
	public void IntraBuyerNegative() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));

		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, BUY,
				Theta.class, -3d,
				InitAggression.class, -0.5));
		
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(36000), Price.of(41000), 1);
	}

	/**
	 * Testing active buyer on market with transactions Price ~= 58333
	 * 
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraBuyerActive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));

		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY, InitAggression.class, 0d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(57000), Price.of(59000), 1);
	}

	/**
	 * Testing r = -0.5 buyer on market with transactions
	 * 
	 * Currently fails due to incorrect market behavior,
	 * but AAAgent acts correctly based on the information it receives
	 */
	@Test
	public void IntraBuyerPositive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));

		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY,
				Theta.class, -3d,
				InitAggression.class, 0.5));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(64000), Price.of(66000), 1);
	}

	/** Testing aggressive buyer on market with transactions Price ~= 66667 */
	@Test
	public void IntraBuyerAggressive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));

		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY, InitAggression.class, 1d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(65000), Price.of(68000), 1);
	}

	/**
	 * Testing passive seller on market with transactions
	 * 
	 * Price ~= 150000 + (Price.INF.intValue() - 150000) / 3
	 */
	@Test
	public void IntraSellerPassive() { // Check Aggression
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, SELL, InitAggression.class, 11d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		// Asserting the bid is correct
		Price low = Price.of(150000 + (Price.INF.intValue() - 150000) / 3 - 1000);
		Price high = Price.of(150000 + (Price.INF.intValue() - 150000) / 3 + 1000);
		assertSingleOrderRange(agent.getActiveOrders(), low, high, 1);
	}

	/** Testing active seller on market with transactions Price ~= 141667 */
	@Test
	public void IntraSellerActive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, SELL, InitAggression.class, 0d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(138000), Price.of(144000), 1);
	}

	/** Testing aggressive seller on market with transactions */
	@Test
	public void IntraSellerAggressive() { // Check Aggression
		// Adding Transactions and Bids
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, SELL, InitAggression.class, 1d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(130000), Price.of(135000), 1);
	}

	/** Testing passive buyer on market with transactions */
	@Test
	public void ExtraBuyerPassive() {

		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));
		
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY, InitAggression.class, -1d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(30000), Price.of(35000), 1);
	}

	/** Testing r = -0.5 buyer on market with transactions */
	@Test
	public void ExtraBuyerNegative() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, BUY,
				Theta.class, -3d,
				InitAggression.class, -0.5));
		
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(38000), Price.of(41000), 1);
	}

	/** Testing r = 0 buyer on market with transactions */
	@Test
	public void ExtraBuyerActive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));
		
		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, BUY,
				Theta.class, -3d,
				InitAggression.class, 0d));
		
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(65000), Price.of(70000), 1);
	}

	/** Testing r = 0 buyer on market with transactions */
	@Test
	public void ExtraBuyerPositive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(125000));

		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, BUY,
				Theta.class, -3d,
				InitAggression.class, 0.5));
		
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		assertSingleOrderRange(agent.getActiveOrders(), Price.of(65000), Price.of(70000), 1);
	}

	/** Testing passive buyer on market with transactions */
	@Test
	public void ExtraSellerPassive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));
		
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, SELL, InitAggression.class, -1d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		// Asserting the bid is correct
		Price low = Price.of(95000 + (int) (Price.INF.doubleValue() / 3));
		Price high = Price.of(105000 + (int) (Price.INF.doubleValue() / 3));
		assertSingleOrderRange(agent.getActiveOrders(), low, high, 1);
	}

	/** Testing passive buyer on market with transactions */
	@Test
	public void ExtraSellerActive() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(75000));
		
		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, SELL, InitAggression.class, 0d));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		// Asserting the bid is correct
		assertSingleOrderRange(agent.getActiveOrders(), Price.of(132000), Price.of(135000), 1);
	}

	/** Testing aggression learning */
	@Test
	public void BuyerAggressionIncrease() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(95000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(105000));

		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, BUY));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		assertTrue(agent.aggression > 0);
		
		assertSingleOrderRange(agent.getActiveOrders(), Price.of(50000), Price.of(100000), 1);
	}

	/** Testing aggression learning */
	@Test
	public void SellerAggressionIncrease() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(105000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(95000));

		AAAgent agent = aaAgent(Props.fromPairs(BuyerStatus.class, SELL));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		assertTrue(agent.aggression > 0);
		
		assertSingleOrderRange(agent.getActiveOrders(), Price.of(100000), Price.of(150000), 1);
	}

	/** Test short-term learning (EQ 7) : Testing aggression update (buyer) */
	@Test
	public void updateAggressionBuyer() {
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(105000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(95000));

		double oldAggression = 0.5;
		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, BUY,
				Theta.class, -3d,
				InitAggression.class, oldAggression));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkAggressionUpdate(BUY, agent.lastTransactionPrice, agent.targetPrice, oldAggression, agent.aggression);
	}

	/**
	 * Testing aggression update (seller)
	 * 
	 * Test short-term learning (EQ 7)
	 * Note that the value of theta affects whether or not this test will pass
	 * (e.g. -3 causes a NaN in computeRShout)
	 */
	@Test
	public void updateAggressionSeller() {
		double oldAggression = 0.2;
		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, SELL,
				Theta.class, 2d,
				InitAggression.class, oldAggression));
		
		setQuote(Price.of(50000), Price.of(150000));
		addTransaction(Price.of(105000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(110000));
		addTransaction(Price.of(100000));
		addTransaction(Price.of(95000));
		
		agent.agentStrategy();

		checkAggressionUpdate(SELL, agent.lastTransactionPrice, agent.targetPrice,
				oldAggression, agent.aggression);
	}

	/**
	 * Testing aggression update (buyer)
	 * 
	 * Note that for extramarginal buyer, must have last transaction price less
	 * than the limit otherwise rShout gets set to 0.
	 */
	@Test
	public void randomizedUpdateAggressionBuyer() {
		setQuote(Price.of(rand.nextUniform(25000, 75000)), Price.of(rand.nextUniform(125000, 175000)));
		addTransaction(Price.of(rand.nextUniform(100000, 110000)));
		addTransaction(Price.of(rand.nextUniform(50000, 150000)));
		addTransaction(Price.of(rand.nextUniform(100000, 120000)));
		addTransaction(Price.of(rand.nextUniform(750000, 150000)));
		addTransaction(Price.of(rand.nextUniform(80000, 100000)));

		
		double oldAggression = 0.5;
		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, BUY,
				Theta.class, -2d,
				PrivateValueVar.class, 5e7,
				InitAggression.class, oldAggression));
//		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();

		checkAggressionUpdate(BUY, agent.lastTransactionPrice, agent.targetPrice, oldAggression, agent.aggression);
	}

	/**
	 * Testing aggression update (seller)
	 * 
	 * For intramarginal seller, want limit price less than equilibrium,
	 * otherwise rShout clipped at 0.
	 */
	@Test
	public void randomizedUpdateAggressionSeller() {
		double oldAggression = 0.2;
		AAAgent agent = aaAgent(Props.fromPairs(
				BuyerStatus.class, SELL,
				Theta.class, -3d,
				PrivateValueVar.class, 5e7,
				InitAggression.class, oldAggression));
		
		setQuote(Price.of(rand.nextUniform(25000, 75000)), Price.of(rand.nextUniform(125000, 175000)));
		addTransaction(Price.of(rand.nextUniform(100000, 110000)));
		addTransaction(Price.of(rand.nextUniform(500000, 150000)));
		addTransaction(Price.of(rand.nextUniform(500000, 120000)));
		addTransaction(Price.of(rand.nextUniform(100000, 150000)));
		addTransaction(Price.of(rand.nextUniform(100000, 110000)));

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

	private AAAgent aaAgent(Props parameters) {
		return AAAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market,
				Props.merge(defaults, parameters));
	}

	private void addOrder(OrderType type, Price price) {
		mockAgent.submitOrder(view, type, price, 1);
	}

	private void addTransaction(Price price) {
		addOrder(BUY, price);
		addOrder(SELL, price);
	}
	
	private void setQuote(Price bid, Price ask) {
		addOrder(BUY, bid);
		addOrder(SELL, ask);
	}
	
	private void setPosition(Agent agent, int position) {
		int quantity = position - agent.getPosition();
		if (quantity == 0)
			return;
		OrderType type = quantity > 0 ? BUY : SELL;
		quantity = Math.abs(quantity);
		agent.submitOrder(view, type, Price.ZERO, quantity);
		mockAgent.submitOrder(view, type == BUY ? SELL : BUY, Price.ZERO, quantity);
		assertEquals(position, agent.getPosition());
	}
	
}
