package entity.market;

import java.io.Serializable;
import java.util.Map;

import fourheap.MatchedOrders;

public interface ClearingRule extends Serializable {

	public Map<MatchedOrders<Price, MarketTime, Order>, Price> pricing(
			Iterable<MatchedOrders<Price, MarketTime, Order>> transactions);

}
