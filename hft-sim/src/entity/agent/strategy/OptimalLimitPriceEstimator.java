package entity.agent.strategy;

import data.FundamentalValue.FundamentalValueView;
import data.Props;
import entity.agent.Agent;
import entity.market.Price;
import event.Timeline;

// XXX Could change this to a class than can get limit price from various fundamental estimates
public class OptimalLimitPriceEstimator extends AbstractLimitPriceEstimator {

	FinalFundamentalEstimator fundamental;
	
	private OptimalLimitPriceEstimator(Agent agent, FinalFundamentalEstimator fundamental) {
		super(agent);
		this.fundamental = fundamental;
	}
	
	public static OptimalLimitPriceEstimator create(Agent agent, FundamentalValueView fundamental, Timeline timeline,
			int simulationLength, double kappa, int fundamentalMean) {
		return new OptimalLimitPriceEstimator(agent, FinalFundamentalEstimator.create(fundamental, timeline, simulationLength, kappa, fundamentalMean));
	}
	
	public static OptimalLimitPriceEstimator create(Agent agent, FundamentalValueView fundamental, Timeline timeline,
			Props props) {
		return new OptimalLimitPriceEstimator(agent, FinalFundamentalEstimator.create(fundamental, timeline, props));
	}

	@Override
	protected Price getFundamentalEstimate() {
		return fundamental.getFundamentalEstimate();
	}

}
