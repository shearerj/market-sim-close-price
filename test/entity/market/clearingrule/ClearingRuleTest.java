package entity.market.clearingrule;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import entity.market.MarketTime;
import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;
import fourheap.Order;

public class ClearingRuleTest {

	@Before
	public void setup() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		
		Order<Price, MarketTime> a = Order.create(new Price(100), 1, MarketTime.create(new TimeStamp(100),100));
		Order<Price, MarketTime> b = Order.create(new Price(100), 1, MarketTime.create(new TimeStamp(100),100));
		MatchedOrders<Price,MarketTime> orderPair =  MatchedOrders.create(a, b, 1);
		list.add(orderPair);
	}
	
	@Test
	public void EarliestPriceBasic() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		list.add(createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(105)));
		
		ClearingRule cr = new EarliestPriceClear(1);
		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		Set<MatchedOrders<Price,MarketTime>> keySet = result.keySet();
		for(MatchedOrders<Price,MarketTime> key : keySet) {
			assert(result.get(key).equals(new Price(110)));
		}
	}
	
	@Test
	public void EarliestPriceMulti() {
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
	
	@Test
	public void UniformPriceBasic() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		ClearingRule cr = new UniformPriceClear(0.5, 1);
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(105));
		list.add(match1);

		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		assert(result.get(match1).equals(new Price(105)));
	}
	
	@Test
	public void UniformPriceRatio() {
		ArrayList<MatchedOrders<Price,MarketTime>> list = new ArrayList<MatchedOrders<Price,MarketTime>>();
		ClearingRule cr = new UniformPriceClear(1, 1);
		
		MatchedOrders<Price,MarketTime> match1 = createOrderPair(new Price(110), 1, new TimeStamp(100), new Price(100), -1, new TimeStamp(105));
		list.add(match1);

		Map<MatchedOrders<Price,MarketTime>,Price> result = cr.pricing(list);
		
		assert(result.get(match1).equals(new Price(110)));
		
		cr = new UniformPriceClear(0, 1);
		result = cr.pricing(list);
		assert(result.get(match1).equals(new Price(100)));
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

		assert(result.get(match1).equals(new Price(105)));
		assert(result.get(match2).equals(new Price(105)));
	}
	
	public MatchedOrders<Price,MarketTime> createOrderPair(Price p1, int q1, TimeStamp t1, Price p2, int q2, TimeStamp t2){
		Order<Price, MarketTime> a = Order.create(p1, q1, MarketTime.create(t1,t1.getInTicks()));
		Order<Price, MarketTime> b = Order.create(p2, q2, MarketTime.create(t2,t2.getInTicks()));
		return MatchedOrders.create(a, b, Math.min(q1, q2));
	}

}
