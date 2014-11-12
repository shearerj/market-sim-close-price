package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static utils.Tests.assertOptionalRange;
import static utils.Tests.assertQuote;
import static utils.Tests.*;

import java.util.concurrent.atomic.AtomicInteger;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.AcceptableProfitThreshold;
import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.RMax;
import systemmanager.Keys.RMin;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.SimLength;
import systemmanager.Keys.Withdraw;
import utils.Mock;
import utils.Rand;
import utils.SummStats;

import com.google.common.collect.ImmutableList;

import data.FundamentalValue;
import data.FundamentalValue.FundamentalValueView;
import data.Props;
import data.Stats;
import entity.agent.position.ListPrivateValue;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.market.Quote;
import event.Activity;
import event.EventQueue;
import event.TimeStamp;
import event.Timeline;
import fourheap.Order.OrderType;

public class BackgroundAgentTest {
	
	private static final Rand rand = Rand.create();
	private static final ListPrivateValue simple = ListPrivateValue.create(ImmutableList.of(Price.of(100), Price.of(10)));
	private static final double eps = 1e-6;
	private static final double kappa = 0.2315;
	private static final int meanValue = 98765;
	private static final double variance = 12345;
	private static final int simulationLength = 58972;
	private static final Props defaults = Props.fromPairs(
			FundamentalKappa.class, kappa,
			FundamentalMean.class, meanValue,
			FundamentalShockVar.class, variance,
			SimLength.class, simulationLength,
			ArrivalRate.class, 0d);
	private static final Props zirpProps = Props.builder()
			.put(BackgroundReentryRate.class, 0d)
			.put(MaxQty.class, 2)
			.put(PrivateValueVar.class, 100d)
			.put(RMin.class, 10000)
			.put(RMax.class, 10000)
			.put(SimLength.class, simulationLength)
			.put(FundamentalKappa.class, kappa)
			.put(FundamentalMean.class, meanValue)
			.put(FundamentalShockVar.class, 0d)
			.put(Withdraw.class, true)
			.put(AcceptableProfitThreshold.class, 0.75)
			.build();

	private Timeline timeline;
	private FundamentalValue fundamental;
	private FundamentalValueView fund;
	private Market market;
	private MarketView view;
	private Agent mockAgent;

	@Before
	public void setup() {
		timeline = Mock.timeline;
		fundamental = FundamentalValue.create(Mock.stats, timeline, kappa, meanValue, variance, rand);
		fund = fundamental.getView(TimeStamp.ZERO);
		market = Mock.market();
		view = market.getPrimaryView();
		mockAgent = Mock.agent();
	}

	/** Verify valuation (where PV = 0) */
	@Test
	public void getValuationBasic() {
		AtomicInteger time = fundamentalSetup();
		BackgroundAgent agent = backgroundAgentwithPrivateValue(ListPrivateValue.create(ImmutableList.<Price> of()));

		for (int t = 0; t < simulationLength; ++t) {
			time.set(t);
			assertEquals(fundamental.getValueAt(TimeStamp.of(t)), agent.getValuation(BUY));
			assertEquals(fundamental.getValueAt(TimeStamp.of(t)), agent.getValuation(SELL));
		}
	}

	@Test
	public void getEstimatedValuationBasic() {
		AtomicInteger time = fundamentalSetup();
		BackgroundAgent agent = backgroundAgentwithPrivateValue(ListPrivateValue.create(ImmutableList.<Price> of()));

		double kappaToPower = Math.pow(kappa, simulationLength);
		double rHat = fund.getValue().doubleValue() * kappaToPower + meanValue * (1 - kappaToPower);
		
		// Verify valuation (where PV = 0)
		assertEquals(rHat, agent.getEstimatedValuation(BUY).doubleValue(), eps);
		assertEquals(rHat, agent.getEstimatedValuation(SELL).doubleValue(), eps);

		for (int t = 0; t < simulationLength; t++) {
			time.set(t);
			double value = fund.getValue().doubleValue();
			rHat = agent.getEstimatedValuation(SELL).doubleValue();
			assertTrue(Math.abs(rHat - meanValue) <= Math.abs(value - meanValue));
		}
	}

