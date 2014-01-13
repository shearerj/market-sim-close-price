package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import activity.Activity;
import activity.AgentStrategy;
import activity.Clear;
import activity.ProcessQuote;
import activity.SubmitOrder;

import com.google.common.collect.Iterables;

import systemmanager.Consts;
import systemmanager.EventManager;
import systemmanager.Keys;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.BestBidAsk;
import entity.infoproc.QuoteProcessor;
import entity.infoproc.SIP;
import entity.market.DummyMarketTime;
import entity.market.MarketTime;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class MAMarketMakerTest {

	private MockMarket market;
	private SIP sip;
	private FundamentalValue fundamental = new MockFundamental(100000);

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "MAMarketMakerTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip);
	}

	private EntityProperties setupProperties(int numRungs, int rungSize, 
			boolean truncateLadder, int tickSize, int numHistorical) {
		EntityProperties agentProperties = new EntityProperties();
		agentProperties.put(Keys.NUM_RUNGS, numRungs);
		agentProperties.put(Keys.RUNG_SIZE, rungSize);
		agentProperties.put(Keys.TRUNCATE_LADDER, truncateLadder);
		agentProperties.put(Keys.TICK_SIZE, tickSize);
		agentProperties.put(Keys.REENTRY_RATE, 0.000001);
		agentProperties.put(Keys.NUM_HISTORICAL, numHistorical);
		return agentProperties;
	}

	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, does not submit any orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new MAMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(2, 10, false, 1, 5));

		// Check activities inserted (none, other than reentry)
		Iterable<? extends Activity> acts = mm.agentStrategy(time);
		assertEquals("Incorrect number of activities", 1, Iterables.size(acts));
		assertTrue(Iterables.getFirst(acts, null) instanceof AgentStrategy);
	}

	/**
	 * Check that bid and ask queues are updating correctly, and moving average
	 * is computed correctly
	 */
	@Test
	public void computeMovingAverage() {
		TimeStamp time = TimeStamp.ZERO;

		MAMarketMaker mm = new MAMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(1, 10, false, 1, 5));

		QuoteProcessor qp = mm.marketQuoteProcessor;

		// Add quotes & execute agent strategy in between (without actually submitting orders)
		int mktTime = 0;
		addQuote(qp, 50, 60, 0, mktTime++);
		mm.agentStrategy(time);
		addQuote(qp, 52, 64, 0, mktTime++);
		mm.agentStrategy(time);
		addQuote(qp, 54, 68, 0, mktTime++);
		mm.agentStrategy(time);
		addQuote(qp, 56, 72, 0, mktTime++);
		mm.agentStrategy(time);
		addQuote(qp, 58, 76, 0, mktTime++);
		Iterable<? extends Activity> acts = mm.agentStrategy(time);

		assertEquals(new Price(58), mm.lastBid);
		assertEquals(new Price(76), mm.lastAsk);
		for (Activity a : acts) if (a instanceof SubmitOrder) a.execute(time);

		// check queues
		assertEquals(new Price(50), mm.bidQueue.peek());
		assertEquals(new Price(60), mm.askQueue.peek());
		ArrayList<Price> list = new ArrayList<Price>(mm.bidQueue);
		int [] bids = {50, 52, 54, 56, 58};
		int i = 0;
		for (Price p : list) assertEquals(bids[i++], p.intValue());
		list = new ArrayList<Price>(mm.askQueue);
		int [] asks = {60, 64, 68, 72, 76};
		i = 0;
		for (Price p : list) assertEquals(asks[i++], p.intValue());

		// check submitted orders
		assertEquals("Incorrect number of orders", 2, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			if (o.getOrderType() == BUY) {
				assertEquals(new Price(54), o.getPrice());
			} else {
				assertEquals(new Price(68), o.getPrice());
			}
		}
	}

	Iterable<? extends Activity> addQuote(QuoteProcessor qp, int buy, int sell, int time, int marketTime) {
		TimeStamp ts = new TimeStamp(time);
		MarketTime mktTime = new DummyMarketTime(ts, marketTime);
		Quote q = new Quote(market, new Price(buy), 1, new Price(sell), 1, ts);
		sip.processQuote(market, mktTime, q, ts);
		return qp.processQuote(market, mktTime, q, ts);
	}

	/**
	 * Verify that bids/asks are being evicted correctly
	 */
	@Test
	public void evictingQueueTest() {
		TimeStamp time = TimeStamp.ZERO;

		MAMarketMaker mm = new MAMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(2, 10, false, 1, 3));

		QuoteProcessor qp = mm.marketQuoteProcessor;

		// Add quotes & execute agent strategy in between (without actually submitting orders)
		int mktTime = 0;
		addQuote(qp, 50, 60, 0, mktTime++);
		mm.agentStrategy(time);

		// check queues
		assertEquals(new Price(50), mm.bidQueue.peek());
		assertEquals(new Price(60), mm.askQueue.peek());
		for (Price p : mm.bidQueue) assertEquals(50, p.intValue());
		for (Price p : mm.askQueue) assertEquals(60, p.intValue());

		addQuote(qp, 52, 64, 0, mktTime++);
		mm.agentStrategy(time);
		// check queues
		assertEquals(new Price(50), mm.bidQueue.peek());
		assertEquals(new Price(60), mm.askQueue.peek());
		ArrayList<Price> list = new ArrayList<Price>(mm.bidQueue);
		int [] bids = {50, 52};
		int i = 0;
		for (Price p : list) assertEquals(bids[i++], p.intValue());
		list = new ArrayList<Price>(mm.askQueue);
		int [] asks = {60, 64};
		i = 0;
		for (Price p : list) assertEquals(asks[i++], p.intValue());
		
		addQuote(qp, 53, 69, 0, mktTime++);
		mm.agentStrategy(time);
		// check queues
		assertEquals(new Price(50), mm.bidQueue.peek());
		assertEquals(new Price(60), mm.askQueue.peek());
		list = new ArrayList<Price>(mm.bidQueue);
		int [] bids2 = {50, 52, 53};
		int j = 0;
		for (Price p : list) assertEquals(bids2[j++], p.intValue());
		list = new ArrayList<Price>(mm.askQueue);
		int [] asks2 = {60, 64, 69};
		j = 0;
		for (Price p : list) assertEquals(asks2[j++], p.intValue());
	}


	@Test
	public void basicLadderTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new MAMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(2, 10, false, 1, 5));

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		// Creating and adding bids
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1, time));
		em.executeUntil(new TimeStamp(1));
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1, time));
		em.addActivity(new Clear(market, time));
		em.executeUntil(new TimeStamp(1));
		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(50), quote.getAskPrice());
		assertEquals(new Price(40), quote.getBidPrice());

		// Check activities inserted (4 submit orders plus agent reentry)
		Iterable<? extends Activity> acts = mm.agentStrategy(time);
		assertEquals("Incorrect number of activities", 5, Iterables.size(acts));
		for (Activity a : acts)	if (a instanceof SubmitOrder) a.execute(time);

		// Check ladder of orders (use market's collection b/c ordering consistent)
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 6, orders.size());

		assertEquals(agent1, orders.get(0).getAgent());
		assertEquals(new Price(40), orders.get(0).getPrice());
		assertEquals(agent2, orders.get(1).getAgent());
		assertEquals(new Price(50), orders.get(1).getPrice());

		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());

		assertEquals(mm, orders.get(2).getAgent());
		assertEquals(new Price(30), orders.get(2).getPrice());
		assertEquals(OrderType.BUY, orders.get(2).getOrderType());
		assertEquals(mm, orders.get(3).getAgent());
		assertEquals(new Price(40), orders.get(3).getPrice());
		assertEquals(OrderType.BUY, orders.get(3).getOrderType());

		assertEquals(mm, orders.get(4).getAgent());
		assertEquals(new Price(60), orders.get(4).getPrice());
		assertEquals(OrderType.SELL, orders.get(4).getOrderType());
		assertEquals(mm, orders.get(5).getAgent());
		assertEquals(new Price(50), orders.get(5).getPrice());
		assertEquals(OrderType.SELL, orders.get(5).getOrderType());
	}

	
	/**
	 * When bid/ask queues are not full yet, test the computation of the
	 * moving average
	 */
	@Test
	public void partialQueueLadderTest() {
		TimeStamp time = TimeStamp.ZERO;

		MAMarketMaker mm = new MAMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(1, 10, false, 1, 5));

		QuoteProcessor qp = mm.marketQuoteProcessor;

		// Add quotes & execute agent strategy in between (without actually submitting orders)
		int mktTime = 0;
		addQuote(qp, 50, 60, 0, mktTime++);
		mm.agentStrategy(time);
		addQuote(qp, 52, 64, 0, mktTime++);
		mm.agentStrategy(time);
		addQuote(qp, 54, 68, 0, mktTime++);
		Iterable<? extends Activity> acts = mm.agentStrategy(time);

		assertEquals(new Price(54), mm.lastBid);
		assertEquals(new Price(68), mm.lastAsk);
		for (Activity a : acts)	if (a instanceof SubmitOrder) a.execute(time);

		// check queues
		assertEquals(new Price(50), mm.bidQueue.peek());
		assertEquals(new Price(60), mm.askQueue.peek());
		ArrayList<Price> list = new ArrayList<Price>(mm.bidQueue);
		int [] bids = {50, 52, 54};
		int i = 0;
		for (Price p : list) assertEquals(bids[i++], p.intValue());
		list = new ArrayList<Price>(mm.askQueue);
		int [] asks = {60, 64, 68};
		i = 0;
		for (Price p : list) assertEquals(asks[i++], p.intValue());

		// check submitted orders
		assertEquals("Incorrect number of orders", 2, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			if (o.getOrderType() == BUY) {
				assertEquals(new Price(52), o.getPrice());
			} else {
				assertEquals(new Price(64), o.getPrice());
			}
		}
	}
	

	/**
	 * Check changing numRungs, rungSize
	 */
	@Test
	public void rungsTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker marketmaker = new MAMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(3, 12, false, 5, 5));

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		// Creating and adding bids
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1, time));
		em.executeUntil(new TimeStamp(1));
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1, time));
		em.addActivity(new Clear(market, time));
		em.executeUntil(new TimeStamp(1));

		// Initial MM strategy
		em.addActivity(new AgentStrategy(marketmaker, time));
		em.executeUntil(new TimeStamp(1));

		// Check ladder of orders
		// market's orders contains all orders ever submitted
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 8, orders.size());
		// Verify that 3 rungs on each side
		// Rung size was 12 quantized by tick size 5
		Order order = orders.get(2);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(20), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(3);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(30), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(4);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(40), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());

		order = orders.get(5);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(70), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(6);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(60), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(7);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(50), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	@Test
	public void noOpTest() {
		EntityProperties agentProperties = setupProperties(2, 10, false, 1, 5);
		agentProperties.put(Keys.NO_OP, true);

		MarketMaker mm = new MAMarketMaker(fundamental, sip, market, 
				new Random(), agentProperties);

		// Check activities inserted (none, since no-op)
		Iterable<? extends Activity> acts = mm.agentStrategy(TimeStamp.ZERO);
		assertEquals("Incorrect number of activities", 0, Iterables.size(acts));
	}

	@Test
	public void truncateBidTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker marketmaker = new MAMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(3, 5, true, 1, 5));

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		EventManager em = new EventManager(new Random());

		// Creating and adding bids
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(102), 1, time));
		em.executeUntil(new TimeStamp(1));
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(105), 1, time));
		em.addActivity(new Clear(market, time));
		em.executeUntil(new TimeStamp(1));

		// Updating NBBO quote
		MockMarket market2 = new MockMarket(sip);
		Quote q = new Quote(market2, new Price(90), 1, new Price(100), 1, time);
		em.addActivity(new ProcessQuote(sip, market2, new DummyMarketTime(time, 1), q, 
				time));
		em.executeUntil(new TimeStamp(1));

		// Just to check that NBBO correct (it crosses)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(102), nbbo.getBestBid());

		// MM strategy
		em.addActivity(new AgentStrategy(marketmaker, time));
		em.executeUntil(new TimeStamp(1));

		// Check ladder of orders
		// market's orders contains all orders ever submitted
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 7, orders.size());
		// Verify that 2 rungs on truncated side
		Order order = orders.get(2);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(92), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(3);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(97), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		// 3 rungs on sell side
		order = orders.get(4);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(115), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(5);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(110), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(6);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(105), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	@Test
	public void truncateAskTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker marketmaker = new MAMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(3, 5, true, 1, 5));

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		EventManager em = new EventManager(new Random());

		// Creating and adding bids
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(70), 1, time));
		em.executeUntil(new TimeStamp(1));
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(89), 1, time));
		em.addActivity(new Clear(market, time));
		em.executeUntil(new TimeStamp(1));

		// Updating NBBO quote
		MockMarket market2 = new MockMarket(sip);
		Quote q = new Quote(market2, new Price(90), 1, new Price(100), 1, time);
		em.addActivity(new ProcessQuote(sip, market2, new DummyMarketTime(time, 1), q, 
				time));
		em.executeUntil(new TimeStamp(1));

		// Just to check that NBBO correct (it crosses)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(89), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(90), nbbo.getBestBid());

		// MM strategy
		em.addActivity(new AgentStrategy(marketmaker, time));
		em.executeUntil(new TimeStamp(1));

		// Check ladder of orders
		// market's orders contains all orders ever submitted
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 7, orders.size());
		// Verify that 3 rungs on buy side
		Order order = orders.get(2);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(60), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(3);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(65), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(4);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(70), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		// 2 rungs on truncated sell side
		order = orders.get(5);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(99), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(6);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(94), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}
}
