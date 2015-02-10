package entity.market;

import java.io.Serializable;

import entity.agent.Agent.AgentView;
import entity.agent.OrderRecord;

/**
 * Contains array of Points which can be evaluated independently by
 * the market (i.e., the bid is divisible). Bids are contained in
 * an array sorted by price and quantity, both descending.
 * 
 * @author ewah
 */
public class Order extends fourheap.Order<Price, MarketTime> implements Serializable {

	private static final long serialVersionUID = 4020465194816241014L;
	
	// FIXME should AgentView and OrderRecord be Serializable?
	private final AgentView agent;
	private final OrderRecord orderRecord;

	public Order(AgentView agent, OrderRecord orderRecord, OrderType type, int quantity, Price price, MarketTime time) {
		super(type, price, quantity, time);
		this.agent = agent;
		this.orderRecord = orderRecord;
	}

	public static Order create(AgentView agent, OrderRecord orderRecord, OrderType type, int quantity,
			Price price, MarketTime time) {
		return new Order(agent, orderRecord, type, quantity, price, time);
	}
	
	public static Order create(AgentView agent, OrderRecord orderRecord, MarketTime time) {
		return create(agent, orderRecord, orderRecord.getOrderType(), orderRecord.getQuantity(), orderRecord.getPrice(), time);
	}
	
	public AgentView getAgent() {
		return agent;
	}

	public OrderRecord getOrderRecord() {
		return orderRecord;
	}

	@Override
	public String toString() {
		return agent + " " + type + ' ' + getQuantity() + " @ " + price;
	}
	
}
