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
		list.add(createOrderPair(new Price(110), 1, new TimeStamp(100), 
								 new Price(100), -1, new TimeStamp(105)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price,MarketTime>> keySet = result.keySet();
		for(MatchedOrders<Price,MarketTime> key : keySet) {
			// Verify clears at the earlier price
			assertEquals(new Price(110), result.get(key));
		}
	}
	
	@Test
	public void TimeMatch(){
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		list.add(createOrderPair(new Price(110), 1, new TimeStamp(100), 
								 new Price(100), -1, new TimeStamp(100)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price,MarketTime>> keySet = result.keySet();
		for(MatchedOrders<Price,MarketTime> key : keySet) {
			// Verify for tie at time, it clears at the earlier price (because of MarketTime)
			assertEquals(new Price(110), result.get(key));
		}
	}
	
	@Test
	public void Multi() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(
				new Price(110), 1, new TimeStamp(100), 
				new Price(100), -1, new TimeStamp(105));
		list.add(match1);
		
		MatchedOrders<Price,MarketTime> match2 = createOrderPair(
				new Price(110), 1, new TimeStamp(105), 
				new Price(100), -1, new TimeStamp(100));
		list.add(match2);
				
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);

		// Verify always clears at the earlier price (no time ties here)
		assertEquals(new Price(110), result.get(match1));
		assertEquals(new Price(100), result.get(match2));
	}
	
	/**
	 * Create matched order pair
	 * @param p1
	 * @param q1
	 * @param t1
	 * @param p2
	 * @param q2
	 * @param t2
	 * @return
	 */
	public MatchedOrders<Price,MarketTime> createOrderPair(Price p1, int q1, 
			TimeStamp t1, Price p2, int q2, TimeStamp t2){
		// NOTE: the same MarketTime will never be created for two orders
		// So if t1 == t2, t2 will be created at an incremented MarketTime
		Order<Price, MarketTime> a = Order.create(p1, q1, MarketTime.create(t1, t1.getInTicks()));
		Order<Price, MarketTime> b = Order.create(p2, q2, 
				MarketTime.create(t2, t2.plus(new TimeStamp(t2.equals(t1) ? 1 : 0)).getInTicks()));
		return MatchedOrders.create(a, b, Math.min(q1, q2));
	}
}
