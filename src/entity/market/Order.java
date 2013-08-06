package entity.market;

import entity.Agent;
import event.TimeStamp;

/**
 * Contains array of Points which can be evaluated independently by
 * the market (i.e., the bid is divisible). Bids are contained in
 * an array sorted by price and quantity, both descending.
 * 
 * @author ewah
 */
public class Order extends Bid {

	protected final Agent agent;
	protected Market market;
	protected final fourheap.Order<Price, TimeStamp> order;
	
	public Order(Agent agent, Market market, fourheap.Order<Price, TimeStamp> order) {
		super(agent, market, order.getSubmitTime()); // FIXME Remove and subclassing
		this.agent = agent;
		this.market = market;
		this.order = order;
	}
	
	public Agent getAgent() {
		return agent;
	}

	public Market getMarket() {
		return market;
	}
	
	public TimeStamp getSubmitTime() {
		return order.getSubmitTime();
	}
	
	public Price getPrice() {
		return order.getPrice();
	}
	
	public int getQuantity() {
		return order.getQuantity();
	}
	
	// FIXME Equals and Hashcode
	
	@Override
	public String toString() {
		return "<" + agent + " : " + market + " : " + order + ">";
	}
	
}
