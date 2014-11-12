package entity.agent.strategy;

import data.FundamentalValue.FundamentalValueView;
import entity.agent.Agent;
import entity.market.Price;

public class NaiveLimitPriceEstimator extends AbstractLimitPriceEstimator {

	private final FundamentalValueView fundamental;
	
	private NaiveLimitPriceEstimator(Agent agent, FundamentalValueView fundamental) {
		super(agent);
		this.fundamental = fundamental;
	}
	
	public static NaiveLimitPriceEstimator create(Agent agent, FundamentalValueView fundamental) {
		return new NaiveLimitPriceEstimator(agent, fundamental);
	}

	@Override
	protected Price getFundamentalEstimate() {
		return fundamental.getValue();
	}

}
