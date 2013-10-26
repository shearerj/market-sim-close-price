package entity.market.clearingrule;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import entity.market.MarketTime;
import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;
import fourheap.Order;

public class EarliestPriceClearTest {

	@Test
	public void Basic() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		list.add(createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(105)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price,MarketTime>> keySet = result.keySet();
		for(MatchedOrders<Price,MarketTime> key : keySet) {
			assertTrue(result.get(key).equals(new Price(110)));
		}
	}
	
	@Test
	public void TimeMatch(){
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		list.add(createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(100)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price,MarketTime>> keySet = result.keySet();
		for(MatchedOrders<Price,MarketTime> key : keySet) {
			assertTrue(result.get(key).equals(new Price(100)));
		}
	}
	
	@Test
	public void Multi() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(105));
		list.add(match1);
		
		MatchedOrders<Price,MarketTime> match2 = createOrderPair(new Price(110), 1, new TimeStamp(105), new Price(100), -1, new TimeStamp(100));
		list.add(match2);
				
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);

		assert(result.get(match1).equals(new Price(110)));
		assert(result.get(match2).equals(new Price(100)));
	}
	
	public MatchedOrders<Price,MarketTime> createOrderPair(Price p1, int q1, TimeStamp t1, Price p2, int q2, TimeStamp t2){
		Order<Price, MarketTime> a = Order.create(p1, q1, MarketTime.create(t1,t1.getInTicks()));
		Order<Price, MarketTime> b = Order.create(p2, q2, MarketTime.create(t2,t2.getInTicks()));
		return MatchedOrders.create(a, b, Math.min(q1, q2));
	}
}
