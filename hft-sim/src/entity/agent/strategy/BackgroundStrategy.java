package entity.agent.strategy;

import entity.agent.OrderRecord;
import fourheap.Order.OrderType;

/**
 * 
 * @author erik
 * 
 */
public interface BackgroundStrategy {

	public OrderRecord getOrder(OrderType type, int quantity);
	
}
