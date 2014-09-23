package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.MockSim;

import com.google.common.collect.Lists;

import data.Props;
import entity.agent.Agent;
import entity.agent.Agent.AgentView;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import event.TimeStamp;
import fourheap.MatchedOrders;

public class EarliestPriceClearTest {
	// TODO Need these for filler... but we shouldn't. Maybe parameterize clearing rule
	private static final Random rand = new Random();
	private MockSim sim;
	private MarketView market;
	private AgentView agent;
	
	@Before
	public void setup() throws IOException {
		sim = MockSim.create(getClass(), Log.Level.NO_LOGGING);
		Market mark = new Market(sim, new UniformPriceClear(0.5, 1), null /*FIXME*/, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
		};
		market = mark.getPrimaryView();
		agent = new Agent(sim, TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
		}.getView(TimeStamp.IMMEDIATE);
	}

	@Test
	public void basicTest() {
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = Lists.newArrayList();
		list.add(createOrderPair(Price.of(110), 1, TimeStamp.of(100), 
								 Price.of(100), 1, TimeStamp.of(105)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime, Order>,Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price,MarketTime, Order>> keySet = result.keySet();
		for(MatchedOrders<Price,MarketTime, Order> key : keySet) {
			// Verify clears at the earlier price
			assertEquals(Price.of(110), result.get(key));
		}
	}
	
	@Test
	public void timeMatch(){
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = Lists.newArrayList();
		list.add(createOrderPair(Price.of(110), 1, TimeStamp.of(100), 
								 Price.of(100), 1, TimeStamp.of(100)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price, MarketTime, Order>, Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price, MarketTime, Order>> keySet = result.keySet();
		for(MatchedOrders<Price, MarketTime, Order> key : keySet) {
			// Verify for tie at time, it clears at the earlier price (because of MarketTime)
			assertEquals(Price.of(110), result.get(key));
		}
	}
	
	@Test
	public void multi() {
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = new ArrayList<MatchedOrders<Price, MarketTime, Order>>();
		
		MatchedOrders<Price, MarketTime, Order> match1 = createOrderPair(
				Price.of(110), 1, TimeStamp.of(100), 
				Price.of(100), 1, TimeStamp.of(105));
		list.add(match1);
		
		MatchedOrders<Price, MarketTime, Order> match2 = createOrderPair(
				Price.of(110), 1, TimeStamp.of(105), 
				Price.of(100), 1, TimeStamp.of(100));
		list.add(match2);
				
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price, MarketTime, Order>, Price> result = cr.pricing(list);

		// Verify always clears at the earlier price (no time ties here)
		assertEquals(Price.of(110), result.get(match1));
		assertEquals(Price.of(100), result.get(match2));
	}
	
	public MatchedOrders<Price, MarketTime, Order> createOrderPair(
			Price p1, int q1, TimeStamp t1, Price p2, int q2, TimeStamp t2){
		// NOTE: the same MarketTime will never be created for two orders
		MarketTime mt1 = MarketTime.from(t1, 1);
		MarketTime mt2 = MarketTime.from(t2, 2);
		Order a = Order.create(agent, OrderRecord.create(market, t1, BUY, p1, q1), mt1);
		Order b = Order.create(agent, OrderRecord.create(market, t2, SELL, p2, q2), mt2);
		// Generic for compartability with 1.6 compiler / non eclipse
		return MatchedOrders.<Price, MarketTime, Order> create(a, b, Math.min(q1, q2));
	}
}
