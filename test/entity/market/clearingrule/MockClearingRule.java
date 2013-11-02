package entity.market.clearingrule;

import java.util.Map;

import systemmanager.Consts.OrderType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ImmutableMap.Builder;

import entity.market.MarketTime;
import entity.market.Price;
import fourheap.MatchedOrders;

public class MockClearingRule implements ClearingRule {

	private static final long serialVersionUID = 1L;
	
	protected final Price clearPrice;
	
	public MockClearingRule(Price clearPrice) {
		this.clearPrice = clearPrice;
	}
	
	@Override
	public Map<MatchedOrders<OrderType, Price, MarketTime>, Price> pricing(
			Iterable<MatchedOrders<OrderType, Price, MarketTime>> transactions) {
		if (Iterables.isEmpty(transactions)) return ImmutableMap.of();
		
		Builder<MatchedOrders<OrderType, Price, MarketTime>, Price> prices = ImmutableMap.builder();
		for (MatchedOrders<OrderType, Price, MarketTime> trans : transactions)
			prices.put(trans, clearPrice);
		return prices.build();
	}

}