	@Test
	public void getValuationConstPV() {
		BackgroundAgent agent = backgroundAgentwithPrivateValue(simple);

		// Verify valuation (current position of 0)
		Price fundPrice = fund.getValue();
		assertEquals(fundPrice.intValue() + 10, agent.getValuation(BUY).intValue());
		assertEquals(fundPrice.intValue() + 100, agent.getValuation(SELL).intValue());
	}

	@Test
	public void getEstimatedValuationConstPV() {
		BackgroundAgent agent = backgroundAgentwithPrivateValue(simple);

		// Verify valuation (current position of 0)
		double kappaToPower = Math.pow(kappa, simulationLength);
		double rHat = fund.getValue().doubleValue() * kappaToPower + meanValue * (1 - kappaToPower);
		assertEquals(rHat + 10, agent.getEstimatedValuation(BUY).doubleValue(), eps);
		assertEquals(rHat + 100, agent.getEstimatedValuation(SELL).doubleValue(), eps);
	}

	// TODO Implementation dependent, should remove 
	@Test
	public void getValuationRand() {
		BackgroundAgent agent = backgroundAgent();
		
		setPosition(agent, 4);
		Price buy4 = agent.getValuation(BUY);
		setPosition(agent, 2);
		Price sell2 = agent.getValuation(SELL);
		
		setPosition(agent, 3);
		assertEquals(Price.of(agent.getValuation(BUY).intValue() + buy4.intValue()), agent.getValuation(BUY, 2));
		assertEquals(Price.of(agent.getValuation(SELL).intValue() + sell2.intValue()), agent.getValuation(SELL, 2));
		
		setPosition(agent, 0);
		Price buy0 = agent.getValuation(BUY);
		setPosition(agent, -1);
		Price buyn1 = agent.getValuation(BUY);
		setPosition(agent, -3);
		Price selln3 = agent.getValuation(SELL);
		setPosition(agent, -4);
		Price selln4 = agent.getValuation(SELL);
		
		setPosition(agent, -2);
		assertEquals(Price.of(agent.getValuation(BUY).intValue() + buyn1.intValue()), agent.getValuation(BUY, 2));
		assertEquals(Price.of(agent.getValuation(SELL).intValue() + selln3.intValue()), agent.getValuation(SELL, 2));
		assertEquals(Price.of(agent.getValuation(BUY).intValue() + buyn1.intValue() + buy0.intValue()), agent.getValuation(BUY, 3));
		assertEquals(Price.of(agent.getValuation(SELL).intValue() + selln3.intValue() + selln4.intValue()), agent.getValuation(SELL, 3));
	}

	@Test
	public void getLimitPriceRand() {
		BackgroundAgent agent = backgroundAgent();
		
		setPosition(agent, 4);
		Price buy4 = agent.getValuation(BUY);
		setPosition(agent, 2);
		Price sell2 = agent.getValuation(SELL);
		
		setPosition(agent, 3);
		assertEquals(Price.of((agent.getValuation(BUY).doubleValue() + buy4.doubleValue()) / 2), agent.getLimitPrice(BUY, 2));
		assertEquals(Price.of((agent.getValuation(SELL).doubleValue() + sell2.doubleValue()) / 2), agent.getLimitPrice(SELL, 2));

		setPosition(agent, 0);
		Price buy0 = agent.getValuation(BUY);
		setPosition(agent, -1);
		Price buyn1 = agent.getValuation(BUY);
		setPosition(agent, -3);
		Price selln3 = agent.getValuation(SELL);
		setPosition(agent, -4);
		Price selln4 = agent.getValuation(SELL);
		
		setPosition(agent, -2);
		assertEquals(Price.of((agent.getValuation(BUY).doubleValue() + buyn1.doubleValue()) / 2), agent.getLimitPrice(BUY, 2));
		assertEquals(Price.of((agent.getValuation(SELL).doubleValue() + selln3.doubleValue()) / 2), agent.getLimitPrice(SELL, 2));
		assertEquals(Price.of((agent.getValuation(BUY).doubleValue() + buyn1.doubleValue() + buy0.doubleValue()) / 3), agent.getLimitPrice(BUY, 3));
		assertEquals(Price.of((agent.getValuation(SELL).doubleValue() + selln3.doubleValue() + selln4.doubleValue()) / 3), agent.getLimitPrice(SELL, 3));
	}
	
	/** Verify that orders are correctly withdrawn at each re-entry */
	@Test
	public void withdrawTest() {
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(Withdraw.class, true));

