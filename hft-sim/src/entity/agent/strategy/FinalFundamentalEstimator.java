package entity.agent.strategy;

import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.SimLength;
import data.FundamentalValue.FundamentalValueView;
import data.Props;
import entity.market.Price;
import event.Timeline;

public class FinalFundamentalEstimator {

	private final FundamentalValueView fundamental;
	private final Timeline timeline;
	private final int simulationLength;
	private final double kappa;
	private final int fundamentalMean;
	
	private FinalFundamentalEstimator(FundamentalValueView fundamental, Timeline timeline,
			int simulationLength, double kappa, int fundamentalMean) {
		this.fundamental = fundamental;
		this.timeline = timeline;
		this.simulationLength = simulationLength;
		this.kappa = kappa;
		this.fundamentalMean = fundamentalMean;
	}
	
	public static FinalFundamentalEstimator create(FundamentalValueView fundamental, Timeline timeline,
			int simulationLength, double kappa, int fundamentalMean) {
		return new FinalFundamentalEstimator(fundamental, timeline, simulationLength, kappa, fundamentalMean);
	}
	
	public static FinalFundamentalEstimator create(FundamentalValueView fundamental, Timeline timeline,
			Props props) {
		return create(fundamental, timeline, props.get(SimLength.class), props.get(FundamentalKappa.class), props.get(FundamentalMean.class));
	}

	public Price getFundamentalEstimate() {
		double stepsLeft = simulationLength - timeline.getCurrentTime().getInTicks() + fundamental.getLatency().getInTicks();
		double kappaCompToPower = Math.pow(1 - kappa, stepsLeft);
		return Price.of(fundamental.getValue().doubleValue() * kappaCompToPower 
			+ fundamentalMean * (1 - kappaCompToPower));
	}
	
}
