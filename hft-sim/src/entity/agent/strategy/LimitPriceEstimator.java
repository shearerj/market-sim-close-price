package entity.agent.strategy;

import entity.market.Price;
import fourheap.Order.OrderType;

public interface LimitPriceEstimator {

	public Price getLimitPrice(OrderType buyOrSell, int quantity);
	
}
