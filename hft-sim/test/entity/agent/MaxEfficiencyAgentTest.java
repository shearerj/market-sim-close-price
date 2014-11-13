package entity.agent;

import static fourheap.Order.OrderType.BUY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.ClearInterval;
import systemmanager.Keys.MaxPosition;
import systemmanager.Keys.PrivateValueVar;
import utils.Mock;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import entity.market.CDAMarket;
import entity.market.CallMarket;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

public class MaxEfficiencyAgentTest {
	
	private static Rand rand = Rand.create();
	
	private FundamentalValue fundamental = Mock.fundamental(100000);
	private Market market;
	
	@Before
	public void setup(){
		market = CallMarket.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs(
				ClearInterval.class, TimeStamp.of(10)));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void callMarket() {
		market = CDAMarket.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Props.fromPairs());
		MaxEfficiencyAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs());
	}
	
	@Test
	public void numOrdersTest() {
		Agent agent = MaxEfficiencyAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market, Props.fromPairs( 
				MaxPosition.class, 10,
				PrivateValueVar.class, 1000d));
		
		agent.agentStrategy();
		
		assertEquals(20, agent.getActiveOrders().size());
		int offset = 0;
		for (OrderRecord order : agent.getActiveOrders())
			offset += order.getOrderType().sign();
		assertEquals(0, offset); // equal number of buys and sells
		
	}
	
	@Test
	public void basicTest() {
		Agent agent = MaxEfficiencyAgent.create(0, Mock.stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, fundamental, market, Props.fromPairs( 
				MaxPosition.class, 1,
				PrivateValueVar.class, 1e7));
		
		agent.agentStrategy();
		
		assertEquals(2, agent.getActiveOrders().size());
		Price buyPrice = null, sellPrice = null;
		for (OrderRecord order : agent.getActiveOrders())
			if (order.getOrderType() == BUY)
				buyPrice = order.getPrice();
			else
				sellPrice = order.getPrice();
		
		assertNotNull(buyPrice);
		assertNotNull(sellPrice);
		assertTrue(buyPrice.lessThanEqual(sellPrice));	
	}
	
	// FIXME Test that position at end of simulation is correctly stored in stats
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			setup();
			basicTest();
			setup();
			numOrdersTest();
		}
	}
	
}
