package entity.market.clearingrule;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

import entity.market.MarketTime;
import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;
import fourheap.Order;

public class UniformPriceClearTest {

	@Test
	public void UniformPriceBasic() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		ClearingRule cr = new UniformPriceClear(0.5, 1);
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(
				new Price(110), 1, new TimeStamp(100), 
				new Price(100), -1, new TimeStamp(105));
		list.add(match1);

		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		// Verify clearing at midpoint of the two orders (policy=0.5)
		assertEquals(result.get(match1), new Price(105));
	}
	
	@Test
	public void UniformPriceRatio() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		ClearingRule cr = new UniformPriceClear(1, 1);
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(
				new Price(110), 1, new TimeStamp(100), 
				new Price(100), -1, new TimeStamp(105));
		list.add(match1);

		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		// Verify clearing at the higher price of the two orders (policy=1)
		assertEquals(result.get(match1), new Price(110));
		
		cr = new UniformPriceClear(0, 1);
		result = cr.pricing(list);
		// Verify clearing at the lower price of the two orders (policy=0)
		assertEquals(result.get(match1), new Price(100));
	}
	
	@Test
	public void UniformPriceMulti() {
		ClearingRule cr = new UniformPriceClear(0.5, 1);

		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(
				new Price(110), 1, new TimeStamp(100), 
				new Price(100), -1, new TimeStamp(105));
		list.add(match1);
		
		MatchedOrders<Price,MarketTime> match2 = createOrderPair(
				new Price(110), 1, new TimeStamp(105), 
				new Price(100), -1, new TimeStamp(100));
		list.add(match2);
				
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		// Verify that for multiple orders, clears at correct midpoint price (policy=0.5)
		assertEquals(result.get(match1), new Price(105));
		assertEquals(result.get(match2), new Price(105));
		
		// Testing second set of prices
		list.clear();
		match1 = createOrderPair(new Price(110), 1, new TimeStamp(100), 
								 new Price(105), -1, new TimeStamp(105));
		list.add(match1);
		match2 = createOrderPair(new Price(104), 1, new TimeStamp(101), 
								 new Price(108), -1, new TimeStamp(102));
		list.add(match2);
		result = cr.pricing(list);
		// Verify that for multiple orders, clears at correct midpoint price (policy=0.5)
		// midpoint between BID=max(matched sells), ASK=min(matched buys)
		assertEquals(result.get(match1), new Price(106));
		assertEquals(result.get(match2), new Price(106));
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
		Order<Price, MarketTime> a = Order.create(p1, q1, MarketTime.create(t1,t1.getInTicks()));
		Order<Price, MarketTime> b = Order.create(p2, q2, MarketTime.create(t2,t2.getInTicks()));
		return MatchedOrders.create(a, b, Math.min(q1, q2));
	}

}
