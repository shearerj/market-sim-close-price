package entity.agent.strategy;

import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.SimLength;
import data.FundamentalValue.FundamentalValueView;
import data.Props;
import entity.agent.Agent;
import entity.market.Price;
import event.Timeline;

public class OptimalLimitPriceEstimator extends AbstractLimitPriceEstimator {

	private final FundamentalValueView fundamental;
	private final Timeline timeline;
	private final int simulationLength;
	private final double kappa;
	private final int fundamentalMean;
	
	private OptimalLimitPriceEstimator(Agent agent, FundamentalValueView fundamental, Timeline timeline,
			int simulationLength, double kappa, int fundamentalMean) {
		super(agent);
		this.fundamental = fundamental;
		this.timeline = timeline;
		this.simulationLength = simulationLength;
		this.kappa = kappa;
		this.fundamentalMean = fundamentalMean;
	}
	
	public static OptimalLimitPriceEstimator create(Agent agent, FundamentalValueView fundamental, Timeline timeline,
			int simulationLength, double kappa, int fundamentalMean) {
		return new OptimalLimitPriceEstimator(agent, fundamental, timeline, simulationLength, kappa, fundamentalMean);
	}
	
	public static OptimalLimitPriceEstimator create(Agent agent, FundamentalValueView fundamental, Timeline timeline,
			Props props) {
		return create(agent, fundamental, timeline, props.get(SimLength.class), props.get(FundamentalKappa.class), props.get(FundamentalMean.class));
	}

	@Override
	protected Price getFundamentalEstimate() {
		double stepsLeft = simulationLength - timeline.getCurrentTime().getInTicks() + fundamental.getLatency().getInTicks();
		double kappaCompToPower = Math.pow(1 - kappa, stepsLeft);
		return Price.of(fundamental.getValue().doubleValue() * kappaCompToPower 
			+ fundamentalMean * (1 - kappaCompToPower));
	}

}
