package entity.agent.strategy;

import entity.agent.OrderRecord;
import fourheap.Order.OrderType;

/**
 * 
 * FIXME There is one problem with this. In general when an agent submits an
 * OrderRecord it's not made positive or quantized. Since this returns an order
 * record to submit, it also doesn't. Maybe this is appropriate? Strategies may
 * want more control over how quantization happens.
 * 
 * @author erik
 * 
 */
public interface BackgroundStrategy {

	public OrderRecord getOrder(OrderType type, int quantity);

}
