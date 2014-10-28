package entity.market;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.MockSim;
import data.Props;
import entity.agent.Agent;
import entity.agent.Agent.AgentView;
import entity.agent.position.PrivateValues;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import event.TimeStamp;
import fourheap.MatchedOrders;

public class UniformPriceClearTest {
	// TODO Need these for filler..., but shouldn't have these dependencies
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
		agent = new Agent(sim, PrivateValues.zero(), TimeStamp.ZERO, rand, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override public void agentStrategy() { }
		}.getView(TimeStamp.IMMEDIATE);
	}

	@Test
	public void UniformPriceBasic() {
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = new ArrayList<MatchedOrders<Price, MarketTime, Order>>();
		ClearingRule cr = new UniformPriceClear(0.5, 1);
		
		MatchedOrders<Price, MarketTime, Order> match1 = createOrderPair(
				Price.of(110), 1, TimeStamp.of(100), 
				Price.of(100), 1, TimeStamp.of(105));
		list.add(match1);

		Map<MatchedOrders<Price, MarketTime, Order>, Price> result = cr.pricing(list);
		
		// Verify clearing at midpoint of the two orders (policy=0.5)
		assertEquals(Price.of(105), result.get(match1));
	}
	
	@Test
	public void UniformPriceRatio() {
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = new ArrayList<MatchedOrders<Price, MarketTime, Order>>();
		ClearingRule cr = new UniformPriceClear(1, 1);
		
		MatchedOrders<Price, MarketTime, Order> match1 = createOrderPair(
				Price.of(110), 1, TimeStamp.of(100), 
				Price.of(100), 1, TimeStamp.of(105));
		list.add(match1);

		Map<MatchedOrders<Price, MarketTime, Order>,Price> result = cr.pricing(list);
		// Verify clearing at the higher price of the two orders (policy=1)
		assertEquals(Price.of(110), result.get(match1));
		
		cr = new UniformPriceClear(0, 1);
		result = cr.pricing(list);
		// Verify clearing at the lower price of the two orders (policy=0)
		assertEquals(Price.of(100), result.get(match1));
	}
	
	@Test
	public void UniformPriceMulti() {
		ClearingRule cr = new UniformPriceClear(0.5, 1);

		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = new ArrayList<MatchedOrders<Price, MarketTime, Order>>();
		
		MatchedOrders<Price, MarketTime, Order> match1 = createOrderPair(
				Price.of(110), 1, TimeStamp.of(100), 
				Price.of(100), 1, TimeStamp.of(105));
		list.add(match1);
		
		MatchedOrders<Price, MarketTime, Order> match2 = createOrderPair(
				Price.of(110), 1, TimeStamp.of(105), 
				Price.of(100), 1, TimeStamp.of(100));
		list.add(match2);
				
		Map<MatchedOrders<Price, MarketTime, Order>,Price> result = cr.pricing(list);
		// Verify that for multiple orders, clears at correct midpoint price (policy=0.5)
		assertEquals(Price.of(105), result.get(match1));
		assertEquals(Price.of(105), result.get(match2));
		
		// Testing second set of prices
		list.clear();
		match1 = createOrderPair(Price.of(110), 1, TimeStamp.of(100), 
								 Price.of(105), 1, TimeStamp.of(105));
		list.add(match1);
		match2 = createOrderPair(Price.of(104), 1, TimeStamp.of(101), 
								 Price.of(108), 1, TimeStamp.of(102));
		list.add(match2);
		result = cr.pricing(list);
		// Verify that for multiple orders, clears at correct midpoint price (policy=0.5)
		// midpoint between BID=max(matched sells), ASK=min(matched buys)
		assertEquals(Price.of(106), result.get(match1));
		assertEquals(Price.of(106), result.get(match2));
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
