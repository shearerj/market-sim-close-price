package entity.agent;

import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import logger.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import systemmanager.Consts;
import systemmanager.Keys;
import activity.Activity;
import activity.AgentStrategy;
import com.google.common.collect.Iterables;

import data.EntityProperties;
import data.FundamentalValue;
import data.MockFundamental;
import entity.infoproc.SIP;
import entity.market.MockMarket;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;

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
	public void submitOrderLadderTest() {
		MarketMaker mm = new MockMarketMaker(fundamental, sip, market, 
				setupProperties(3, 5, false));
		mm.stepSize = 5;
		
		Iterable<? extends Activity> acts = mm.submitOrderLadder(new Price(30), new Price(40), 
				new Price(50), new Price(60), TimeStamp.ZERO);
		for (Activity a : acts) a.execute(TimeStamp.ZERO);
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
		MarketMaker mm = new MockMarketMaker(fundamental, sip, market, 
				setupProperties(2, 5, false));
		mm.stepSize = 5;
		
		Iterable<? extends Activity> acts = mm.createOrderLadder(null,
				new Price(50), TimeStamp.ZERO);
		assertEquals(0, Iterables.size(acts));
	}
	
	@Test
	public void createOrderLadderTest() {
		MarketMaker mm = new MockMarketMaker(fundamental, sip, market, 
				setupProperties(2, 5, false));
		mm.stepSize = 5;
		
		Iterable<? extends Activity> acts = mm.createOrderLadder(new Price(40),
				new Price(50), TimeStamp.ZERO);
		for (Activity a : acts) a.execute(TimeStamp.ZERO);
		assertEquals("Incorrect number of orders", 4, mm.activeOrders.size());
		for (Order o : mm.activeOrders) {
			int price = o.getPrice().intValue();
			if (o.getOrderType() == SELL) 
				assertTrue(price == 50 || price == 55);
			else
				assertTrue(price == 40 || price == 35);
		}
	}

}
