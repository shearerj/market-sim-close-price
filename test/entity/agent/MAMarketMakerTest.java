package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import activity.AgentStrategy;
import activity.Clear;
import activity.ProcessQuote;
import activity.SubmitOrder;
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

	private Executor exec;
	private MockMarket market;
	private SIP sip;
	private FundamentalValue fundamental = new MockFundamental(100000);

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "MAMarketMakerTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}
	
	private MAMarketMaker createMAMM(int numRungs, int rungSize, 
			boolean truncateLadder, int tickSize, int numHistorical) {
		return new MAMarketMaker(exec, fundamental, sip, market, new Random(),
				tickSize, false, numRungs, rungSize, truncateLadder, false, true,
				numHistorical);
	}

	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, does not submit any orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = createMAMM(2, 10, false, 1, 5);

		// Check activities inserted (none, other than reentry)
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());
	}

	/**
	 * When the quote is undefined (either bid or ask is null) but prior quote
	 * was defined, then the market maker should not do anything.
	 */
	@Test
	public void quoteUndefined() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = createMAMM(2, 10, false, 1, 5);
		mm.lastAsk = new Price(55);
		mm.lastBid = new Price(45);

		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(null, quote.getAskPrice());
		assertEquals(null, quote.getBidPrice());
		
		// Check activities inserted (none, other than reentry)
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());
		
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		
		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		// Check market quote
		quote = market.getQuoteProcessor().getQuote();
		assertEquals(null, quote.getAskPrice());
		assertEquals(new Price(40), quote.getBidPrice());
		
		// Check activities inserted (none, other than reentry)
		mm.lastAsk = new Price(55);
		mm.lastBid = new Price(45);
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());
	}
	
	/**
	 * Case where withdrawing the ladder causes the quote to become undefined
	 * (as well as the last NBBO quote)
	 */
	@Test
	public void withdrawUndefinedTest() {
		TimeStamp time1 = TimeStamp.create(1);

		MarketMaker marketmaker = createMAMM(3, 5, true, 1, 5);
		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));

		// Initial MM strategy; submits ladder with numRungs=3
		exec.executeActivity(new AgentStrategy(marketmaker));
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		
		// To change the bid stored in the MM
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(48), 1));
		exec.executeActivity(new AgentStrategy(marketmaker));
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 44 || price == 39 || price == 34);
		}
		// Withdraw other orders (now quote is (41, 50) from MM)
		agent1.withdrawAllOrders();
		agent2.withdrawAllOrders();
		
		exec.setTime(time1);
		// Note that now the quote is undefined, after it withdraws its ladder
		// so it will insert lastBid/Ask into the queues so the ladder changes
		exec.executeActivity(new AgentStrategy(marketmaker));
		assertNotNull(marketmaker.lastBid);
		assertNotNull(marketmaker.lastAsk);
		assertEquals("Incorrect number of orders", 6, marketmaker.activeOrders.size());
		for (Order o : marketmaker.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == BUY) 
				assertTrue(price == 45 || price == 40 || price == 35);
			else
				assertTrue(price == 50 || price == 55 || price == 60);
		}
	}
	
	/**
	 * Check that bid and ask queues are updating correctly, and moving average
	 * is computed correctly
	 */
	@Test
	public void computeMovingAverage() {
		TimeStamp time = TimeStamp.ZERO;

		MAMarketMaker mm = createMAMM(1, 10, false, 1, 5);

		QuoteProcessor qp = mm.marketQuoteProcessor;

		// Add quotes & execute agent strategy in between (without actually submitting orders)
		int mktTime = 0;
		addQuote(qp, 50, 60, 0, mktTime += 5);
		mm.agentStrategy(time);
		addQuote(qp, 52, 64, 0, mktTime += 5);
		mm.agentStrategy(time);
		addQuote(qp, 54, 68, 0, mktTime += 5);
		mm.agentStrategy(time);
		addQuote(qp, 56, 72, 0, mktTime += 5);
		mm.agentStrategy(time);
		addQuote(qp, 58, 76, 0, mktTime += 5);
		mm.agentStrategy(time);

		assertEquals(new Price(58), mm.lastBid);
		assertEquals(new Price(76), mm.lastAsk);

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

	private void addQuote(QuoteProcessor qp, int buy, int sell, int time, int marketTime) {
		TimeStamp ts = TimeStamp.create(time);
		MarketTime mktTime = new DummyMarketTime(ts, marketTime);
		Quote q = new Quote(market, new Price(buy), 1, new Price(sell), 1, mktTime);
		sip.processQuote(market, q, ts);
		qp.processQuote(market, q, ts);
	}

	/**
	 * Verify that bids/asks are being evicted correctly
	 */
	@Test
	public void evictingQueueTest() {
		TimeStamp time = TimeStamp.ZERO;

		MAMarketMaker mm = createMAMM(2, 10, false, 1, 3);

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

		MarketMaker mm = createMAMM(2, 10, false, 1, 5);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));
		// Check market quote
		Quote quote = market.getQuoteProcessor().getQuote();
		assertEquals(new Price(50), quote.getAskPrice());
		assertEquals(new Price(40), quote.getBidPrice());

		// Check activities inserted (4 submit orders plus agent reentry)
		mm.agentStrategy(time);

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

		MAMarketMaker mm = createMAMM(1, 10, false, 1, 5);

		QuoteProcessor qp = mm.marketQuoteProcessor;

		// Add quotes & execute agent strategy in between (without actually submitting orders)
		// FIXME This is not possible in the new system
		int mktTime = 0;
		addQuote(qp, 50, 60, 0, mktTime += 5);
		mm.agentStrategy(time);
		addQuote(qp, 52, 64, 0, mktTime += 5);
		mm.agentStrategy(time);
		addQuote(qp, 54, 68, 0, mktTime += 5);
		mm.agentStrategy(time);

		assertEquals(new Price(54), mm.lastBid);
		assertEquals(new Price(68), mm.lastAsk);

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
		MarketMaker marketmaker = createMAMM(3, 12, false, 5, 5);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));

		// Initial MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

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
		MarketMaker mm = createMAMM(2, 10, false, 1, 5);
		mm.noOp = true;

		// Check activities inserted (none, since no-op)
		mm.agentStrategy(TimeStamp.ZERO);
		assertTrue(mm.activeOrders.isEmpty());
	}

	@Test
	public void truncateBidTest() {
		MarketMaker marketmaker = createMAMM(3, 5, true, 1, 5);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(102), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(105), 1));
		exec.executeActivity(new Clear(market));

		// Updating NBBO quote
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(90), 1, new Price(100), 1, TimeStamp.ZERO);
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		// Just to check that NBBO correct (it crosses)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(100), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(102), nbbo.getBestBid());

		// MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

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
		MarketMaker marketmaker = createMAMM(3, 5, true, 1, 5);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(70), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(89), 1));
		exec.executeActivity(new Clear(market));

		// Updating NBBO quote
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(90), 1, new Price(100), 1, TimeStamp.ZERO);
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		// Just to check that NBBO correct (it crosses)
		BestBidAsk nbbo = sip.getNBBO();
		assertEquals("Incorrect ASK", new Price(89), nbbo.getBestAsk());
		assertEquals("Incorrect BID", new Price(90), nbbo.getBestBid());

		// MM strategy
		exec.executeActivity(new AgentStrategy(marketmaker));

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
