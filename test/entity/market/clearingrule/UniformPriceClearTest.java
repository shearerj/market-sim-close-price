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
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(105));
		list.add(match1);

		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		assertTrue(result.get(match1).equals(new Price(105)));
	}
	
	@Test
	public void UniformPriceRatio() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		ClearingRule cr = new UniformPriceClear(1, 1);
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(105));
		list.add(match1);

		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		assertTrue(result.get(match1).equals(new Price(110)));
		
		cr = new UniformPriceClear(0, 1);
		result = cr.pricing(list);
		assertTrue(result.get(match1).equals(new Price(100)));
	}
	
	@Test
	public void UniformPriceMulti() {
		ClearingRule cr = new UniformPriceClear(0.5, 1);

		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(105));
		list.add(match1);
		
		MatchedOrders<Price,MarketTime> match2 = createOrderPair(new Price(110), 1, new TimeStamp(105), new Price(100), -1, new TimeStamp(100));
		list.add(match2);
				
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);

		assertTrue(result.get(match1).equals(new Price(105)));
		assertTrue(result.get(match2).equals(new Price(105)));
	}

	public MatchedOrders<Price,MarketTime> createOrderPair(Price p1, int q1, TimeStamp t1, Price p2, int q2, TimeStamp t2){
		Order<Price, MarketTime> a = Order.create(p1, q1, MarketTime.create(t1,t1.getInTicks()));
		Order<Price, MarketTime> b = Order.create(p2, q2, MarketTime.create(t2,t2.getInTicks()));
		return MatchedOrders.create(a, b, Math.min(q1, q2));
	}

}
