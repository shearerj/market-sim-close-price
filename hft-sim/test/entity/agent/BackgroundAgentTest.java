package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static logger.Log.Level.DEBUG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.checkSingleOrder;
import static utils.Tests.checkSingleOrderRange;
import static utils.Tests.checkSingleTransaction;
import static utils.Tests.j;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;
import utils.SummStats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ObjectArrays;

import data.FundamentalValue.FundamentalValueView;
import data.Props;
import data.Stats;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class BackgroundAgentTest {
	
	private static final Random rand = new Random();
	private static final PrivateValue simple = new PrivateValue(1, ImmutableList.of(Price.of(100), Price.of(10)));
	private static final double eps = 0.05;
	private static final double kappa = 0.2;
	private static final int meanValue = 100000;
	private static final int simulationLength = 60000;
	private static final Props defaults = Props.fromPairs(
			Keys.PRIVATE_VALUE_VAR, 1e8,
			Keys.BID_RANGE_MIN, 0,
			Keys.BID_RANGE_MAX, 1000);
	private static Object[] zirpProps = new Object[] {
		Keys.REENTRY_RATE, 0,
		Keys.MAX_QUANTITY, 2,
		Keys.PRIVATE_VALUE_VAR, 100,
		Keys.BID_RANGE_MIN, 10000,
		Keys.BID_RANGE_MAX, 10000,
		Keys.SIMULATION_LENGTH, simulationLength,
		Keys.FUNDAMENTAL_KAPPA, kappa,
		Keys.FUNDAMENTAL_MEAN, meanValue,
		Keys.FUNDAMENTAL_SHOCK_VAR, 0,
		Keys.WITHDRAW_ORDERS, true,
		Keys.ACCEPTABLE_PROFIT_FRACTION, 0.75};

	private MockSim sim;
	private FundamentalValueView fundamental;
	private Market market;
	private MarketView view;
	private Agent mockAgent;

	@Before
	public void defaultSetup() throws IOException {
		setup(
				Keys.FUNDAMENTAL_KAPPA, kappa,
				Keys.FUNDAMENTAL_MEAN, meanValue,
				Keys.FUNDAMENTAL_SHOCK_VAR, 10000,
				Keys.SIMULATION_LENGTH, simulationLength);
	}
	
	public void setup(Object... params) throws IOException {
		sim = MockSim.create(getClass(), Log.Level.NO_LOGGING, ObjectArrays.concat(ObjectArrays.concat(params,
				MarketType.CDA), j.join(Keys.NUM_MARKETS, 1)));
		market = Iterables.getOnlyElement(sim.getMarkets());
		view = market.getPrimaryView();
		fundamental = sim.getFundamentalView(TimeStamp.IMMEDIATE);
		mockAgent = mockAgent();
	}

	@Test
	public void getValuationBasic() {
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				new PrivateValue(1, ImmutableList.of(Price.of(0), Price.of(0))));
		FundamentalValueView fund = sim.getFundamentalView(TimeStamp.ZERO);

		for (int time = 0; time < 100; ++time) {
			// Verify valuation (where PV = 0)
			sim.executeUntil(TimeStamp.of(time));
			assertEquals(fund.getValue(), agent.getValuation(BUY));
			assertEquals(fund.getValue(), agent.getValuation(SELL));
		}
	}

	@Test
	public void getEstimatedValuationBasic() {
		BackgroundAgent agent = backgroundAgent();

		// Verify valuation (where PV = 0)
		Price val = agent.getEstimatedValuation(BUY);
		double kappaToPower = Math.pow(kappa, simulationLength);
		double rHat = fundamental.getValue().doubleValue() * kappaToPower + meanValue * (1 - kappaToPower);
		assertEquals(rHat, val.intValue(), eps);
		val = agent.getEstimatedValuation(SELL);
		assertEquals(rHat, val.intValue(), eps);

		final int iterations = 1000;
		for (int i = 0; i < iterations; i++) {
			sim.executeUntil(TimeStamp.of(i));
			double value = fundamental.getValue().doubleValue();
			double rHatIter = agent.getEstimatedValuation(SELL).doubleValue();
			if (value > meanValue) {
				// rHat should be between current fundamental and mean,
				// but closer to the mean this early in the run.
				assertTrue(rHatIter < value);
				assertTrue(rHatIter >= meanValue);
				assertTrue(Math.abs(rHatIter - meanValue) < Math.abs(rHatIter - value));
			} else if (value < meanValue) {
				assertTrue(rHatIter > value);
				assertTrue(rHatIter <= meanValue);
				assertTrue(Math.abs(rHatIter - meanValue) < Math.abs(rHatIter - value));
			}
		}
	}

	@Test
	public void getValuationConstPV() {
		BackgroundAgent agent = backgroundAgentwithPrivateValue(simple);

		// Verify valuation (current position of 0)
		Price fund = fundamental.getValue();
		assertEquals(fund.intValue() + 10, agent.getValuation(BUY).intValue());
		assertEquals(fund.intValue() + 100, agent.getValuation(SELL).intValue());
	}

	@Test
	public void getEstimatedValuationConstPV() {
		BackgroundAgent agent = backgroundAgentwithPrivateValue(simple);

		// Verify valuation (current position of 0)
		Price val = agent.getEstimatedValuation(BUY);
		double kappaToPower = Math.pow(kappa, simulationLength);
		double rHat = fundamental.getValue().doubleValue() * kappaToPower + meanValue * (1 - kappaToPower);
		assertEquals(rHat + 10, val.intValue(), eps);
		val = agent.getEstimatedValuation(SELL);
		assertEquals(rHat + 100, val.intValue(), eps);
	}

	// TODO Implementation dependent, should remove 
	@Test
	public void getValuationRand() {
		// Testing with randomized values
		BackgroundAgent agent = backgroundAgent();
		PrivateValue pv = agent.privateValue;

		// Get valuation for various positionBalances
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		int pv4 = pv.values.get(4).intValue();
		int pv5 = pv.values.get(5).intValue();
		int pv6 = pv.values.get(6).intValue();
		int pv7 = pv.values.get(7).intValue();
		int pv8 = pv.values.get(8).intValue();
		int pv9 = pv.values.get(9).intValue();

		agent.positionBalance = 3;
		Price fund = fundamental.getValue();
		Price val = agent.getValuation(BUY);
		assertEquals(fund.intValue() + pv8, val.intValue());
		assertEquals(fund.intValue()*2 + pv9 + pv8, 
				agent.getValuation(BUY, 2).intValue());
		val = agent.getValuation(SELL);
		assertEquals(fund.intValue() + pv7, val.intValue());
		assertEquals(fund.intValue()*2 + pv7 + pv6, 
				agent.getValuation(SELL, 2).intValue());

		agent.positionBalance = -2;
		fund = fundamental.getValue();
		val = agent.getValuation(BUY);
		assertEquals(fund.intValue() + pv3, val.intValue());
		assertEquals(fund.intValue()*3 + pv3 + pv4 + pv5, 
				agent.getValuation(BUY, 3).intValue());
		val = agent.getValuation(SELL);
		assertEquals(fund.intValue() + pv2, val.intValue());
		assertEquals(fund.intValue()*3 + pv2 + pv1 + pv0, 
				agent.getValuation(SELL, 3).intValue());
	}

	// XXX Is simulation length supposed to be zero here?
	@Test
	public void getLimitPriceRand() {
		// Testing with randomized values

		BackgroundAgent agent = backgroundAgent(
				Keys.BID_RANGE_MIN, 0,
				Keys.BID_RANGE_MAX, 1000,
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 1000000);
		PrivateValue pv = agent.privateValue;

		// Get valuation for various positionBalances
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		int pv4 = pv.values.get(4).intValue();
		int pv5 = pv.values.get(5).intValue();
		int pv6 = pv.values.get(6).intValue();
		int pv7 = pv.values.get(7).intValue();
		int pv8 = pv.values.get(8).intValue();
		int pv9 = pv.values.get(9).intValue();

		agent.positionBalance = 3;
		Price fund = fundamental.getValue();
		assertEquals(Price.of(((double) fund.intValue()*2 + pv9 + pv8) / 2),
				agent.getLimitPrice(BUY, 2));
		assertEquals(Price.of(((double) fund.intValue()*2 + pv7 + pv6) / 2),
				agent.getLimitPrice(SELL, 2));

		agent.positionBalance = -2;
		fund = fundamental.getValue();
		assertEquals(Price.of(((double) fund.intValue()*3 + pv3 + pv4 + pv5) / 3),
				agent.getLimitPrice(BUY, 3));
		assertEquals(Price.of(((double) fund.intValue()*3 + pv2 + pv1 + pv0) / 3),
				agent.getLimitPrice(SELL, 3));
	}
	
	/** Verify that orders are correctly withdrawn at each re-entry */
	@Test
	public void withdrawTest() {
		BackgroundAgent agent = backgroundAgent(Keys.WITHDRAW_ORDERS, true);

		// execute strategy once; then before reenter, change the position balance
		// that way, when execute strategy again, it won't submit new orders
		agent.activeOrders.add(OrderRecord.create(view, sim.getCurrentTime(), BUY, Price.of(1), 1));
		// verify that order submitted
		assertEquals(1, agent.activeOrders.size());
		agent.agentStrategy();
		// verify that order withdrawn
		assertTrue(agent.activeOrders.isEmpty());
	}

	// TODO Test is implementation dependent and should be changed
	@Test
	public void processTransaction() {
		BackgroundAgent agent = backgroundAgent(
				Keys.BID_RANGE_MIN, 0,
				Keys.BID_RANGE_MAX, 1000,
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 1000000);
		Agent mockAgent = backgroundAgent();

		assertEquals(0, agent.positionBalance);
		assertEquals(0, mockAgent.positionBalance);

		// Creating and adding bids
		submitOrder(agent, BUY, Price.of(110000), 1);
		submitOrder(mockAgent, SELL, Price.of(100000), 1);
		
		assertEquals(1, view.getTransactions().size());
		assertEquals(1, agent.positionBalance);
		assertEquals(-110000, agent.profit);
		assertEquals(-1, mockAgent.positionBalance);
		assertEquals(110000, mockAgent.profit);

		// Check surplus
		int val = agent.getTransactionValuation(BUY, sim.getCurrentTime()).intValue();
		assertEquals(val - 110000, agent.surplus.getValueAtDiscount(Consts.DiscountFactor.NO_DISC), 0.001);
		assertEquals(val - 110000, agent.getPayoff(), 0.001);

		// Check payoff
		agent.liquidateAtPrice(fundamental.getValue());
		Price endTimeFundamental = fundamental.getValue();
		assertEquals(endTimeFundamental.intValue(), agent.getLiquidationProfit());
		assertEquals(val - 110000 + endTimeFundamental.intValue(), agent.getPayoff(), 0.001);
	}

	@Test
	public void processTransactionMultiQuantity() {
		BackgroundAgent agent = backgroundAgent(
				Keys.BID_RANGE_MIN, 0,
				Keys.BID_RANGE_MAX, 1000,
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 1000000);
		Agent mockAgent = backgroundAgent();

		assertEquals(0, agent.positionBalance);
		assertEquals(0, mockAgent.positionBalance);

		// Creating and adding bids
		submitOrder(agent, BUY, Price.of(110000), 3);
		submitOrder(mockAgent, SELL, Price.of(100000), 2);

		// Testing the market for the correct transactions
		assertEquals(1, view.getTransactions().size());
		assertEquals(2, agent.positionBalance);
		assertEquals(-220000, agent.profit);
		assertEquals(-2, mockAgent.positionBalance);
		assertEquals(220000, mockAgent.profit);

		// Check surplus
		int val = agent.getTransactionValuation(BUY, 2, sim.getCurrentTime()).intValue();
		assertEquals(val - 220000, agent.surplus.getValueAtDiscount(Consts.DiscountFactor.NO_DISC), 0.001);
	}

	// TODO Test is implementation dependent and should be changed
	@Test
	public void getTransactionValuationRand() {
		BackgroundAgent agent = backgroundAgent(
				Keys.BID_RANGE_MIN, 0,
				Keys.BID_RANGE_MAX, 1000,
				Keys.MAX_QUANTITY, 5,
				Keys.PRIVATE_VALUE_VAR, 1000000);
		PrivateValue pv = agent.privateValue;
		Agent mockAgent = backgroundAgent();

		// Get valuation for various positionBalances
		int pv0 = pv.values.get(0).intValue();
		int pv1 = pv.values.get(1).intValue();
		int pv2 = pv.values.get(2).intValue();
		int pv3 = pv.values.get(3).intValue();
		int pv4 = pv.values.get(4).intValue();
		int pv5 = pv.values.get(5).intValue(); // +1
		int pv6 = pv.values.get(6).intValue(); // +2
		int pv7 = pv.values.get(7).intValue(); // +3
		int pv8 = pv.values.get(8).intValue(); // +4
		int pv9 = pv.values.get(9).intValue(); // +5

		// Creating and adding bids
		submitOrder(agent, BUY, Price.of(110000), 1);
		sim.executeUntil(TimeStamp.of(1));
		submitOrder(mockAgent, SELL, Price.of(100000), 1);

		// Post-trans balance is 4 or 5 but before the buy transacted it was 3
		agent.positionBalance = 4;
		Price val = agent.getTransactionValuation(BUY, sim.getCurrentTime());
		Price val2 = agent.getTransactionValuation(BUY, sim.getCurrentTime());
		assertEquals(pv8, val.intValue());
		assertEquals(pv8, val2.intValue());
		assertEquals(4, agent.positionBalance);
		agent.positionBalance = 5;
		assertEquals(pv9 + pv8, 
				agent.getTransactionValuation(BUY, 2, sim.getCurrentTime()).intValue());
		assertEquals(5, agent.positionBalance);
		// Post-trans balance is 2 or 1 but before the sell transacted it was 3
		agent.positionBalance = 2;
		val = agent.getTransactionValuation(SELL, sim.getCurrentTime());
		assertEquals(pv7, val.intValue());
		agent.positionBalance = 1;
		assertEquals(pv7 + pv6, 
				agent.getTransactionValuation(SELL, 2, sim.getCurrentTime()).intValue());

		// Post-trans balance is -1 or 1 but before the buy transacted it was -2
		agent.positionBalance = -1;
		val = agent.getTransactionValuation(BUY, sim.getCurrentTime());
		assertEquals(pv3, val.intValue());
		agent.positionBalance = 1;
		assertEquals(pv3 + pv4 + pv5, 
				agent.getTransactionValuation(BUY, 3, sim.getCurrentTime()).intValue());
		// Post-trans balance is -3 or -5 but before the sell transacted it was -2
		agent.positionBalance = -3;
		val = agent.getTransactionValuation(SELL, sim.getCurrentTime());
		assertEquals(pv2, val.intValue());
		agent.positionBalance = -5;
		assertEquals(pv2 + pv1 + pv0, 
				agent.getTransactionValuation(SELL, 3, sim.getCurrentTime()).intValue());
	}

	/**
	 * Verify do not submit order if exceed max position allowed.
	 * 
	 * XXX much of this is tested within ZIAgentTest, may want to move it here
	 */
	@Test
	public void testZIStrat() {
		sim.log(DEBUG, "Testing execution of ZI strategy");
		BackgroundAgent agent = backgroundAgent(zirpProps);

		agent.executeZIStrategy(BUY, 5);
		assertTrue(agent.activeOrders.isEmpty());
		assertEquals(0, agent.positionBalance);
		
		agent.executeZIStrategy(SELL, 5);
		assertTrue(agent.activeOrders.isEmpty());
		assertEquals(0, agent.positionBalance);

		// Test ZI strategy
		agent.executeZIStrategy(BUY, 1);
		assertEquals(1, agent.activeOrders.size());

		// Verify that no other buy orders submitted, because would exceed max position
		agent.positionBalance = 1;
		agent.executeZIStrategy(BUY, 1);
		assertEquals(1, agent.activeOrders.size());
		agent.executeZIStrategy(SELL, 1);
		assertEquals(2, agent.activeOrders.size());
	}
	
	@Test
	public void randZIBuyTest() {
		int min_shade = rand.nextInt(5000); 		//[$0.00, $5.00];
		int max_shade = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];

		BackgroundAgent agent = backgroundAgent(
				Keys.BID_RANGE_MIN, min_shade,
				Keys.BID_RANGE_MAX, max_shade,
				Keys.PRIVATE_VALUE_VAR, 1e8);

		sim.log(DEBUG, "Agent bid range min: %d, maximum: %d", min_shade, max_shade);

		//Execute strategy
		sim.executeUntil(TimeStamp.of(100));
		agent.executeZIStrategy(BUY, 1);

		// Calculate Price Range
		Price fund = fundamental.getValue();
		Price pv = agent.privateValue.getValue(0, BUY);
		
		checkSingleOrderRange(agent.activeOrders,
				Price.of(fund.intValue() + pv.intValue() - max_shade),
				Price.of(fund.intValue() + pv.intValue() - min_shade),
				1);
	}
	
	@Test
	public void randZISellTest() {
		int min_shade = rand.nextInt(5000); 		//[$0.00, $5.00];
		int max_shade = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];

		BackgroundAgent agent = backgroundAgent(
				Keys.BID_RANGE_MIN, min_shade,
				Keys.BID_RANGE_MAX, max_shade,
				Keys.PRIVATE_VALUE_VAR, 1e8);

		sim.log(DEBUG, "Agent bid range min: %d, maximum: %d", min_shade, max_shade);

		//Execute strategy
		sim.executeUntil(TimeStamp.of(100));
		agent.executeZIStrategy(SELL, 1);

		// Calculate Price Range
		Price fund = fundamental.getValue();
		Price pv = agent.privateValue.getValue(0, SELL);
		
		checkSingleOrderRange(agent.activeOrders,
				Price.of(fund.intValue() + pv.intValue() + min_shade),
				Price.of(fund.intValue() + pv.intValue() + max_shade),
				1);
	}
	
	@Test
	public void initialPriceZIBuyTest() {
		sim.log(DEBUG, "Testing ZI submitted bid range is correct");
		BackgroundAgent agent = backgroundAgent();
		agent.executeZIStrategy(BUY, 1);
		/*
		 * Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000
		 * ($10.000) Bid Range = 0, 1000 ($0.00, $1.00) 99.7% of bids should
		 * fall between 100000 +/- (3*10000 + 1000) = 70000, 13000
		 */
		checkSingleOrderRange(agent.activeOrders, Price.of(70000), Price.of(130000), 1);
	}
	
	@Test
	public void initialPriceZISellTest() {
		sim.log(DEBUG, "Testing ZI submitted bid range is correct");
		BackgroundAgent agent = backgroundAgent();
		agent.executeZIStrategy(SELL, 1);
		/*
		 * Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000
		 * ($10.000) Bid Range = 0, 1000 ($0.00, $1.00) 99.7% of bids should
		 * fall between 100000 +/- (3*10000 + 1000) = 70000, 13000
		 */
		checkSingleOrderRange(agent.activeOrders, Price.of(70000), Price.of(130000), 1);
	}
	
	@Test
	public void ziPrivateValueBuyTest() throws IOException {
		setup(
				Keys.FUNDAMENTAL_KAPPA, kappa,
				Keys.FUNDAMENTAL_MEAN, meanValue,
				Keys.FUNDAMENTAL_SHOCK_VAR, 0,
				Keys.SIMULATION_LENGTH, simulationLength);
		
		sim.log(DEBUG, "Testing ZI 100 DummyPrivateValue arguments are correct");
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				new PrivateValue(1, ImmutableList.of(Price.of(10000), Price.of(-10000))));

		sim.executeUntil(TimeStamp.of(100));
		agent.executeZIStrategy(BUY, 1);
		// Buyers always buy at price lower than valuation ($100 + buy PV = $90)
		checkSingleOrderRange(agent.activeOrders, Price.of(89000), Price.of(90000), 1);
	}
	
	@Test
	public void ziPrivateValueSellTest() throws IOException {
		setup(
				Keys.FUNDAMENTAL_KAPPA, kappa,
				Keys.FUNDAMENTAL_MEAN, meanValue,
				Keys.FUNDAMENTAL_SHOCK_VAR, 0,
				Keys.SIMULATION_LENGTH, simulationLength);
		
		sim.log(DEBUG, "Testing ZI 100 DummyPrivateValue arguments are correct");
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				new PrivateValue(1, ImmutableList.of(Price.of(10000), Price.of(-10000))));

		sim.executeUntil(TimeStamp.of(100));
		agent.executeZIStrategy(SELL, 1);
		// Sellers always sell at price higher than valuation ($100 + sell PV = $110)
		checkSingleOrderRange(agent.activeOrders, Price.of(110000), Price.of(111000), 1);
	}

	@Test
	public void testSubmitBuyOrder() {
		// Verify that when submit order, if would exceed position limits, then
		// do not submit the order
		BackgroundAgent agent = backgroundAgent();
		Agent mockAgent = backgroundAgent();

		submitOrder(agent, BUY, Price.of(50), 1);
		submitOrder(mockAgent, SELL, Price.of(40), 1);

		assertEquals(0, agent.activeOrders.size());
		assertEquals(1, agent.positionBalance);

		// Verify that a new buy order can't be submitted
		submitOrder(agent, BUY, Price.of(50), 1);
		assertEquals(0, agent.activeOrders.size());
		assertEquals(1, agent.positionBalance);

		// Verify that a new sell order CAN be submitted
		submitOrder(agent, SELL, Price.of(45), 1);
		assertEquals(1, agent.activeOrders.size());
		assertEquals(1, agent.positionBalance);
	}

	@Test
	public void testSubmitSellOrder() {
		// Verify that when submit order, if would exceed position limits, then
		// do not submit the order
		BackgroundAgent agent = backgroundAgent();
		Agent mockAgent = backgroundAgent();

		submitOrder(mockAgent, BUY, Price.of(50), 1);
		submitOrder(agent, SELL, Price.of(40), 1);

		assertEquals(0, agent.activeOrders.size());
		assertEquals(-1, agent.positionBalance);

		// Verify that a new sell order can't be submitted
		submitOrder(agent, SELL, Price.of(50), 1);
		assertEquals(0, agent.activeOrders.size());
		assertEquals(-1, agent.positionBalance);

		// Verify that a new buy order CAN be submitted
		submitOrder(agent, BUY, Price.of(45), 1);
		assertEquals(1, agent.activeOrders.size());
		assertEquals(-1, agent.positionBalance);
	}

	@Test
	public void testPayoff() throws IOException {
		setup(
				Keys.FUNDAMENTAL_KAPPA, kappa,
				Keys.FUNDAMENTAL_MEAN, meanValue,
				Keys.FUNDAMENTAL_SHOCK_VAR, 0,
				Keys.SIMULATION_LENGTH, simulationLength);
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				new PrivateValue(1, ImmutableList.of(Price.of(1000), Price.of(-2000))),
				Keys.BID_RANGE_MIN, 0,
				Keys.BID_RANGE_MAX, 1000);
		Agent mockAgent = mockAgent();
		
		submitOrder(mockAgent, BUY, Price.of(51000), 1);
		submitOrder(agent, SELL, Price.of(41000), 1);

		assertEquals(0, agent.activeOrders.size());
		assertEquals(-1, agent.positionBalance);
		// background agent sells at 51, surplus from PV is 51-1
		assertEquals(50000, agent.getPayoff(), 0.001);
		// mock agent payoff is just profit
		assertEquals(-51000, mockAgent.getPayoff(), 0.001);


		submitOrder(agent, BUY, Price.of(95000), 1);
		submitOrder(mockAgent, SELL, Price.of(41000), 1);

		assertEquals(0, agent.activeOrders.size());
		assertEquals(0, agent.positionBalance);
		// background agent buys at 95, surplus is 1-95 + (50 from previous)
		assertEquals(50000 + (1000 - 95000), agent.getPayoff(), 0.001);
		// mock agent payoff is just profit
		assertEquals(-51000 + 95000, mockAgent.getPayoff(), 0.001);

		// after liquidate, should have no change because net position is 0
		agent.liquidateAtPrice(fundamental.getValue());
		assertEquals(50000 + (1000 - 95000), agent.getPayoff(), 0.001);
	}

	@Test
	public void testLiquidation() throws IOException {
		// Verify that post-liquidation, payoff includes liquidation
		setup(
				Keys.FUNDAMENTAL_KAPPA, kappa,
				Keys.FUNDAMENTAL_MEAN, meanValue,
				Keys.FUNDAMENTAL_SHOCK_VAR, 0,
				Keys.SIMULATION_LENGTH, simulationLength);
		
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				new PrivateValue(1, ImmutableList.of(Price.of(1000), Price.of(-1000))),
				Keys.BID_RANGE_MIN, 0,
				Keys.BID_RANGE_MAX, 1000);
		Agent mockAgent = backgroundAgent();
		
		submitOrder(mockAgent, BUY, Price.of(51000), 1);
		submitOrder(agent, SELL, Price.of(41000), 1);

		// background agent sells 1 @ 51 (only private value portion counted)
		assertEquals(-1, agent.positionBalance);
		assertEquals(51000, agent.profit);
		assertEquals(51000-1000, agent.getPayoff(), 0.001);

		agent.liquidateAtPrice(fundamental.getValue());
		// background agent liquidates to account for short position of 1
		assertEquals(-100000, agent.getLiquidationProfit());
		assertEquals(50000 - 100000, agent.getPayoff(), 0.001);
	}

	@Test
	public void testMovingFundamentalLiquidation() throws IOException {
		// Verify that post-liquidation, payoff includes liquidation
		setup(
				Keys.FUNDAMENTAL_KAPPA, kappa,
				Keys.FUNDAMENTAL_MEAN, meanValue,
				Keys.FUNDAMENTAL_SHOCK_VAR, 10000000,
				Keys.SIMULATION_LENGTH, simulationLength);

		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				new PrivateValue(1, ImmutableList.of(Price.of(1000), Price.of(-1000))),
				Keys.BID_RANGE_MIN, 0,
				Keys.BID_RANGE_MAX, 1000);
		Agent mockAgent = mockAgent();
		
		submitOrder(mockAgent, BUY, Price.of(51000), 1);
		submitOrder(agent, SELL, Price.of(41000), 1);

		// background agent sells 1 @ 51 (only private value portion counted)
		assertEquals(-1, agent.positionBalance);
		assertEquals(51000, agent.profit);
		assertEquals(51000-1000, agent.getPayoff(), 0.001);

		sim.executeUntil(TimeStamp.of(simulationLength));
		Price endTimeFundamental = fundamental.getValue();
		agent.liquidateAtPrice(endTimeFundamental);
		// background agent liquidates to account for short position of 1
		assertEquals(endTimeFundamental.intValue() * agent.positionBalance, agent.getLiquidationProfit());
		assertEquals(50000 - endTimeFundamental.intValue(), agent.getPayoff(), 0.001);
		assertNotEquals(50000 - 100000, agent.getPayoff(), 0.001);
	}

	@Test
	public void zirpBasicBuyerTest() throws IOException {
		setup(zirpProps);
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote( Price.of(120000), Price.of(130000));
		
		Price val = zirp.getEstimatedValuation(BUY);
		zirp.executeZIRPStrategy(BUY, 1);
		sim.executeImmediate();

		 // Verify that agent does shade since 10000 * 0.75 > val - 130000
		
		checkSingleOrder(zirp.activeOrders, Price.of(val.intValue() - 10000), 1, TimeStamp.ZERO, TimeStamp.ZERO);
	}

	@Test
	public void zirpBasicBuyerTest2() throws IOException {
		setup(zirpProps);
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote( Price.of(80000), Price.of(85000));
		sim.executeUntil(TimeStamp.of(1));

		zirp.executeZIRPStrategy(BUY, 1);
		sim.executeImmediate();
		
		/*
		 * When markup is not sufficient, then don't shade since 10000 * 0.75 <=
		 * val - 85000 Verify that agent's order will trade immediately
		 */
		assertEquals(1, zirp.positionBalance);
		checkSingleTransaction(view.getTransactions(), Price.of(85000), TimeStamp.of(1), 1);
	}

	/** to test what the price of the agent's submitted order is */
	@Test
	public void zirpBasicBuyerTest3() throws IOException {
		setup(zirpProps);
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote(Price.of(80000), Price.of(85000));
		
		Price val = zirp.getEstimatedValuation(BUY);
		zirp.executeZIRPStrategy(BUY, 1);
		sim.executeImmediate();

		// When markup is not sufficient, then don't shade since 10000 * 0.75 <= val - 85000
		checkSingleOrder(zirp.activeOrders, val, 1, TimeStamp.ZERO, TimeStamp.ZERO);
	}
	
	@Test
	public void zirpBasicSellerTest() throws IOException {
		setup(zirpProps);
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote(Price.of(80000), Price.of(85000));

		Price val = zirp.getEstimatedValuation(SELL);
		zirp.executeZIRPStrategy(SELL, 1);
		sim.executeImmediate();
		
		// Verify that agent doesn't shade since 10000 * 0.75 > 80000 - val
		checkSingleOrder(zirp.activeOrders, Price.of(val.intValue() + 10000), 1, TimeStamp.ZERO, TimeStamp.ZERO);
		assertEquals(0, zirp.positionBalance);
	}

	@Test
	public void zirpBasicSellerTest2() throws IOException {
		setup(zirpProps);
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		
		setQuote(Price.of(120000), Price.of(130000));
		sim.executeUntil(TimeStamp.of(1));

		zirp.executeZIRPStrategy(SELL, 1);
		sim.executeImmediate();

		/*
		 * when markup is not sufficient, then don't shade since 10000 * 0.75 <=
		 * 120000 - val Verify that agent's order will trade immediately
		 */
		checkSingleTransaction(view.getTransactions(), Price.of(120000), TimeStamp.of(1), 1);
	}

	/** to test what the price of the agent's submitted order is 
	 * @throws IOException */
	@Test
	public void zirpBasicSellerTest3() throws IOException {
		setup(zirpProps);
		
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote(Price.of(120000), Price.of(130000));
		
		Price val = zirp.getEstimatedValuation(SELL);
		zirp.executeZIRPStrategy(SELL, 1);
		sim.executeImmediate();
		
		// when markup is not sufficient, then don't shade since 10000 * 0.75 <= 120000 - val
		checkSingleOrder(zirp.activeOrders, val, 1, TimeStamp.ZERO, TimeStamp.ZERO);
	}
	
	/** Test that returns empty if exceed max position */
	@Test
	public void testZIRPStrat() {
		BackgroundAgent zirp = backgroundAgent(Keys.MAX_QUANTITY, 2);

		zirp.executeZIRPStrategy(BUY, 5);
		assertTrue(zirp.activeOrders.isEmpty());
		assertEquals(0, zirp.positionBalance);
		zirp.executeZIRPStrategy(SELL, 5);
		assertTrue(zirp.activeOrders.isEmpty());
		assertEquals(0, zirp.positionBalance);

		// Test ZIRP strategy
		zirp.executeZIRPStrategy(BUY, 1);
		assertEquals(1, zirp.activeOrders.size());
	}

	// TODO: check getEstimatedValuation/Fundamental
	
	@Test
	public void executionTimePostTest() {
		BackgroundAgent agent1 = backgroundAgent();
		BackgroundAgent agent2 = backgroundAgent();
		
		submitOrder(agent1, BUY, Price.of(50), 4);
		sim.executeUntil(TimeStamp.of(1));
		submitOrder(agent2, SELL, Price.of(50), 1);
		sim.executeUntil(TimeStamp.of(2));
		submitOrder(agent2, SELL, Price.of(50), 2);
		sim.executeUntil(TimeStamp.of(3));
		submitOrder(mockAgent, SELL, Price.of(50), 1); // Won't count towards execution times
		
		/*
		 * 1 executes with 1 delay
		 * 2 execute with 2 delay
		 * 1 executes with 3 delay
		 * 3 execute with 0 delay
		 */
		assertEquals(8, sim.getStats().getSummaryStats().get(Stats.EXECUTION_TIME).sum(), eps);
		assertEquals(1.1428571428571428, sim.getStats().getSummaryStats().get(Stats.EXECUTION_TIME).mean(), eps);
	}
	
	@Test
	public void controlRandPrivateValueTest() {
		int n = 10;
		SummStats pvMean = SummStats.on();
		for (int i = 0; i < n; ++i)
			pvMean.add(backgroundAgent().privateValue.getMean().doubleValue());
		assertEquals(pvMean.mean(), sim.getStats().getSummaryStats().get(Stats.CONTROL_PRIVATE_VALUE).mean(), eps);
	}
	
	@Test
	public void controlPrivateValueTest() {
		backgroundAgentwithPrivateValue(new PrivateValue(1, ImmutableList.of(Price.of(0), Price.of(0))));
		backgroundAgentwithPrivateValue(new PrivateValue(1, ImmutableList.of(Price.of(10), Price.of(10))));
		
		assertEquals(5, sim.getStats().getSummaryStats().get(Stats.CONTROL_PRIVATE_VALUE).mean(), eps);
	}
	
	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i < 100; ++i) {
			defaultSetup();
			getValuationRand();
			defaultSetup();
			getLimitPriceRand();
			defaultSetup();
			getTransactionValuationRand();
			defaultSetup();
			randZIBuyTest();
			defaultSetup();
			randZISellTest();
			defaultSetup();
			initialPriceZIBuyTest();
			defaultSetup();
			initialPriceZISellTest();
			defaultSetup();
			ziPrivateValueBuyTest();
			defaultSetup();
			ziPrivateValueSellTest();
			defaultSetup();
			zirpBasicBuyerTest();
			defaultSetup();
			zirpBasicBuyerTest2();
			defaultSetup();
			zirpBasicBuyerTest3();
			defaultSetup();
			zirpBasicSellerTest();
			defaultSetup();
			zirpBasicSellerTest2();
			defaultSetup();
			zirpBasicSellerTest3();
			defaultSetup();
			controlRandPrivateValueTest();
		}
	}
	
	private void setQuote(Price bid, Price ask) {
		submitOrder(mockAgent, BUY, bid, 1);
		submitOrder(mockAgent, SELL, ask, 1);
	}

	private OrderRecord submitOrder(Agent agent, OrderType buyOrSell, Price price, int quantity) {
		OrderRecord order = agent.submitOrder(view, buyOrSell, price, quantity);
		sim.executeImmediate();
		return order;
	}

	private BackgroundAgent backgroundAgentwithPrivateValue(PrivateValue privateValue, Object... pairs) {
		return new BackgroundAgent(sim, TimeStamp.ZERO, market, privateValue, rand, Props.withDefaults(defaults, pairs)) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestAgent " + id; }
		};
	}

	private BackgroundAgent backgroundAgent(Object... pairs) {
		return new BackgroundAgent(sim, TimeStamp.ZERO, market, rand, Props.withDefaults(defaults, pairs)) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
	
	private Agent mockAgent() {
		return new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}

}
