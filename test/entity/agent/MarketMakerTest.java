package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static logger.Log.Level.*;
import static logger.Log.log;

import java.io.File;
import java.io.IOException;

import logger.Log;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Executor;
import systemmanager.Keys;
import activity.Clear;
import activity.ProcessQuote;
import activity.SubmitOrder;
import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.DummyMarketTime;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;

public class MarketMakerTest {

	private Executor exec;
	private MockMarket market;
	private SIP sip;
	private FundamentalValue fundamental = new MockFundamental(100000);

	@BeforeClass
	public static void setupClass() throws IOException {
		log = Log.create(DEBUG, new File(Consts.TEST_OUTPUT_DIR + "MarketMakerTest.log"));
	}

	@Before
	public void setup() {
		exec = new Executor();
		sip = new SIP(exec, TimeStamp.IMMEDIATE);
		market = new MockMarket(exec, sip);
	}

	// TODO test different tick size & subsequent step/rung size difference

	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, does not submit any orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 10,
				Keys.TRUNCATE_LADDER, false));

		// Check activities inserted (none, other than reentry)
		mm.agentStrategy(time);
		assertTrue(mm.activeOrders.isEmpty());
	}

	@Test
	public void submitOrderLadderTest() {
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 3,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false));
		mm.stepSize = 5;

		mm.submitOrderLadder(new Price(30), new Price(40), new Price(50), new Price(60));
		assertEquals("Incorrect number of orders", 6, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 50 || price == 55 || price == 60);
			else
				assertTrue(price == 40 || price == 35 || price == 30);
		}
	}

	@Test
	public void createOrderLadderNullTest() {
		// if either ladder bid or ask is null, it needs to return
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false));
		assertEquals(5, mm.stepSize);

		mm.createOrderLadder(null, new Price(50));
		assertTrue(mm.activeOrders.isEmpty());
	}

	@Test
	public void createOrderLadderTest() {
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false));
		assertEquals(5, mm.stepSize);

		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 50 || price == 55);
			else
				assertTrue(price == 40 || price == 35);
		}
	}


	@Test
	public void tickImprovement() {
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_INSIDE, true)); // default
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));

		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 51 || price == 56);
			else
				assertTrue(price == 39 || price == 34);
		}
	}

	@Test
	public void truncateLadderTickImprovement() {
		TimeStamp time = TimeStamp.ZERO;
		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		exec.executeActivity(new Clear(market));

		// Updating NBBO quote (this will update immed, although SIP is delayed)
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(30), 1, new Price(38), 1, time);
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 3, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 51 || price == 56);
			else
				assertEquals(34, price);
		}
	}
	
	@Test
	public void tickOutside() {
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, false,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_INSIDE, false));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));
		
		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 49 || price == 54);
			else
				assertTrue(price == 41 || price == 36);
		}
	}
	
	@Test
	public void truncateLadderTickImprovementOutside() {
		TimeStamp time = TimeStamp.ZERO;
		SIP sip = new SIP(exec, TimeStamp.create(10));
		MarketMaker mm = new MockMarketMaker(exec, fundamental, sip, market, EntityProperties.fromPairs(
				Keys.NUM_RUNGS, 2,
				Keys.RUNG_SIZE, 5,
				Keys.TRUNCATE_LADDER, true,
				Keys.TICK_IMPROVEMENT, true,
				Keys.TICK_INSIDE, false));
		assertEquals(5, mm.stepSize);

		// Creating dummy agents
		MockBackgroundAgent agent1 = new MockBackgroundAgent(exec, fundamental, sip, market);
		MockBackgroundAgent agent2 = new MockBackgroundAgent(exec, fundamental, sip, market);

		// Creating and adding bids
		exec.executeActivity(new SubmitOrder(agent1, market, BUY, new Price(40), 1));
		exec.executeActivity(new SubmitOrder(agent2, market, SELL, new Price(50), 1));

		// Updating NBBO quote (this will update immed, although SIP is delayed)
		MockMarket market2 = new MockMarket(exec, sip);
		Quote q = new Quote(market2, new Price(30), 1, new Price(38), 1, new DummyMarketTime(time, 1));
		exec.executeActivity(new ProcessQuote(sip, market2, q));

		mm.createOrderLadder(new Price(40), new Price(50));
		assertEquals("Incorrect number of orders", 3, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 49 || price == 54);
			else
				assertEquals(36, price);
		}
	}
}
