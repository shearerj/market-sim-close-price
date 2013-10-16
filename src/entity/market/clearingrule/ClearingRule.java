package entity.market.clearingrule;

import java.io.Serializable;
import java.util.Map;


import entity.market.MarketTime;
import entity.market.Price;
import fourheap.MatchedOrders;

public interface ClearingRule extends Serializable {

	public Map<MatchedOrders<Price, MarketTime>, Price> pricing(
			Iterable<MatchedOrders<Price, MarketTime>> transactions);

}
