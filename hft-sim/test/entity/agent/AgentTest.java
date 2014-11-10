package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertQuote;
import static utils.Tests.assertRegex;

import java.util.concurrent.atomic.AtomicBoolean;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.MarketLatency;
import utils.Mock;
import utils.Rand;

import com.google.common.collect.Iterables;

import data.Props;
import data.Stats;
import entity.agent.position.PrivateValues;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.sip.MarketInfo;
import entity.sip.SIP;
import event.EventQueue;
import event.TimeStamp;
import event.Timeline;

public class AgentTest {
	private static final double eps = 1e-6;
	private static final Rand rand = Rand.create();
	private Market market;
	private MarketView view;
	private Agent agent;
	
	@Before
	public void setup()  {
		market = Mock.market();
		view = market.getPrimaryView();
		agent = Mock.agent();
	}

	@Test
	public void basicWithdrawBuy() {
		OrderRecord buy = agent.submitOrder(view, BUY, Price.of(50), 1);
		agent.submitOrder(view, SELL, Price.of(100), 2);
		assertQuote(view.getQuote(), Price.of(50), 1, Price.of(100), 2);
		
		agent.withdrawOrder(buy);
		assertQuote(view.getQuote(), null, 0, Price.of(100), 2);
	}
	
	@Test
	public void basicWithdrawSell() {
		agent.submitOrder(view, BUY, Price.of(50), 1);
		OrderRecord sell = agent.submitOrder(view, SELL, Price.of(100), 2);
		assertQuote(view.getQuote(), Price.of(50), 1, Price.of(100), 2);
		
		agent.withdrawOrder(sell);
		assertQuote(view.getQuote(), Price.of(50), 1, null, 0);
	}
	
	@Test
	public void withdrawNewestOrder() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		view = Mock.market(timeline).getPrimaryView();
		agent = Mock.agent(timeline);
		
		agent.submitOrder(view, BUY, Price.of(50), 1);
		timeline.executeUntil(TimeStamp.of(1)); // Oldest relative to create time
		agent.submitOrder(view, SELL, Price.of(100), 1);
		timeline.executeUntil(TimeStamp.of(1));
		
		// Verify orders added correctly
		assertQuote(view.getQuote(), Price.of(50), 1, Price.of(100), 1);
		
		// Withdraw newest order (sell)
		agent.withdrawNewestOrder();
		timeline.executeUntil(TimeStamp.of(1));
		
