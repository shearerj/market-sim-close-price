package entity.market.clearingrule;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

import systemmanager.Consts.OrderType;
import entity.market.MarketTime;
import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;
import fourheap.Order;

public class UniformPriceClearTest {

	@Test
	public void UniformPriceBasic() {
		ArrayList<MatchedOrders<OrderType, Price,MarketTime>> list = new ArrayList<MatchedOrders<OrderType, Price,MarketTime>>();
		ClearingRule cr = new UniformPriceClear(0.5, 1);
		
		MatchedOrders<OrderType, Price,MarketTime> match1 = createOrderPair(
				new Price(110), 1, new TimeStamp(100), 
				new Price(100), 1, new TimeStamp(105));
		list.add(match1);

		Map<MatchedOrders<OrderType, Price,MarketTime>,Price> result = cr.pricing(list);
		
		// Verify clearing at midpoint of the two orders (policy=0.5)
		assertEquals(new Price(105), result.get(match1));
	}
	
	@Test
	public void UniformPriceRatio() {
		ArrayList<MatchedOrders<OrderType, Price,MarketTime>> list = new ArrayList<MatchedOrders<OrderType, Price,MarketTime>>();
		ClearingRule cr = new UniformPriceClear(1, 1);
		
		MatchedOrders<OrderType, Price,MarketTime> match1 = createOrderPair(
				new Price(110), 1, new TimeStamp(100), 
				new Price(100), 1, new TimeStamp(105));
		list.add(match1);

		Map<MatchedOrders<OrderType, Price,MarketTime>,Price> result = cr.pricing(list);
		// Verify clearing at the higher price of the two orders (policy=1)
		assertEquals(new Price(110), result.get(match1));
		
		cr = new UniformPriceClear(0, 1);
		result = cr.pricing(list);
		// Verify clearing at the lower price of the two orders (policy=0)
		assertEquals(new Price(100), result.get(match1));
	}
	
	@Test
	public void UniformPriceMulti() {
		ClearingRule cr = new UniformPriceClear(0.5, 1);

		ArrayList<MatchedOrders<OrderType, Price,MarketTime>> list = new ArrayList<MatchedOrders<OrderType, Price,MarketTime>>();
		
		MatchedOrders<OrderType, Price,MarketTime> match1 = createOrderPair(
				new Price(110), 1, new TimeStamp(100), 
				new Price(100), 1, new TimeStamp(105));
		list.add(match1);
		
		MatchedOrders<OrderType, Price,MarketTime> match2 = createOrderPair(
				new Price(110), 1, new TimeStamp(105), 
				new Price(100), 1, new TimeStamp(100));
		list.add(match2);
				
		Map<MatchedOrders<OrderType, Price,MarketTime>,Price> result = cr.pricing(list);
		// Verify that for multiple orders, clears at correct midpoint price (policy=0.5)
		assertEquals(new Price(105), result.get(match1));
		assertEquals(new Price(105), result.get(match2));
		
		// Testing second set of prices
		list.clear();
		match1 = createOrderPair(new Price(110), 1, new TimeStamp(100), 
								 new Price(105), 1, new TimeStamp(105));
		list.add(match1);
		match2 = createOrderPair(new Price(104), 1, new TimeStamp(101), 
								 new Price(108), 1, new TimeStamp(102));
		list.add(match2);
		result = cr.pricing(list);
		// Verify that for multiple orders, clears at correct midpoint price (policy=0.5)
		// midpoint between BID=max(matched sells), ASK=min(matched buys)
		assertEquals(new Price(106), result.get(match1));
		assertEquals(new Price(106), result.get(match2));
	}
	
	
	/**
	 * Create matched order pair (buy, then sell)
	 * @param p1
	 * @param q1
	 * @param t1
	 * @param p2
	 * @param q2
	 * @param t2
	 * @return
	 */
	public MatchedOrders<OrderType, Price,MarketTime> createOrderPair(Price p1, int q1, 
			TimeStamp t1, Price p2, int q2, TimeStamp t2){
		Order<OrderType, Price, MarketTime> a = Order.create(OrderType.BUY, p1, q1, MarketTime.create(t1,t1.getInTicks()));
		Order<OrderType, Price, MarketTime> b = Order.create(OrderType.SELL, p2, q2, MarketTime.create(t2,t2.getInTicks()));
		return MatchedOrders.create(a, b, Math.min(q1, q2));
	}

}
