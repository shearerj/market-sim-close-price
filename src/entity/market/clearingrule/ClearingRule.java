package entity.market.clearingrule;

import java.io.Serializable;
import java.util.Map;


import entity.market.Price;
import event.TimeStamp;
import fourheap.MatchedOrders;

public interface ClearingRule extends Serializable {

	public Map<MatchedOrders<Price, TimeStamp>, Price> pricing(Iterable<MatchedOrders<Price, TimeStamp>> transactions);
	
}
