package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertQuote;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.MaxQty;
import systemmanager.Keys.RMax;
import systemmanager.Keys.RMin;
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
import event.EventQueue;
import event.TimeStamp;
import event.Timeline;
import fourheap.Order.OrderType;

public class BackgroundAgentTest {
	
	private static final Rand rand = Rand.create();
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
	public void testPositionBounds() {
		BackgroundAgent agent = backgroundAgent(Props.fromPairs(MaxQty.class, 5));
		
		// Can submit buy for 1 at 0
		agent.submitNMSOrder(BUY, Price.ZERO, 1);
		assertTrue(view.getQuote().getBidPrice().isPresent());
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		// Can submit sell for 1 at 0
		agent.submitNMSOrder(SELL, Price.ZERO, 1);
		assertTrue(view.getQuote().getAskPrice().isPresent());
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);

		// Can't submit buy for 6 at 0
		agent.submitNMSOrder(BUY, Price.ZERO, 6);
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, agent.getPosition());
		agent.withdrawAllOrders();
		
		// Can't submit sell for 6 at 0
		agent.submitNMSOrder(SELL, Price.ZERO, 6);
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertEquals(0, agent.getPosition());
		agent.withdrawAllOrders();

		// Can't submit buy for 1 at 5
		setPosition(agent, 5);
		agent.submitNMSOrder(BUY, Price.ZERO, 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
		agent.withdrawAllOrders();
		
		// Can submit sell for 1 at 5
		agent.submitNMSOrder(SELL, Price.ZERO, 1);
		assertTrue(view.getQuote().getAskPrice().isPresent());
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);
		
		// Can't submit sell for 1 at -5
		setPosition(agent, -5);
		agent.submitNMSOrder(SELL, Price.ZERO, 1);
		assertQuote(view.getQuote(), null, 0, null, 0);
		agent.withdrawAllOrders();
		
		// Can submit buy for 1 at -5
		agent.submitNMSOrder(BUY, Price.ZERO, 1);
		assertTrue(view.getQuote().getBidPrice().isPresent());
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);
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
			controlRandPrivateValueTest();
		}
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
	
	private EventQueue queueSetup() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		this.timeline = timeline;
		market = Mock.market(timeline);
		view = market.getPrimaryView();
		return timeline;
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