		assertQuote(view.getQuote(), Price.of(50), 1, null, 0);
	}
	
	@Test
	public void withdrawOldestOrder() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		view = Mock.market(timeline).getPrimaryView();
		agent = Mock.agent(timeline);
		
		agent.submitOrder(view, BUY, Price.of(50), 1);
		timeline.executeUntil(TimeStamp.of(1)); // Oldest relative to create time
		agent.submitOrder(view, SELL, Price.of(100), 1);
		timeline.executeUntil(TimeStamp.of(1));
		
		// Verify orders added correctly
		assertQuote(view.getQuote(), Price.of(50), 1, Price.of(100), 1);
		
		agent.withdrawOldestOrder();
		timeline.executeUntil(TimeStamp.of(1));
		
		assertQuote(view.getQuote(), null, 0, Price.of(100), 1);
	}
	
	@Test
	public void withdrawAllOrders() {
		agent.submitOrder(view, BUY, Price.of(50), 1);
		agent.submitOrder(view, SELL, Price.of(100), 2);
		assertQuote(view.getQuote(), Price.of(50), 1, Price.of(100), 2);
		
		agent.withdrawAllOrders();
		assertQuote(view.getQuote(), null, 0, null, 0);
	}
	
	/** Test that withdraw still works even when agent has no orders */
	@Test
	public void noOrderWithdraw() {
		agent.withdrawNewestOrder();
		agent.withdrawOldestOrder();
		agent.withdrawAllOrders();
	}
	
	@Test
	public void withdrawOrderDelayed()  {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		Market real = Mock.market(timeline);
		view = real.getPrimaryView();
		MarketView slow = real.getView(TimeStamp.of(10));
		
		final AtomicBoolean orderSubmitted = new AtomicBoolean(false);
		agent = new Agent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, PrivateValues.zero(), TimeStamp.ZERO,
				Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() { }
			@Override protected void orderSubmitted(OrderRecord order, MarketView market, TimeStamp submittedTime) {
				super.orderSubmitted(order, market, submittedTime);
				orderSubmitted.set(true);
			}
		};
		
		OrderRecord order = agent.submitOrder(slow, BUY, Price.of(100), 1);
		
		// Verify Orders don't exist yet
		assertFalse(orderSubmitted.get());
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertQuote(slow.getQuote(), null, 0, null, 0);
		
		// Verify orders exist in market, but agent has no knowledge
		timeline.executeUntil(TimeStamp.of(10));
		assertFalse(orderSubmitted.get());
		assertQuote(view.getQuote(), Price.of(100), 1, null, 0);
		assertQuote(slow.getQuote(), null, 0, null, 0);
		
		// After quotes have updated and reached agent
		timeline.executeUntil(TimeStamp.of(20));
		assertTrue(orderSubmitted.get());
		assertQuote(view.getQuote(), Price.of(100), 1, null, 0);
		assertQuote(slow.getQuote(), Price.of(100), 1, null, 0);
		
		// Withdraw order
		agent.withdrawOrder(order);
		
		// Check that agent still knows order is submitted
		assertEquals(1, order.getQuantity());
		
		// Verify that quote is now stale
		timeline.executeUntil(TimeStamp.of(30));
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertQuote(slow.getQuote(), Price.of(100), 1, null, 0);
		// Agent still thinks order might be around, even through it's now gone from the market
		assertEquals(1, order.getQuantity());
		
		// After quotes have updated
		timeline.executeUntil(TimeStamp.of(40));
		assertQuote(view.getQuote(), null, 0, null, 0);
		assertQuote(slow.getQuote(), null, 0, null, 0);
		// Check order removed
		assertEquals(0, order.getQuantity());
	}
	
	/** Test that routed nms orders update */
	@Test
	public void orderQuickNMSMarketUpdateBuy() {
		nbboSetup(Mock.timeline, Price.of(80), Price.of(100));
		
		OrderRecord order = agent.submitNMSOrder(view, BUY, Price.of(100), 1);
		assertNotEquals(view, order.getCurrentMarket());
	}
	
	/** Test that nonrouted nms orders update */
	@Test
	public void orderQuickNMSMarketNoUpdateBuy() {
		nbboSetup(Mock.timeline, Price.of(80), Price.of(100));
		
		OrderRecord order = agent.submitNMSOrder(view, BUY, Price.of(90), 1);
		assertEquals(view, order.getCurrentMarket());
	}
	
	/** Assert that the market view updates even for slow transactions */
	@Test
	public void orderSlowNMSMarketUpdateBuy() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		nbboSetup(timeline, Price.of(80), Price.of(100));
		view = market.getView(TimeStamp.of(5));
		
		OrderRecord order = agent.submitNMSOrder(view, BUY, Price.of(100), 1);
		
		/*
		 * Normally this would trigger at 9 and 10, but by default routed orders
		 * use the primary view of the routed market, and so the routing happens
		 * instantly once it reaches the primary market at 5.
		 */
		timeline.executeUntil(TimeStamp.of(4));
		assertEquals(view, order.getCurrentMarket());
		
		timeline.executeUntil(TimeStamp.of(5));
		assertNotEquals(view, order.getCurrentMarket());
	}
	
	/** Assert that the market view updates even for slow transactions */
	@Test
	public void orderSlowNMSMarketNoUpdateBuy() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		nbboSetup(timeline, Price.of(80), Price.of(100));
		view = market.getView(TimeStamp.of(5));
		
		OrderRecord order = agent.submitNMSOrder(view, BUY, Price.of(90), 1);
		
		/*
		 * Normally this would trigger at 9 and 10, but by default routed orders
		 * use the primary view of the routed market, and so the routing happens
		 * instantly once it reaches the primary market at 5.
		 */
		timeline.executeUntil(TimeStamp.of(4));
		assertEquals(view, order.getCurrentMarket());
		
		timeline.executeUntil(TimeStamp.of(5));
		assertEquals(view, order.getCurrentMarket());
	}
	
	/** Test that routed nms orders update */
	@Test
	public void orderQuickNMSMarketUpdateSell() {
		nbboSetup(Mock.timeline, Price.of(80), Price.of(100));
		
		OrderRecord order = agent.submitNMSOrder(view, SELL, Price.of(80), 1);
		assertNotEquals(view, order.getCurrentMarket());
	}
	
	/** Test that nonrouted nms orders update */
	@Test
	public void orderQuickNMSMarketNoUpdateSell() {
		nbboSetup(Mock.timeline, Price.of(80), Price.of(100));
		
		OrderRecord order = agent.submitNMSOrder(view, SELL, Price.of(90), 1);
		assertEquals(view, order.getCurrentMarket());
	}
	
	/** Assert that the market view updates even for slow transactions */
	@Test
	public void orderSlowNMSMarketUpdateSELL() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		nbboSetup(timeline, Price.of(80), Price.of(100));
		view = market.getView(TimeStamp.of(5));
		
		OrderRecord order = agent.submitNMSOrder(view, SELL, Price.of(80), 1);
		
		/*
		 * Normally this would trigger at 9 and 10, but by default routed orders
		 * use the primary view of the routed market, and so the routing happens
		 * instantly once it reaches the primary market at 5.
		 */
		timeline.executeUntil(TimeStamp.of(4));
		assertEquals(view, order.getCurrentMarket());
		
		timeline.executeUntil(TimeStamp.of(5));
		assertNotEquals(view, order.getCurrentMarket());
	}
	
	/** Assert that the market view updates even for slow transactions */
	@Test
	public void orderSlowNMSMarketNoUpdateSell() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		nbboSetup(timeline, Price.of(80), Price.of(100));
		view = market.getView(TimeStamp.of(5));
		
		OrderRecord order = agent.submitNMSOrder(view, SELL, Price.of(90), 1);
		
		/*
		 * Normally this would trigger at 9 and 10, but by default routed orders
		 * use the primary view of the routed market, and so the routing happens
		 * instantly once it reaches the primary market at 5.
		 */
		timeline.executeUntil(TimeStamp.of(4));
		assertEquals(view, order.getCurrentMarket());
		
		timeline.executeUntil(TimeStamp.of(5));
		assertEquals(view, order.getCurrentMarket());
	}
	
	/** Test that withdraw still works if order is withdrawn while routing */
	@Test
	public void withdrawWhileRouting() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		nbboSetup(timeline, Price.of(80), Price.of(100));
		view = market.getView(TimeStamp.of(5));
		
		OrderRecord order = agent.submitNMSOrder(view, SELL, Price.of(80), 1);
		
		/*
		 * Normally this would trigger at 9 and 10, but by default routed orders
		 * use the primary view of the routed market, and so the routing happens
		 * instantly once it reaches the primary market at 5.
		 */
		timeline.executeUntil(TimeStamp.of(4));
		assertEquals(view, order.getCurrentMarket());
		
		agent.withdrawOrder(order);
		assertEquals(1, order.getQuantity());
		
		timeline.executeUntil(TimeStamp.of(5));
		assertNotEquals(view, order.getCurrentMarket());
		assertTrue(agent.getActiveOrders().isEmpty());
		assertEquals(0, order.getQuantity());
	}

	@Test
	public void processTransaction() {
		Agent other = Mock.agent();
		
		assertTrue(view.getTransactions().isEmpty());
		
		// Creating and adding bids
		agent.submitOrder(view, BUY, Price.of(110), 1);
		other.submitOrder(view, SELL, Price.of(100), 1);
		
		assertEquals(1, view.getTransactions().size());
		assertEquals(1, agent.getPosition());
		assertEquals(-110, agent.getProfit(), eps);
		assertTrue(agent.getActiveOrders().isEmpty());
		assertEquals(-1, other.getPosition());
		assertEquals(110, other.getProfit(), eps);
		assertTrue(other.getActiveOrders().isEmpty());
	}
	
	@Test
	public void processTransactionSlow() {
		// Note, it is critical that the agent's also use the event queue as their timeline
		EventQueue timeline = latencySetup(TimeStamp.of(5));
		Agent other = mockAgent(timeline);
		
		assertTrue(view.getTransactions().isEmpty());
		
		// Creating and adding bids
		OrderRecord remaining = agent.submitOrder(view, BUY, Price.of(110), 2);
		timeline.executeUntil(TimeStamp.of(1));
		
		other.submitOrder(view, SELL, Price.of(100), 1);
		
		timeline.executeUntil(TimeStamp.of(10));
		assertTrue(view.getTransactions().isEmpty());
		
		timeline.executeUntil(TimeStamp.of(11)); // 5 more to reach market and transact, and then 5 to come back to agents
		
		assertEquals(1, view.getTransactions().size());
		assertEquals(1, agent.getPosition());
		assertEquals(-110, agent.getProfit(), eps);
		assertEquals(remaining, Iterables.getOnlyElement(agent.getActiveOrders()));
		assertEquals(1, remaining.getQuantity());
		assertEquals(-1, other.getPosition());
		assertEquals(110, other.getProfit(), eps);
		assertTrue(other.getActiveOrders().isEmpty());
	}
	
	@Test
	public void processTransactionMultiQuantity() {
		Agent other = Mock.agent();

		assertTrue(view.getTransactions().isEmpty());

		// Creating and adding bids
		agent.submitOrder(view, BUY, Price.of(110), 3);
		other.submitOrder(view, SELL, Price.of(100), 2);

		// Testing the market for the correct transactions
		assertEquals(1, view.getTransactions().size());
		assertEquals(2, agent.getPosition());
		assertEquals(-220, agent.getProfit(), eps);
		assertEquals(-2, other.getPosition());
		assertEquals(220, other.getProfit(), eps);
	}
	
	@Test
	public void classPostTransactionTest() {
		Stats stats = Stats.create();
		Mock.timeline.ignoreNext();
		ZIRAgent zir = ZIRAgent.create(0, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs());
		NoOpAgent noop1 = NoOpAgent.create(1, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, Props.fromPairs());
		NoOpAgent noop2 = NoOpAgent.create(2, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, Props.fromPairs());
		
		zir.submitOrder(view, BUY, Price.of(50), 2);
		noop1.submitOrder(view, SELL, Price.of(50), 3);
		noop2.submitOrder(view, BUY, Price.of(50), 1);
		
		// XXX Number of transactions is independent of quantity
		assertEquals(1, stats.getSummaryStats().get(Stats.NUM_TRANS + "ziragent").sum(), eps);
		assertEquals(3, stats.getSummaryStats().get(Stats.NUM_TRANS + "noopagent").sum(), eps);
		// XXX Total transactions is double the number of times actually transacted
		assertEquals(4, stats.getSummaryStats().get(Stats.NUM_TRANS_TOTAL).sum(), eps);
	}
	
	@Test
	public void liquidation() {
		Agent other = Mock.agent();
		
		// Check that no change if position 0
		agent.liquidateAtPrice(Price.of(100000));
		assertEquals(0, agent.getProfit());
		
		// Check liquidation when position > 0 (sell 1 unit)
		agent = Mock.agent();
		agent.submitOrder(view, BUY, Price.ZERO, 1);
		other.submitOrder(view, SELL, Price.ZERO, 1);
		
		assertEquals(1, agent.getPosition());
		agent.liquidateAtPrice(Price.of(100000));
		assertEquals(100000, agent.getProfit());
		
		// Check liquidation when position < 0 (buy 2 units)
		agent = Mock.agent();
		agent.submitOrder(view, SELL, Price.ZERO, 2);
		other.submitOrder(view, BUY, Price.ZERO, 2);
		
		assertEquals(-2, agent.getPosition());
		agent.liquidateAtPrice(Price.of(100000));
		assertEquals(-200000, agent.getProfit());
	}
	
	@Test
	public void payoffTest() {
		Agent other = Mock.agent();
		
		other.submitOrder(view, BUY, Price.of(200), 2);
		agent.submitOrder(view, SELL, Price.of(200), 2);
		
		other.liquidateAtPrice(Price.of(100));
		agent.liquidateAtPrice(Price.of(100));
		
		assertEquals(200, agent.getPayoff(), eps);
		assertEquals(-200, other.getPayoff(), eps);
	}
	
	@Test
	public void toStringTest() {
		Agent agent = Mock.agent();
		ZIRAgent zir = ZIRAgent.create(12345, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs());
		NoOpAgent noop = NoOpAgent.create(8459, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, Props.fromPairs());
		assertRegex("\\(\\d+\\)", agent.toString());
		assertEquals("ZIR(12345)", zir.toString());
		assertEquals("NoOp(8459)", noop.toString());
	}
	
	@Test
	public void extraTest()  {
		for (int i = 0; i < 100; i++) {
			setup();
			basicWithdrawBuy();
			setup();
			basicWithdrawSell();
		}
	}
	
	private MarketInfo nbboSetup(Timeline timeline, Price bid, Price ask) {
		SIP sip = SIP.create(Mock.stats, timeline, Log.nullLogger(), rand, TimeStamp.ZERO);
		market = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs());
		MarketView other = CDAMarket.create(1, Mock.stats, timeline, Log.nullLogger(), rand, sip, Props.fromPairs()).getPrimaryView();
		view = market.getPrimaryView();
		other.submitOrder(agent, OrderRecord.create(other, TimeStamp.ZERO, BUY, bid, 1));
		other.submitOrder(agent, OrderRecord.create(other, TimeStamp.ZERO, SELL, ask, 1));
		return sip;
	}
	
	private EventQueue latencySetup(TimeStamp marketLatency) {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		market = CDAMarket.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs(MarketLatency.class, marketLatency));
		view = market.getPrimaryView();
		agent = mockAgent(timeline);
		return timeline;
	}
	
	private Agent mockAgent(Timeline timeline) {
		return new Agent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, PrivateValues.zero(),
				TimeStamp.ZERO, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() { }
		};
	}
	
}