		agent.submitOrder(BUY, Price.of(1), 1);
		assertQuote(view.getQuote(), Price.of(1), 1, null, 0);
		
		agent.agentStrategy(); // verify that order withdrawn
		assertQuote(view.getQuote(), null, 0, null, 0);
	}

	@Test
	public void processTransaction() {
		BackgroundAgent agent = backgroundAgent();

		assertEquals(0, agent.getPosition());
		assertEquals(0, mockAgent.getPosition());
		
		Price val = agent.getPrivateValue(BUY);

		// Creating and adding bids
		agent.submitOrder(BUY, Price.of(110000), 1);
		mockAgent.submitOrder(view, SELL, Price.of(110000), 1);
		
		assertEquals(1, view.getTransactions().size());
		assertEquals(1, agent.getPosition());
		assertEquals(-110000, agent.getProfit());
		assertEquals(-1, mockAgent.getPosition());
		assertEquals(110000, mockAgent.getProfit());

		// Check surplus
		assertEquals(val.intValue() - 110000, agent.getPayoff(), 0.001);

		// Check payoff
		Price endTimeFundamental = fundamental.getValueAt(TimeStamp.of(simulationLength));
		agent.liquidateAtPrice(endTimeFundamental);
		
		assertEquals(endTimeFundamental.intValue() - 110000, agent.getProfit());
		assertEquals(val.intValue() - 110000 + endTimeFundamental.intValue(), agent.getPayoff(), 0.001);
	}

	@Test
	public void processTransactionMultiQuantity() {
		BackgroundAgent agent = backgroundAgent();

		assertEquals(0, agent.getPosition());
		assertEquals(0, mockAgent.getPosition());
		
		Price val = agent.getPrivateValue(2, BUY);

		// Creating and adding bids
		agent.submitOrder(BUY, Price.of(110000), 3);
		mockAgent.submitOrder(view, SELL, Price.of(110000), 2);

		// Testing the market for the correct transactions
		assertEquals(1, view.getTransactions().size());
		assertEquals(2, agent.getPosition());
		assertEquals(-220000, agent.getProfit());
		assertEquals(-2, mockAgent.getPosition());
		assertEquals(220000, mockAgent.getProfit());

		// Check surplus
		assertEquals(val.intValue() - 220000, agent.getPayoff(), eps);
	}

	/** Verify do not submit order if exceed max position allowed. */
	@Test
	public void testZIStrat() {
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(MaxQty.class, 5));
		
		// Test ZI strategy
		agent.executeZIStrategy(BUY, 1);
		assertTrue(view.getQuote().getBidPrice().isPresent());
		
		// Reset
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		// Test ZI strategy
		agent.executeZIStrategy(SELL, 1);
		assertTrue(view.getQuote().getAskPrice().isPresent());
		
		// Reset
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);

		// Test that large quantities don't submit 
		agent.executeZIStrategy(BUY, 6);
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, agent.getPosition());
		
		agent.executeZIStrategy(SELL, 6);
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, agent.getPosition());

		// Test that small quantities when a position is held don't submit
		setPosition(agent, 5);
		agent.executeZIStrategy(BUY, 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		// But still submits sells
		agent.executeZIStrategy(SELL, 1);
		assertTrue(view.getQuote().getAskPrice().isPresent());
		
		// Reset
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		setPosition(agent, -5);
		agent.executeZIStrategy(SELL, 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		// But still submits buys
		agent.executeZIStrategy(BUY, 1);
		assertTrue(view.getQuote().getBidPrice().isPresent());
		
		// Reset
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);
	}
	
	@Test
	public void randZIBuyTest() {
		int min_shade = rand.nextInt(5000); 		//[$0.00, $5.00];
		int max_shade = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];

		BackgroundAgent agent = backgroundAgent(Props.fromPairs(
				RMin.class, min_shade,
				RMax.class, max_shade));

		//Execute strategy
		agent.executeZIStrategy(BUY, 1);

		// Calculate Price Range
		Price fundPrice = fund.getValue();
		Price pv = agent.getPrivateValue(BUY);
		
		Quote quote = view.getQuote();
		assertOptionalRange(quote.getBidPrice(),
				Price.of(fundPrice.intValue() + pv.intValue() - max_shade),
				Price.of(fundPrice.intValue() + pv.intValue() - min_shade));
		assertEquals(1, quote.getBidQuantity());
	}
	
	@Test
	public void randZISellTest() {
		int min_shade = rand.nextInt(5000); 		//[$0.00, $5.00];
		int max_shade = 5000 + rand.nextInt(5000);	//[$5.00, $10.00];

		BackgroundAgent agent = backgroundAgent(Props.fromPairs(
				RMin.class, min_shade,
				RMax.class, max_shade));

		agent.executeZIStrategy(SELL, 1);

		// Calculate Price Range
		Price fundPrice = fund.getValue();
		Price pv = agent.getPrivateValue(SELL);
		
		Quote quote = view.getQuote();
		assertOptionalRange(quote.getAskPrice(),
				Price.of(fundPrice.intValue() + pv.intValue() + min_shade),
				Price.of(fundPrice.intValue() + pv.intValue() + max_shade));
		assertEquals(1, quote.getAskQuantity());

	}
	
	@Test
	public void initialPriceZIBuyTest() {
		fundamental = FundamentalValue.create(Mock.stats, timeline, 0, 100000, 1e8, rand);
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(RMin.class, 0, RMax.class, 1000));
		agent.executeZIStrategy(BUY, 1);
		/*
		 * Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000
		 * ($10.000) Bid Range = 0, 1000 ($0.00, $1.00) 99.7% of bids should
		 * fall between 100000 +/- (3*10000 + 1000) = 70000, 13000
		 */
		Quote quote = view.getQuote();
		assertOptionalRange(quote.getBidPrice(), Price.of(70000), Price.of(130000));
		assertEquals(1, quote.getBidQuantity());
	}
	
	@Test
	public void initialPriceZISellTest() {
		fundamental = FundamentalValue.create(Mock.stats, timeline, 0, 100000, 1e8, rand);
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(RMin.class, 0, RMax.class, 1000));
		agent.executeZIStrategy(SELL, 1);
		/*
		 * Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000
		 * ($10.000) Bid Range = 0, 1000 ($0.00, $1.00) 99.7% of bids should
		 * fall between 100000 +/- (3*10000 + 1000) = 70000, 13000
		 */
		Quote quote = view.getQuote();
		assertOptionalRange(quote.getAskPrice(), Price.of(70000), Price.of(130000));
		assertEquals(1, quote.getAskQuantity());
	}
	
	@Test
	public void ziPrivateValueBuyTest() {
		fundamental = Mock.fundamental(100000);
		BackgroundAgent agent = backgroundAgentwithPrivateValue(ListPrivateValue.create(
				ImmutableList.of(Price.of(10000), Price.of(-10000))), Props.fromPairs(
						RMin.class, 0,
						RMax.class, 1000));

		agent.executeZIStrategy(BUY, 1);
		// Buyers always buy at price lower than valuation ($100 + buy PV = $90)
		Quote quote = view.getQuote();
		assertOptionalRange(quote.getBidPrice(), Price.of(89000), Price.of(90000));
		assertEquals(1, quote.getBidQuantity());
	}
	
	@Test
	public void ziPrivateValueSellTest() {
		fundamental = Mock.fundamental(100000);
		BackgroundAgent agent = backgroundAgentwithPrivateValue(ListPrivateValue.create(
				ImmutableList.of(Price.of(10000), Price.of(-10000))), Props.fromPairs(
						RMin.class, 0,
						RMax.class, 1000));

		agent.executeZIStrategy(SELL, 1);
		// Sellers always sell at price higher than valuation ($100 + sell PV = $110)
		Quote quote = view.getQuote();
		assertOptionalRange(quote.getAskPrice(), Price.of(110000), Price.of(111000));
		assertEquals(1, quote.getAskQuantity());
	}

	@Test
	public void testSubmitBuyOrder() {
		// Verify that when submit order, if would exceed position limits, then do not submit the order
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(MaxQty.class, 1));
		setPosition(agent, 1);

		// Verify that a new buy order can't be submitted
		agent.submitOrder(BUY, Price.of(50), 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		// Verify that a new sell order CAN be submitted
		agent.submitOrder(SELL, Price.of(45), 1);
		assertQuote(view.getQuote(), null, 0, Price.of(45), 1);
	}

	@Test
	public void testSubmitSellOrder() {
		// Verify that when submit order, if would exceed position limits, then
		// do not submit the order
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(MaxQty.class, 1));
		setPosition(agent, -1);

		// Verify that a new sell order can't be submitted
		agent.submitOrder(SELL, Price.of(45), 1);
		assertQuote(view.getQuote(), null, 0, null, 0);

		// Verify that a new buy order CAN be submitted
		agent.submitOrder(BUY, Price.of(50), 1);
		assertQuote(view.getQuote(), Price.of(50), 1, null, 0);
	}

	@Test
	public void testPayoff() {
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				ListPrivateValue.create(ImmutableList.of(Price.of(1000), Price.of(-2000))),
				Props.fromPairs(RMin.class, 0, RMax.class, 1000));
		
		mockAgent.submitOrder(view, BUY, Price.of(51000), 1);
		agent.submitOrder(SELL, Price.of(51000), 1);

		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(-1, agent.getPosition());
		// background agent sells at 51, surplus from PV is 51-1
		assertEquals(50000, agent.getPayoff(), 0.001);
		// mock agent payoff is just profit
		assertEquals(-51000, mockAgent.getPayoff(), 0.001);


		agent.submitOrder(BUY, Price.of(95000), 1);
		mockAgent.submitOrder(view, SELL, Price.of(95000), 1);

		assertEquals(0, agent.getPosition());
		// background agent buys at 95, surplus is 1-95 + (50 from previous)
		assertEquals(50000 + (1000 - 95000), agent.getPayoff(), 0.001);
		// mock agent payoff is just profit
		assertEquals(-51000 + 95000, mockAgent.getPayoff(), 0.001);

		// after liquidate, should have no change because net position is 0
		agent.liquidateAtPrice(fund.getValue());
		assertEquals(50000 + (1000 - 95000), agent.getPayoff(), 0.001);
	}

	@Test
	public void testLiquidation() {
		// Verify that post-liquidation, payoff includes liquidation
		fundamental = Mock.fundamental(100000);
		fund = fundamental.getView(TimeStamp.ZERO);
		
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				ListPrivateValue.create(ImmutableList.of(Price.of(1000), Price.of(-1000))),
				Props.fromPairs(RMin.class, 0, RMax.class, 1000));
		
		mockAgent.submitOrder(view, BUY, Price.of(51000), 1);
		agent.submitOrder(SELL, Price.of(51000), 1);

		// background agent sells 1 @ 51 (only private value portion counted)
		assertEquals(-1, agent.getPosition());
		assertEquals(51000, agent.getProfit());
		assertEquals(51000-1000, agent.getPayoff(), 0.001);

		agent.liquidateAtPrice(fund.getValue());
		// background agent liquidates to account for short position of 1
		assertEquals(51000 - 100000, agent.getProfit());
		assertEquals(50000 - 100000, agent.getPayoff(), 0.001);
	}

	@Test
	public void testMovingFundamentalLiquidation() {
		// Verify that post-liquidation, payoff includes liquidation
		
		BackgroundAgent agent = backgroundAgentwithPrivateValue(
				ListPrivateValue.create(ImmutableList.of(Price.of(1000), Price.of(-1000))),
				Props.fromPairs(RMin.class, 0, RMax.class, 1000));
		
		mockAgent.submitOrder(view, BUY, Price.of(51000), 1);
		agent.submitOrder(SELL, Price.of(51000), 1);

		// background agent sells 1 @ 51 (only private value portion counted)
		assertEquals(-1, agent.getPosition());
		assertEquals(51000, agent.getProfit());
		assertEquals(51000-1000, agent.getPayoff(), 0.001);

		Price endTimeFundamental = fund.getValue();
		agent.liquidateAtPrice(endTimeFundamental);
		// background agent liquidates to account for short position of 1
		assertEquals(51000 - endTimeFundamental.intValue(), agent.getProfit());
		assertEquals(50000 - endTimeFundamental.intValue(), agent.getPayoff(), 0.001);
	}

	@Test
	public void zirpBasicBuyerTest() {
		fundamental = Mock.fundamental(100000);
		fund = fundamental.getView(TimeStamp.ZERO);
		
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote(Price.of(120000), Price.of(130000));
		
		zirp.executeZIRPStrategy(BUY, 1);

		// Verify that agent does shade since 10000 * 0.75 > valuation - 130000
		assertEquals(1, zirp.getPosition());
	}

	@Test
	public void zirpBasicBuyerTest2() {
		fundamental = Mock.fundamental;
		fund = fundamental.getView(TimeStamp.ZERO);
		
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote(Price.of(80000), Price.of(85000));

		zirp.executeZIRPStrategy(BUY, 1);
		
		/*
		 * When markup is not sufficient, then don't shade since 10000 * 0.75 <=
		 * val - 85000 Verify that agent's order will trade immediately
		 */
		assertEquals(1, zirp.getPosition());
		assertSingleTransaction(view.getTransactions(), Price.of(85000), TimeStamp.ZERO, 1);
	}

	/** to test what the price of the agent's submitted order is */
	@Test
	public void zirpBasicBuyerTest3() {
		fundamental = Mock.fundamental;
		fund = fundamental.getView(TimeStamp.ZERO);
		
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote(Price.of(80000), Price.of(85000));
		
		zirp.executeZIRPStrategy(BUY, 1);

		// When markup is not sufficient, then don't shade since 10000 * 0.75 <= val - 85000
		// FIXME(for Elaine) test that order has price zirp.getEstimatedValuation(BUY) (before actually submitting)
		fail();
	}
	
	@Test
	public void zirpBasicSellerTest() {
		fundamental = Mock.fundamental;
		fund = fundamental.getView(TimeStamp.ZERO);
		
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote(Price.of(80000), Price.of(85000));

		Price val = zirp.getEstimatedValuation(SELL);
		zirp.executeZIRPStrategy(SELL, 1);
		mockAgent.withdrawAllOrders(); // Withdraw orders to see zirps orders
		
		// Verify that agent doesn't shade since 10000 * 0.75 > 80000 - val 
		assertQuote(view.getQuote(), null, 0, Price.of(val.intValue() + 10000), 1);
		assertEquals(0, zirp.getPosition());
	}

	@Test
	public void zirpBasicSellerTest2() {
		fundamental = Mock.fundamental(100000);
		fund = fundamental.getView(TimeStamp.ZERO);
		
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		
		setQuote(Price.of(120000), Price.of(130000));

		zirp.executeZIRPStrategy(SELL, 1);

		/*
		 * when markup is not sufficient, then don't shade since 10000 * 0.75 <=
		 * 120000 - val Verify that agent's order will trade immediately
		 */
		assertSingleTransaction(view.getTransactions(), Price.of(120000), TimeStamp.ZERO, 1);
	}

	/** to test what the price of the agent's submitted order is */
	@Test
	public void zirpBasicSellerTest3() {
		fundamental = Mock.fundamental;
		fund = fundamental.getView(TimeStamp.ZERO);
		
		BackgroundAgent zirp = backgroundAgent(zirpProps);
		setQuote(Price.of(120000), Price.of(130000));
		
		Price val = zirp.getEstimatedValuation(SELL);
		zirp.executeZIRPStrategy(SELL, 1);
		
		// when markup is not sufficient, then don't shade since 10000 * 0.75 <= 120000 - val
		// FIXME(for Elaine) This assert may be incorrect
		assertSingleOrder(zirp.getActiveOrders(), val, 1, TimeStamp.ZERO, TimeStamp.ZERO);
	}
	
	/** Test that returns empty if exceed max position */
	@Test
	public void testZIRPStrat() {
		BackgroundAgent zirp = backgroundAgent(Props.fromPairs(MaxQty.class, 2));

		zirp.executeZIRPStrategy(BUY, 5);
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, zirp.getPosition());
		zirp.executeZIRPStrategy(SELL, 5);
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, zirp.getPosition());

		// Test ZIRP strategy
		zirp.executeZIRPStrategy(BUY, 1);
		assertTrue(view.getQuote().getBidPrice().isPresent());
	}

	// TODO: check getEstimatedValuation/Fundamental
	
	@Test
	public void executionTimePostTest() {
		EventQueue queue = queueSetup();
		Stats stats = Stats.create();
		
		BackgroundAgent agent = backgroundAgent(stats);
		
		agent.submitOrder(BUY, Price.of(50), 4);
		queue.executeUntil(TimeStamp.of(1));
		agent.submitOrder(SELL, Price.of(50), 1);
		queue.executeUntil(TimeStamp.of(2));
		agent.submitOrder(SELL, Price.of(50), 2);
		queue.executeUntil(TimeStamp.of(3));
		mockAgent.submitOrder(view, SELL, Price.of(50), 1); // Won't count towards execution times
		queue.executeUntil(TimeStamp.of(3));
		
		/*
		 * 1 executes with 1 delay
		 * 2 execute with 2 delay
		 * 1 executes with 3 delay
		 * 3 execute with 0 delay
		 */
		assertEquals(8, stats.getSummaryStats().get(Stats.EXECUTION_TIME).sum(), eps);
		assertEquals(1.1428571428571428, stats.getSummaryStats().get(Stats.EXECUTION_TIME).mean(), eps);
	}
	
	@Test
	public void controlRandPrivateValueTest() {
		int n = 10;
		Stats stats = Stats.create();
		SummStats pvMean = SummStats.on();
		for (int i = 0; i < n; ++i)
			pvMean.add(backgroundAgent(stats).getPrivateValueMean().doubleValue());
		assertEquals(pvMean.mean(), stats.getSummaryStats().get(Stats.CONTROL_PRIVATE_VALUE).mean(), eps);
	}
	
	@Test
	public void controlPrivateValueTest() {
		Stats stats = Stats.create();
		backgroundAgentwithPrivateValue(ListPrivateValue.create(ImmutableList.of(Price.of(0), Price.of(0))), stats, Props.fromPairs());
		backgroundAgentwithPrivateValue(ListPrivateValue.create(ImmutableList.of(Price.of(10), Price.of(10))), stats, Props.fromPairs());
		
		assertEquals(5, stats.getSummaryStats().get(Stats.CONTROL_PRIVATE_VALUE).mean(), eps);
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; ++i) {
			setup();
			getValuationRand();
			setup();
			getLimitPriceRand();
			setup();
			randZIBuyTest();
			setup();
			randZISellTest();
			setup();
			initialPriceZIBuyTest();
			setup();
			initialPriceZISellTest();
			setup();
			ziPrivateValueBuyTest();
			setup();
			ziPrivateValueSellTest();
			setup();
			zirpBasicBuyerTest();
			setup();
			zirpBasicBuyerTest2();
			setup();
			zirpBasicBuyerTest3();
			setup();
			zirpBasicSellerTest();
			setup();
			zirpBasicSellerTest2();
			setup();
			zirpBasicSellerTest3();
			setup();
			controlRandPrivateValueTest();
		}
	}
	
	private void setQuote(Price bid, Price ask) {
		mockAgent.submitOrder(view, BUY, bid, 1);
		mockAgent.submitOrder(view, SELL, ask, 1);
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
	
	private AtomicInteger fundamentalSetup() {
		final AtomicInteger time = new AtomicInteger();
		timeline = new Timeline() {
			@Override public void scheduleActivityIn(TimeStamp delay, Activity act) { }
			@Override public TimeStamp getCurrentTime() { return TimeStamp.of(time.get()); }
		};
		fundamental = FundamentalValue.create(Mock.stats, timeline, kappa, meanValue, variance, rand);
		fund = fundamental.getView(TimeStamp.ZERO);
		return time;
	}
	
	private EventQueue queueSetup() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		this.timeline = timeline;
		market = Mock.market(timeline);
		view = market.getPrimaryView();
		return timeline;
	}

	private BackgroundAgent backgroundAgentwithPrivateValue(ListPrivateValue privateValue) {
		return backgroundAgentwithPrivateValue(privateValue, Props.fromPairs());
	}
	
	private BackgroundAgent backgroundAgentwithPrivateValue(ListPrivateValue privateValue, Props props) {
		return backgroundAgentwithPrivateValue(privateValue, Mock.stats, props);
	}
	
	private BackgroundAgent backgroundAgentwithPrivateValue(ListPrivateValue privateValue, Stats stats, Props props) {
		return new BackgroundAgent(0, stats, timeline, Log.nullLogger(), rand, Mock.sip, fundamental, privateValue, market,
				Props.merge(defaults, props)) {
			private static final long serialVersionUID = 1L;
		};
	}
	
	private BackgroundAgent backgroundAgent() {
		return backgroundAgent(Props.fromPairs());
	}
	
	private BackgroundAgent backgroundAgent(Props props) {
		return new BackgroundAgent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market, Props.merge(defaults, props)) {
			private static final long serialVersionUID = 1L;
		};
	}
	
	private BackgroundAgent backgroundAgent(Stats stats) {
		return new BackgroundAgent(0, stats, timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market, defaults) {
			private static final long serialVersionUID = 1L;
		};
	}

}
