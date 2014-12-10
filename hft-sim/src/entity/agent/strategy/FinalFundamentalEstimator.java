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
	private final long simulationLength;
	private final double kappa;
	private final double fundamentalMean;
	
	private FinalFundamentalEstimator(FundamentalValueView fundamental, Timeline timeline,
			long simulationLength, double kappa, double fundamentalMean) {
		this.fundamental = fundamental;
		this.timeline = timeline;
		this.simulationLength = simulationLength;
		this.kappa = kappa;
		this.fundamentalMean = fundamentalMean;
	}
	
	public static FinalFundamentalEstimator create(FundamentalValueView fundamental, Timeline timeline,
			long simulationLength, double kappa, double fundamentalMean) {
		return new FinalFundamentalEstimator(fundamental, timeline, simulationLength, kappa, fundamentalMean);
	}
	
	public static FinalFundamentalEstimator create(FundamentalValueView fundamental, Timeline timeline, Props props) {
		return create(fundamental, timeline, props.get(SimLength.class), props.get(FundamentalKappa.class), props.get(FundamentalMean.class));
	}

	// XXX -1 because the final time is "simulationLength - 1"
	public Price getFundamentalEstimate() {
		double stepsLeft = simulationLength - 1 - timeline.getCurrentTime().getInTicks() + fundamental.getLatency().getInTicks();
		double kappaCompToPower = Math.pow(1 - kappa, stepsLeft);
		return Price.of(fundamental.getValue().doubleValue() * kappaCompToPower + fundamentalMean * (1 - kappaCompToPower));
	}
	
}
