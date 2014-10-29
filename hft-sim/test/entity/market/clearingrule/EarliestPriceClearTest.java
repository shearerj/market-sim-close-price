package entity.market.clearingrule;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import utils.Mock;

import com.google.common.collect.Lists;

import entity.agent.Agent.AgentView;
import entity.agent.OrderRecord;
import entity.market.Market.MarketView;
import entity.market.MarketTime;
import entity.market.Order;
import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;
import fourheap.Order.OrderType;

public class EarliestPriceClearTest {
	private static final MarketView market = Mock.market().getPrimaryView();
	private static final AgentView agent = Mock.agent().getView(TimeStamp.ZERO);

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
	public void timeMatchBuy(){
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = Lists.newArrayList();
		list.add(createOrderPair(Price.of(110), 1, TimeStamp.of(100), 
								 Price.of(100), 1, TimeStamp.of(100), BUY));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price, MarketTime, Order>, Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price, MarketTime, Order>> keySet = result.keySet();
		for(MatchedOrders<Price, MarketTime, Order> key : keySet) {
			// Verify for tie at time, it clears at the earlier (buy) price (because of MarketTime)
			assertEquals(Price.of(110), result.get(key));
		}
	}
	
	@Test
	public void timeMatchSell(){
		ArrayList<MatchedOrders<Price, MarketTime, Order>> list = Lists.newArrayList();
		list.add(createOrderPair(Price.of(110), 1, TimeStamp.of(100), 
								 Price.of(100), 1, TimeStamp.of(100), SELL));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price, MarketTime, Order>, Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price, MarketTime, Order>> keySet = result.keySet();
		for(MatchedOrders<Price, MarketTime, Order> key : keySet) {
			// Verify for tie at time, it clears at the earlier (sell) price (because of MarketTime)
			assertEquals(Price.of(100), result.get(key));
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
	
	private static MatchedOrders<Price, MarketTime, Order> createOrderPair(
			Price p1, int q1, TimeStamp t1, Price p2, int q2, TimeStamp t2) {
		return createOrderPair(p1, q1, t1, p2, q2, t2, BUY);
	}
	
	private static MatchedOrders<Price, MarketTime, Order> createOrderPair(
			Price p1, int q1, TimeStamp t1, Price p2, int q2, TimeStamp t2, OrderType first) {
		// NOTE: the same MarketTime will never be created for two orders
		MarketTime mt1 = MarketTime.from(t1, first == BUY ? 0 : 1);
		MarketTime mt2 = MarketTime.from(t2, first == BUY ? 1 : 0);
		Order a = Order.create(agent, OrderRecord.create(market, t1, BUY, p1, q1), mt1);
		Order b = Order.create(agent, OrderRecord.create(market, t2, SELL, p2, q2), mt2);
		return MatchedOrders.<Price, MarketTime, Order> create(a, b, Math.min(q1, q2));
	}
}
