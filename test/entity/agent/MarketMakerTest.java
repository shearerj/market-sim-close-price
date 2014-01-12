package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.EventManager;
import systemmanager.Keys;
import activity.Activity;
import activity.AgentStrategy;
import activity.Clear;
import activity.SubmitOrder;

import com.google.common.collect.Iterables;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import entity.market.Quote;
import event.TimeStamp;
import fourheap.Order.OrderType;

public class MarketMakerTest {

	private MockMarket market;
	private SIP sip;
	private FundamentalValue fundamental = new MockFundamental(100000);

	@BeforeClass
	public static void setupClass() {
		Logger.setup(3, new File(Consts.TEST_OUTPUT_DIR + "BasicMarketMakerTest.log"));
	}

	@Before
	public void setup() {
		sip = new SIP(TimeStamp.IMMEDIATE);
		market = new MockMarket(sip);
	}

	private EntityProperties setupProperties(int numRungs, int rungSize, 
			boolean truncateLadder) {
		EntityProperties agentProperties = new EntityProperties();
		agentProperties.put(Keys.NUM_RUNGS, numRungs);
		agentProperties.put(Keys.RUNG_SIZE, rungSize);
		agentProperties.put(Keys.TRUNCATE_LADDER, truncateLadder);
		return agentProperties;
	}

	@Test
	public void nullBidAsk() {
		// testing when no bid/ask, does not submit any orders
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new MockMarketMaker(fundamental, sip, market, 
				setupProperties(2, 10, false));

		// Check activities inserted (none, other than reentry)
		Iterable<? extends Activity> acts = mm.agentStrategy(time);
		assertEquals("Incorrect number of activities", 1, Iterables.size(acts));
		assertTrue(Iterables.getFirst(acts, null) instanceof AgentStrategy);
	}
	
	@Test
	public void basicLadderTest() {
		TimeStamp time = TimeStamp.ZERO;

		MarketMaker mm = new MockMarketMaker(fundamental, sip, market, 
				setupProperties(2, 10, false));

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
	
	// TODO test submitOrderLadder only
	
	
	// TODO test createOrderLadder as well
	
}
