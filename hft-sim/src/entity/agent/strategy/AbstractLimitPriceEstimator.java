package entity.agent.strategy;

import entity.agent.Agent;
import entity.market.Price;
import fourheap.Order.OrderType;

public abstract class AbstractLimitPriceEstimator implements LimitPriceEstimator {

	private final Agent agent;
	
	protected AbstractLimitPriceEstimator(Agent agent) {
		this.agent = agent;
	}
	
	protected abstract Price getFundamentalEstimate();
	
	@Override
	public Price getLimitPrice(OrderType buyOrSell, int quantity) {
		return Price.of(getFundamentalEstimate().doubleValue() + agent.getPrivateValue(quantity, buyOrSell).doubleValue() / quantity);
	}

}
