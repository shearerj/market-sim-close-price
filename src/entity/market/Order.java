package entity.market;

import java.io.Serializable;

import com.google.common.base.Objects;

import entity.agent.Agent;
import event.TimeStamp;

/**
 * Contains array of Points which can be evaluated independently by
 * the market (i.e., the bid is divisible). Bids are contained in
 * an array sorted by price and quantity, both descending.
 * 
 * @author ewah
 */
public class Order implements Serializable {

	private static final long serialVersionUID = 4020465194816241014L;
	
	protected final Agent agent;
	protected Market market;
	protected final fourheap.Order<Price, MarketTime> order;
	
	public Order(Agent agent, Market market, fourheap.Order<Price, MarketTime> order) {
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
		return order.getSubmitTime().getTime();
	}
	
	public Price getPrice() {
		return order.getPrice();
	}
	
	public int getQuantity() {
		return order.getQuantity();
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(agent, market, order);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj.getClass().equals(getClass())))
			return false;
		Order that = (Order) obj;
		return Objects.equal(agent, that.agent)
				&& Objects.equal(market, that.market)
				&& Objects.equal(order, that.order);
	}
	
	@Override
	public String toString() {
		return "<" + agent + " : " + market + " : " + order + ">";
	}
	
}
