package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static utils.Tests.assertQuote;
import static utils.Tests.assertRegex;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import logger.Log;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.MarketLatency;
import utils.Mock;
import utils.Rand;
import utils.Repeat;
import utils.RepeatRule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import data.FundamentalValue;
import data.Props;
import data.Stats;
import entity.agent.position.ListPrivateValue;
import entity.agent.position.PrivateValue;
import entity.agent.position.PrivateValues;
import entity.agent.strategy.LimitPriceEstimator;
import entity.agent.strategy.NaiveLimitPriceEstimator;
import entity.market.CDAMarket;
import entity.market.Market;
import entity.market.Market.MarketView;
import entity.market.Price;
import entity.sip.MarketInfo;
import entity.sip.SIP;
import event.EventQueue;
import event.TimeStamp;
import event.Timeline;
import fourheap.Order.OrderType;

public class AgentTest {
	private static final double eps = 1e-6;
	private static final Rand rand = Rand.create();
	private Market market;
	private MarketView view;
	private Agent agent;
	
	@Rule
	public RepeatRule repeatRule = new RepeatRule();
	
	@Before
	public void setup()  {
		market = Mock.market();
		view = market.getPrimaryView();
		agent = Mock.agent();
	}

	@Test
	@Repeat(100)
	public void basicWithdrawBuy() {
		OrderRecord buy = agent.submitOrder(view, BUY, Price.of(50), 1);
		agent.submitOrder(view, SELL, Price.of(100), 2);
		assertQuote(view.getQuote(), Price.of(50), 1, Price.of(100), 2);
		
		agent.withdrawOrder(buy);
		assertQuote(view.getQuote(), null, 0, Price.of(100), 2);
	}
	
	@Test
	@Repeat(100)
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
		agent = new Agent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, PrivateValues.zero(),
				ImmutableList.<TimeStamp> of().iterator(), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
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
		ZIRAgent zir = ZIRAgent.create(0, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs(BackgroundReentryRate.class, 0d));
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
		ZIRAgent zir = ZIRAgent.create(12345, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs(BackgroundReentryRate.class, 0d));
		NoOpAgent noop = NoOpAgent.create(8459, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, Props.fromPairs());
		assertRegex("\\(\\d+\\)", agent.toString());
		assertEquals("ZIR(12345)", zir.toString());
		assertEquals("NoOp(8459)", noop.toString());
	}
	
	@Test
	public void getLimitPriceRand() {
		ListPrivateValue privateValue = ListPrivateValue.createRandomly(5, 1e8, rand);
		FundamentalValue fundamental = Mock.fundamental(12345);
		Agent agent = mockAgent(privateValue, fundamental);
		LimitPriceEstimator estimator = NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO));
		
		Price fundPrice = fundamental.getValueAt(TimeStamp.ZERO);
		
		setPosition(agent, 3);
		assertEquals(Price.of(fundPrice.doubleValue() + privateValue.getValue(3, 2, BUY).doubleValue()/2), estimator.getLimitPrice(BUY, 2));
		assertEquals(Price.of(fundPrice.doubleValue() + privateValue.getValue(3, 2, SELL).doubleValue()/2), estimator.getLimitPrice(SELL, 2));
		
		setPosition(agent, -2);
		assertEquals(Price.of(fundPrice.doubleValue() + privateValue.getValue(-2, 2, BUY).doubleValue()/2), estimator.getLimitPrice(BUY, 2));
		assertEquals(Price.of(fundPrice.doubleValue() + privateValue.getValue(-2, 2, SELL).doubleValue()/2), estimator.getLimitPrice(SELL, 2));

		setPosition(agent, -2);
		assertEquals(Price.of(fundPrice.doubleValue() + privateValue.getValue(-2, 3, BUY).doubleValue()/3), estimator.getLimitPrice(BUY, 3));
		assertEquals(Price.of(fundPrice.doubleValue() + privateValue.getValue(-2, 3, SELL).doubleValue()/3), estimator.getLimitPrice(SELL, 3));
	}
	
	@Test
	@Repeat(100)
	public void reentryTest() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		PeekingIterator<TimeStamp> reentries = Iterators.peekingIterator(Agent.exponentials(0.1, rand));
		Agent agent = mockAgent(timeline, reentries);
		
		// Test reentries
		assertTrue(reentries.hasNext());
		TimeStamp next = reentries.peek();
		assertTrue(next.getInTicks() >= 0);
		
		// Test agent strategy
		timeline.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		assertTrue(reentries.hasNext());
	}
	
	@Test
	public void reentryRateZeroTest() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		// Test reentries - note should never iterate past INFINITE b/c it 
		// will never execute
		PeekingIterator<TimeStamp> reentries = Iterators.peekingIterator(Agent.exponentials(0, rand));
		assertFalse(reentries.hasNext());
		
		final AtomicBoolean executed = new AtomicBoolean(false);
		new Agent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, PrivateValues.zero(),
				reentries, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() {
				super.agentStrategy();
				executed.set(true);
			}
		};
		
		assertFalse(reentries.hasNext());

		// Now test for agent, which should not arrive at time 0
		timeline.executeUntil(TimeStamp.of(100));
		assertFalse(reentries.hasNext());
		assertFalse(executed.get());
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
	
	private void setPosition(Agent agent, int position) {
		int quantity = position - agent.getPosition();
		if (quantity == 0)
			return;
		OrderType type = quantity > 0 ? BUY : SELL;
		quantity = Math.abs(quantity);
		agent.submitOrder(view, type, Price.ZERO, quantity);
		this.agent.submitOrder(view, type == BUY ? SELL : BUY, Price.ZERO, quantity);
		assertEquals(position, agent.getPosition());
	}
	
	private Agent mockAgent(Timeline timeline, Iterator<TimeStamp> arrivalIntervals) {
		return new Agent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, PrivateValues.zero(),
				arrivalIntervals, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
		};
	}
	
	private Agent mockAgent(Timeline timeline) {
		return mockAgent(timeline, ImmutableList.<TimeStamp> of().iterator());
	}
	
	private Agent mockAgent(PrivateValue privateValue, FundamentalValue fundamental) {
		return new Agent(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, privateValue,
				ImmutableList.<TimeStamp> of().iterator(), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
		};
	}

}
