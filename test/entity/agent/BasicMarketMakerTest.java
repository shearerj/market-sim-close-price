package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.*;
import static logger.Log.Level.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Iterables;

import activity.Activity;
import activity.AgentStrategy;
import activity.Clear;
import activity.ProcessQuote;
import activity.SubmitOrder;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import systemmanager.Consts;
import systemmanager.EventManager;
import systemmanager.Keys;
import entity.infoproc.BestBidAsk;
import entity.infoproc.SIP;
import entity.market.DummyMarketTime;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class BasicMarketMakerTest {

	private MockMarket market;
	private SIP sip;
	private FundamentalValue fundamental = new MockFundamental(100000);

	@BeforeClass
	public static void setupClass() throws IOException {
		Log.log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "BasicMarketMakerTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip);
	}

	private EntityProperties setupProperties(int numRungs, int rungSize, 
			boolean truncateLadder, int tickSize) {
		EntityProperties agentProperties = new EntityProperties();
		agentProperties.put(Keys.NUM_RUNGS, numRungs);
		agentProperties.put(Keys.RUNG_SIZE, rungSize);
		agentProperties.put(Keys.TRUNCATE_LADDER, truncateLadder);
		agentProperties.put(Keys.TICK_IMPROVEMENT, false);
		agentProperties.put(Keys.TICK_SIZE, tickSize);
		agentProperties.put(Keys.REENTRY_RATE, 0);
		return agentProperties;
	}

	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, does not submit any orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(2, 10, false, 1));

		// Check activities inserted (none, other than reentry)
		Iterable<? extends Activity> acts = mm.agentStrategy(time);
		assertEquals("Incorrect number of activities", 1, Iterables.size(acts));
		assertTrue(Iterables.getFirst(acts, null) instanceof AgentStrategy);
	}

	/**
	 * When the quote is undefined (either bid or ask is null) but prior quote
	 * was defined, then the market maker should not do anything.
	 */
	@Test
	public void quoteUndefined() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(2, 10, false, 1));
		mm.lastAsk = new Price(55);
		mm.lastBid = new Price(45);

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(null, quote.getAskPrice());
		assertEquals(null, quote.getBidPrice());
		
		// Check activities inserted (none, other than reentry)
		Iterable<? extends Activity> acts = mm.agentStrategy(time);
		assertEquals("Incorrect number of activities", 1, Iterables.size(acts));
		assertTrue(Iterables.getFirst(acts, null) instanceof AgentStrategy);
		assertEquals(0, mm.activeOrders.size());
		
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		
		// Creating and adding bids
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1, time));
		em.executeUntil(new TimeStamp(1));
		// Check market quote
		quote = market.getQuoteProcessor().getQuote();
		assertEquals(null, quote.getAskPrice());
		assertEquals(new Price(40), quote.getBidPrice());
		
		// Check activities inserted (none, other than reentry)
		mm.lastAsk = new Price(55);
		mm.lastBid = new Price(45);
		acts = mm.agentStrategy(time);
		assertEquals("Incorrect number of activities", 1, Iterables.size(acts));
		assertTrue(Iterables.getFirst(acts, null) instanceof AgentStrategy);
		assertEquals(0, mm.activeOrders.size());
	}
	
	
	@Test
	public void basicLadderTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(2, 10, false, 1));

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
	 * Check when quote changes in between reentries
	 */
	@Test
	public void quoteChangeTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(10);

		MarketMaker marketmaker = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(2, 10, false, 1));

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

		// Quote change
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(42), 1, time));
		em.executeUntil(new TimeStamp(1));
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(48), 1, time));
		em.addActivity(new Clear(market, time));
		em.executeUntil(new TimeStamp(1));
		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(48), quote.getAskPrice());
		assertEquals(new Price(42), quote.getBidPrice());

		// Next MM strategy execution
		em.addActivity(new AgentStrategy(marketmaker, time1));
		em.executeUntil(new TimeStamp(11));

		// Check ladder of orders, previous orders withdrawn
		// market's orders contains all orders ever submitted
		ArrayList<Order> orders = new ArrayList<Order>(market.getOrders());
		assertEquals("Incorrect number of orders", 12, orders.size());
		assertEquals(agent1, orders.get(6).getAgent());
		assertEquals(new Price(42), orders.get(6).getPrice());
		assertEquals(agent2, orders.get(7).getAgent());
		assertEquals(new Price(48), orders.get(7).getPrice());

		Order order = orders.get(8);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(32), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());
		order = orders.get(9);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(42), order.getPrice());
		assertEquals(OrderType.BUY, order.getOrderType());

		order = orders.get(10);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(58), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
		order = orders.get(11);
		assertEquals(marketmaker, order.getAgent());
		assertEquals(new Price(48), order.getPrice());
		assertEquals(OrderType.SELL, order.getOrderType());
	}

	/**
	 * Check changing numRungs, rungSize
	 */
	@Test
	public void rungsTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker marketmaker = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(3, 12, false, 5));

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
		EntityProperties agentProperties = setupProperties(2, 10, false, 1);
		agentProperties.put(Keys.NO_OP, true);

		MarketMaker mm = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), agentProperties);

		// Check activities inserted (none, since no-op)
		Iterable<? extends Activity> acts = mm.agentStrategy(TimeStamp.ZERO);
		assertEquals("Incorrect number of activities", 0, Iterables.size(acts));
	}

	@Test
	public void truncateBidTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker marketmaker = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(3, 5, true, 1));

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

		MarketMaker marketmaker = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(3, 5, true, 1));

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

	@Test
	public void withdrawLadderTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		MarketMaker marketmaker = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(3, 5, true, 1));
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		// Creating and adding bids
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1, time));
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1, time));
		em.executeUntil(new TimeStamp(1));

		// Initial MM strategy; submits ladder with numRungs=3
		em.addActivity(new AgentStrategy(marketmaker, time));
		em.executeUntil(new TimeStamp(1));
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		// Withdraw other orders & submit new orders
		agent1.withdrawAllOrders(time);
		assertEquals(0, agent1.activeOrders.size());
		agent2.withdrawAllOrders(time);
		assertEquals(0, agent2.activeOrders.size());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(42), 1, time1));
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(49), 1, time1));
		em.executeUntil(new TimeStamp(2));
		
		// Verify that it withdraws ladder entirely & submits new ladder
		em.addActivity(new AgentStrategy(marketmaker, time1));
		em.executeUntil(new TimeStamp(2));
		assertNotNull(marketmaker.lastBid);
		assertNotNull(marketmaker.lastAsk);
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 42 || price == 37 || price == 32);
			else
				assertTrue(price == 49 || price == 54 || price == 59);
		}
	}
	
	/**
	 * Case where withdrawing the ladder causes the quote to become undefined
	 * (as well as the last NBBO quote)
	 */
	@Test
	public void withdrawUndefinedTest() {
		TimeStamp time = TimeStamp.ZERO;
		TimeStamp time1 = new TimeStamp(1);

		MarketMaker marketmaker = new BasicMarketMaker(fundamental, sip, market, 
				new Random(), setupProperties(3, 5, true, 1));
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(fundamental, sip, market);

		// Creating and adding bids
		EventManager em = new EventManager(new Random());
		em.addActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1, time));
		em.addActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1, time));
		em.executeUntil(new TimeStamp(1));

		// Initial MM strategy; submits ladder with numRungs=3
		em.addActivity(new AgentStrategy(marketmaker, time));
		em.executeUntil(new TimeStamp(1));
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());

		// Withdraw other orders
		agent1.withdrawAllOrders(time);
		assertEquals(0, agent1.activeOrders.size());
		marketmaker.lastBid = new Price(42); // to make sure MM will withdraw its orders
		
		// Verify that it withdraws ladder entirely
		// Note that now the quote is undefined, after it withdraws its ladder
		// so it will submit a ladder with the lastBid
		em.addActivity(new AgentStrategy(marketmaker, time1));
		em.executeUntil(new TimeStamp(2));
		assertNotNull(marketmaker.lastBid);
		assertNotNull(marketmaker.lastAsk);
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 42 || price == 37 || price == 32);
			else
				assertTrue(price == 50 || price == 55 || price == 60);
		}
	}
}
