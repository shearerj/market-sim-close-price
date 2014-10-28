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

import java.io.IOException;
import java.util.List;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.AcceptableProfitFrac;
import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.BidRangeMax;
import systemmanager.Keys.BidRangeMin;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.WithdrawOrders;
import systemmanager.MockSim;
import utils.Rands;
import utils.SummStats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import data.FundamentalValue.FundamentalValueView;
import data.Props;
import data.Stats;
import entity.agent.position.ListPrivateValue;
import entity.agent.position.PrivateValues;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class BackgroundAgentTest {
	
	private static final Random rand = new Random();
	private static final List<Price> simple = ImmutableList.of(Price.of(100), Price.of(10));
	private static final double eps = 0.05;
	private static final double kappa = 0.2;
	private static final int meanValue = 100000;
	private static final int simulationLength = 60000;
	private static final Props defaultSim = Props.fromPairs(
			FundamentalKappa.class, kappa,
			FundamentalMean.class, meanValue,
			FundamentalShockVar.class, 10000d,
			SimLength.class, simulationLength);
	private static final Props defaults = Props.fromPairs(
			PrivateValueVar.class, 1e8,
			BidRangeMin.class, 0,
			BidRangeMax.class, 1000);
	private static Props zirpProps = Props.builder()
			.put(BackgroundReentryRate.class, 0d)
			.put(MaxQty.class, 2)
			.put(PrivateValueVar.class, 100d)
			.put(BidRangeMin.class, 10000)
			.put(BidRangeMax.class, 10000)
			.put(SimLength.class, simulationLength)
			.put(FundamentalKappa.class, kappa)
			.put(FundamentalMean.class, meanValue)
			.put(FundamentalShockVar.class, 0d)
			.put(WithdrawOrders.class, true)
			.put(AcceptableProfitFrac.class, 0.75)
			.build();

	private MockSim sim;
	private FundamentalValueView fundamental;
	private Market market;
	private MarketView view;
	private Agent mockAgent;

	@Before
	public void defaultSetup() throws IOException {
		setup(Props.fromPairs());
	}
	
	public void setup(Props params) throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 1, Props.merge(defaultSim, params));
		market = Iterables.getOnlyElement(sim.getMarkets());
		view = market.getPrimaryView();
		fundamental = sim.getFundamentalView(TimeStamp.IMMEDIATE);
		mockAgent = mockAgent();
	}

	@Test
	public void getValuationBasic() {
		BackgroundAgent agent = backgroundAgentwithPrivateValue(ImmutableList.of(Price.of(0), Price.of(0)));
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
		ImmutableList.Builder<Price> builder = ImmutableList.builder();
		for (int i = 0; i < 10; ++i)
			builder.add(Price.of(Rands.nextGaussian(rand, 0, 1e8)));
		List<Price> pv = builder.build();
		
		BackgroundAgent agent = backgroundAgentwithPrivateValue(pv);

		// Get valuation for various positionBalances
		int pv0 = pv.get(0).intValue();
		int pv1 = pv.get(1).intValue();
		int pv2 = pv.get(2).intValue();
		int pv3 = pv.get(3).intValue();
		int pv4 = pv.get(4).intValue();
		int pv5 = pv.get(5).intValue();
		int pv6 = pv.get(6).intValue();
		int pv7 = pv.get(7).intValue();
		int pv8 = pv.get(8).intValue();
		int pv9 = pv.get(9).intValue();

		setPosition(agent, 3);
		Price fund = fundamental.getValue();
		Price val = agent.getValuation(BUY);
		assertEquals(fund.intValue() + pv8, val.intValue());
		assertEquals(fund.intValue()*2 + pv9 + pv8, 
				agent.getValuation(BUY, 2).intValue());
		val = agent.getValuation(SELL);
		assertEquals(fund.intValue() + pv7, val.intValue());
		assertEquals(fund.intValue()*2 + pv7 + pv6, 
				agent.getValuation(SELL, 2).intValue());

		setPosition(agent, -2);
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
		ImmutableList.Builder<Price> builder = ImmutableList.builder();
		for (int i = 0; i < 10; ++i)
			builder.add(Price.of(Rands.nextGaussian(rand, 0, 1000000)));
		List<Price> pv = builder.build();
		
		BackgroundAgent agent = backgroundAgentwithPrivateValue(pv, Props.fromPairs(
				BidRangeMin.class, 0,
				BidRangeMax.class, 1000,
				MaxQty.class, 5));

		// Get valuation for various positionBalances
		int pv0 = pv.get(0).intValue();
		int pv1 = pv.get(1).intValue();
		int pv2 = pv.get(2).intValue();
		int pv3 = pv.get(3).intValue();
		int pv4 = pv.get(4).intValue();
		int pv5 = pv.get(5).intValue();
		int pv6 = pv.get(6).intValue();
		int pv7 = pv.get(7).intValue();
		int pv8 = pv.get(8).intValue();
		int pv9 = pv.get(9).intValue();

		setPosition(agent, 3);
		Price fund = fundamental.getValue();
		assertEquals(Price.of(((double) fund.intValue()*2 + pv9 + pv8) / 2),
				agent.getLimitPrice(BUY, 2));
		assertEquals(Price.of(((double) fund.intValue()*2 + pv7 + pv6) / 2),
				agent.getLimitPrice(SELL, 2));

		setPosition(agent, -2);
		fund = fundamental.getValue();
		assertEquals(Price.of(((double) fund.intValue()*3 + pv3 + pv4 + pv5) / 3),
				agent.getLimitPrice(BUY, 3));
		assertEquals(Price.of(((double) fund.intValue()*3 + pv2 + pv1 + pv0) / 3),
				agent.getLimitPrice(SELL, 3));
	}
	
	/** Verify that orders are correctly withdrawn at each re-entry */
	@Test
	public void withdrawTest() {
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(WithdrawOrders.class, true));

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
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(
				BidRangeMin.class, 0,
				BidRangeMax.class, 1000,
				MaxQty.class, 5,
				PrivateValueVar.class, 1000000d));
		Agent mockAgent = backgroundAgent();

		assertEquals(0, agent.getPosition());
		assertEquals(0, mockAgent.getPosition());

		// Creating and adding bids
		submitOrder(agent, BUY, Price.of(110000), 1);
		submitOrder(mockAgent, SELL, Price.of(100000), 1);
		
		assertEquals(1, view.getTransactions().size());
		assertEquals(1, agent.getPosition());
		assertEquals(-110000, agent.getProfit());
		assertEquals(-1, mockAgent.getPosition());
		assertEquals(110000, mockAgent.getProfit());

		// Check surplus
		int val = agent.getValuation(BUY).intValue();
		assertEquals(val - 110000, agent.getPayoff(), 0.001);

		// Check payoff
		agent.liquidateAtPrice(fundamental.getValue());
		Price endTimeFundamental = fundamental.getValue();
		assertEquals(endTimeFundamental.intValue(), agent.getProfit());
		assertEquals(val - 110000 + endTimeFundamental.intValue(), agent.getPayoff(), 0.001);
	}

	@Test
	public void processTransactionMultiQuantity() {
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(
				BidRangeMin.class, 0,
				BidRangeMax.class, 1000,
				MaxQty.class, 5,
				PrivateValueVar.class, 1000000d));
		Agent mockAgent = backgroundAgent();

		assertEquals(0, agent.getPosition());
		assertEquals(0, mockAgent.getPosition());

		// Creating and adding bids
		submitOrder(agent, BUY, Price.of(110000), 3);
		submitOrder(mockAgent, SELL, Price.of(100000), 2);

		// Testing the market for the correct transactions
		assertEquals(1, view.getTransactions().size());
		assertEquals(2, agent.getPosition());
		assertEquals(-220000, agent.getProfit());
		assertEquals(-2, mockAgent.getPosition());
		assertEquals(220000, mockAgent.getProfit());

		// Check surplus
		int val = agent.getValuation(agent.getPosition(), BUY).intValue();
		assertEquals(val - 220000, agent.getPayoff(), 0.001);
	}

	// TODO Test is implementation dependent and should be changed
	@Test
	public void getTransactionValuationRand() {
		ImmutableList.Builder<Price> builder = ImmutableList.builder();
		for (int i = 0; i < 10; ++i)
			builder.add(Price.of(Rands.nextGaussian(rand, 0, 1000000)));
		List<Price> pv = builder.build();
		
		BackgroundAgent agent = backgroundAgentwithPrivateValue(pv, Props.fromPairs(
				BidRangeMin.class, 0,
				BidRangeMax.class, 1000,
				MaxQty.class, 5));
		Agent mockAgent = backgroundAgent();

		// Get valuation for various positionBalances
		int pv0 = pv.get(0).intValue();
		int pv1 = pv.get(1).intValue();
		int pv2 = pv.get(2).intValue();
		int pv3 = pv.get(3).intValue();
		int pv4 = pv.get(4).intValue();
		int pv5 = pv.get(5).intValue(); // +1
		int pv6 = pv.get(6).intValue(); // +2
		int pv7 = pv.get(7).intValue(); // +3
		int pv8 = pv.get(8).intValue(); // +4
		int pv9 = pv.get(9).intValue(); // +5

		// Creating and adding bids
		submitOrder(agent, BUY, Price.of(110000), 1);
		sim.executeUntil(TimeStamp.of(1));
		submitOrder(mockAgent, SELL, Price.of(100000), 1);

		// Post-trans balance is 4 or 5 but before the buy transacted it was 3
		setPosition(agent, 4);
		Price val = agent.getValuation(BUY);
		assertEquals(pv8, val.intValue());
		assertEquals(4, agent.getPosition());
		setPosition(agent, 5);
		assertEquals(pv9 + pv8, agent.getValuation(2, BUY).intValue());
		assertEquals(5, agent.getPosition());
		// Post-trans balance is 2 or 1 but before the sell transacted it was 3
		setPosition(agent, 2);
		val = agent.getValuation(SELL);
		assertEquals(pv7, val.intValue());
		setPosition(agent, 1);
		assertEquals(pv7 + pv6, agent.getValuation(2, SELL).intValue());

		// Post-trans balance is -1 or 1 but before the buy transacted it was -2
		setPosition(agent, -1);
		val = agent.getValuation(BUY);
		assertEquals(pv3, val.intValue());
		setPosition(agent, 1);
		assertEquals(pv3 + pv4 + pv5, agent.getValuation(3, BUY).intValue());
		// Post-trans balance is -3 or -5 but before the sell transacted it was -2
		setPosition(agent, -3);
		val = agent.getValuation(SELL);
		assertEquals(pv2, val.intValue());
		setPosition(agent, -5);
		assertEquals(pv2 + pv1 + pv0, agent.getValuation(3, SELL).intValue());
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
		assertEquals(0, agent.getPosition());
		
		agent.executeZIStrategy(SELL, 5);
		assertTrue(agent.activeOrders.isEmpty());
		assertEquals(0, agent.getPosition());

		// Test ZI strategy
		agent.executeZIStrategy(BUY, 1);
		assertEquals(1, agent.activeOrders.size());

		// Verify that no other buy orders submitted, because would exceed max position
		agent.withdrawAllOrders();
		sim.executeImmediate();
		
		setPosition(agent, 1);
		agent.executeZIStrategy(BUY, 1);
		assertEquals(1, agent.activeOrders.size());
		agent.executeZIStrategy(SELL, 1);
		assertEquals(2, agent.activeOrders.size());
	}
	
	@Test
	public void randZIBuyTest() {
		int min_shade = rand.nextInt(5000); 		//[$0.00, $5.00];
		int max_shade = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];

		BackgroundAgent agent = backgroundAgent(Props.fromPairs(
				BidRangeMin.class, min_shade,
				BidRangeMax.class, max_shade,
				PrivateValueVar.class, 1e8));

		sim.log(DEBUG, "Agent bid range min: %d, maximum: %d", min_shade, max_shade);

		//Execute strategy
		sim.executeUntil(TimeStamp.of(100));
		agent.executeZIStrategy(BUY, 1);

		// Calculate Price Range
		Price fund = fundamental.getValue();
		Price pv = agent.getValuation(BUY);
		
		checkSingleOrderRange(agent.activeOrders,
				Price.of(fund.intValue() + pv.intValue() - max_shade),
				Price.of(fund.intValue() + pv.intValue() - min_shade),
				1);
	}
	
	@Test
	public void randZISellTest() {
		int min_shade = rand.nextInt(5000); 		//[$0.00, $5.00];
		int max_shade = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];

		BackgroundAgent agent = backgroundAgent(Props.fromPairs(
				BidRangeMin.class, min_shade,
				BidRangeMax.class, max_shade,
				PrivateValueVar.class, 1e8));

		sim.log(DEBUG, "Agent bid range min: %d, maximum: %d", min_shade, max_shade);

		//Execute strategy
		sim.executeUntil(TimeStamp.of(100));
		agent.executeZIStrategy(SELL, 1);

		// Calculate Price Range
		Price fund = fundamental.getValue();
		Price pv = agent.getValuation(SELL);
		
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
		setup(Props.fromPairs(FundamentalShockVar.class, 0d));
		
		sim.log(DEBUG, "Testing ZI 100 DummyPrivateValue arguments are correct");
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				ImmutableList.of(Price.of(10000), Price.of(-10000)));

		sim.executeUntil(TimeStamp.of(100));
		agent.executeZIStrategy(BUY, 1);
		// Buyers always buy at price lower than valuation ($100 + buy PV = $90)
		checkSingleOrderRange(agent.activeOrders, Price.of(89000), Price.of(90000), 1);
	}
	
	@Test
	public void ziPrivateValueSellTest() throws IOException {
		setup(Props.fromPairs(FundamentalShockVar.class, 0d));
		
		sim.log(DEBUG, "Testing ZI 100 DummyPrivateValue arguments are correct");
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				ImmutableList.of(Price.of(10000), Price.of(-10000)));

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
		assertEquals(1, agent.getPosition());

		// Verify that a new buy order can't be submitted
		submitOrder(agent, BUY, Price.of(50), 1);
		assertEquals(0, agent.activeOrders.size());
		assertEquals(1, agent.getPosition());

		// Verify that a new sell order CAN be submitted
		submitOrder(agent, SELL, Price.of(45), 1);
		assertEquals(1, agent.activeOrders.size());
		assertEquals(1, agent.getPosition());
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
		assertEquals(-1, agent.getPosition());

		// Verify that a new sell order can't be submitted
		submitOrder(agent, SELL, Price.of(50), 1);
		assertEquals(0, agent.activeOrders.size());
		assertEquals(-1, agent.getPosition());

		// Verify that a new buy order CAN be submitted
		submitOrder(agent, BUY, Price.of(45), 1);
		assertEquals(1, agent.activeOrders.size());
		assertEquals(-1, agent.getPosition());
	}

	@Test
	public void testPayoff() throws IOException {
		setup(Props.fromPairs(FundamentalShockVar.class, 0d));
		BackgroundAgent agent = backgroundAgentwithPrivateValue(ImmutableList.of(Price.of(1000), Price.of(-2000)),
				Props.fromPairs(BidRangeMin.class, 0, BidRangeMax.class, 1000));
		Agent mockAgent = mockAgent();
		
		submitOrder(mockAgent, BUY, Price.of(51000), 1);
		submitOrder(agent, SELL, Price.of(41000), 1);

		assertEquals(0, agent.activeOrders.size());
		assertEquals(-1, agent.getPosition());
		// background agent sells at 51, surplus from PV is 51-1
		assertEquals(50000, agent.getPayoff(), 0.001);
		// mock agent payoff is just profit
		assertEquals(-51000, mockAgent.getPayoff(), 0.001);


		submitOrder(agent, BUY, Price.of(95000), 1);
		submitOrder(mockAgent, SELL, Price.of(41000), 1);

		assertEquals(0, agent.activeOrders.size());
		assertEquals(0, agent.getPosition());
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
		setup(Props.fromPairs(FundamentalShockVar.class, 0d));
		
		BackgroundAgent agent = backgroundAgentwithPrivateValue(ImmutableList.of(Price.of(1000), Price.of(-1000)),
				Props.fromPairs(BidRangeMin.class, 0, BidRangeMax.class, 1000));
		Agent mockAgent = backgroundAgent();
		
		submitOrder(mockAgent, BUY, Price.of(51000), 1);
		submitOrder(agent, SELL, Price.of(41000), 1);

		// background agent sells 1 @ 51 (only private value portion counted)
		assertEquals(-1, agent.getPosition());
		assertEquals(51000, agent.getProfit());
		assertEquals(51000-1000, agent.getPayoff(), 0.001);

		agent.liquidateAtPrice(fundamental.getValue());
		// background agent liquidates to account for short position of 1
		assertEquals(-100000, agent.getProfit());
		assertEquals(50000 - 100000, agent.getPayoff(), 0.001);
	}

	@Test
	public void testMovingFundamentalLiquidation() throws IOException {
		// Verify that post-liquidation, payoff includes liquidation
		setup(Props.fromPairs(FundamentalShockVar.class, 10000000d));

		BackgroundAgent agent = backgroundAgentwithPrivateValue(ImmutableList.of(Price.of(1000), Price.of(-1000)),
				Props.fromPairs(BidRangeMin.class, 0, BidRangeMax.class, 1000));
		Agent mockAgent = mockAgent();
		
		submitOrder(mockAgent, BUY, Price.of(51000), 1);
		submitOrder(agent, SELL, Price.of(41000), 1);

		// background agent sells 1 @ 51 (only private value portion counted)
		assertEquals(-1, agent.getPosition());
		assertEquals(51000, agent.getProfit());
		assertEquals(51000-1000, agent.getPayoff(), 0.001);

		sim.executeUntil(TimeStamp.of(simulationLength));
		Price endTimeFundamental = fundamental.getValue();
		agent.liquidateAtPrice(endTimeFundamental);
		// background agent liquidates to account for short position of 1
		assertEquals(endTimeFundamental.intValue() * agent.getPosition(), agent.getProfit());
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
		assertEquals(1, zirp.getPosition());
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
		assertEquals(0, zirp.getPosition());
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
		BackgroundAgent zirp = backgroundAgent(Props.fromPairs(MaxQty.class, 2));

		zirp.executeZIRPStrategy(BUY, 5);
		assertTrue(zirp.activeOrders.isEmpty());
		assertEquals(0, zirp.getPosition());
		zirp.executeZIRPStrategy(SELL, 5);
		assertTrue(zirp.activeOrders.isEmpty());
		assertEquals(0, zirp.getPosition());

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
			pvMean.add(backgroundAgent().getPrivateValueMean().doubleValue());
		assertEquals(pvMean.mean(), sim.getStats().getSummaryStats().get(Stats.CONTROL_PRIVATE_VALUE).mean(), eps);
	}
	
	@Test
	public void controlPrivateValueTest() {
		backgroundAgentwithPrivateValue(ImmutableList.of(Price.of(0), Price.of(0)));
		backgroundAgentwithPrivateValue(ImmutableList.of(Price.of(10), Price.of(10)));
		
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
	
	private void setPosition(Agent agent, int position) {
		int quantity = position - agent.getPosition();
		if (quantity == 0)
			return;
		OrderType type = quantity > 0 ? BUY : SELL;
		submitOrder(agent, type, Price.ZERO, Math.abs(quantity));
		submitOrder(mockAgent, type == BUY ? SELL : BUY, Price.ZERO, quantity);
		assertEquals(position, agent.getPosition());
	}

	private BackgroundAgent backgroundAgentwithPrivateValue(List<Price> privateValue, Props props) {
		return new BackgroundAgent(sim, new ListPrivateValue(privateValue) { private static final long serialVersionUID = 1L; },
				market,
				rand, Props.merge(defaults, props)) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
	
	private BackgroundAgent backgroundAgentwithPrivateValue(List<Price> privateValue) {
		return backgroundAgentwithPrivateValue(privateValue, Props.fromPairs());
	}

	private BackgroundAgent backgroundAgent(Props props) {
		return new BackgroundAgent(sim, market, rand, Props.merge(defaults, props)) {
			private static final long serialVersionUID = 1L;
			@Override public String toString() { return "TestAgent " + id; }
		};
	}
	
	private BackgroundAgent backgroundAgent() {
		return backgroundAgent(Props.fromPairs());
	}
	
	private Agent mockAgent() {
		return new Agent(sim, PrivateValues.zero(), TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
			@Override public String toString() { return "TestAgent " + id; }
		};
	}

}
