package entity.market.clearingrule;

import java.io.Serializable;
import java.util.Map;

import systemmanager.Consts.OrderType;
import entity.market.MarketTime;
import entity.market.Price;
import fourheap.MatchedOrders;

public interface ClearingRule extends Serializable {

	public Map<MatchedOrders<OrderType, Price, MarketTime>, Price> pricing(
			Iterable<MatchedOrders<OrderType, Price, MarketTime>> transactions);

}
