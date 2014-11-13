package entity.agent.strategy;

import static org.junit.Assert.assertEquals;
import logger.Log;

import org.junit.Test;

import utils.Mock;
import utils.Rand;
import data.FundamentalValue;
import event.EventQueue;
import event.TimeStamp;
import event.Timeline;

public class FinalFundamentalEstimatorTest {

	private static final Rand rand = Rand.create();
	
	// FIXME In general test that this is estimating the liquidation fundamental. (Check that it equals at the end of the simulation?
	
	@Test
	public void estimatedFundamental() {
		// FIXME (for Elaine) I'm not sure what this was supposed to be checking
		double kappa = 0.05;
		int meanVal = 100000;
		double var = 1E6;
		int simLength = 125;
		Timeline timeline = EventQueue.create(Log.nullLogger(), rand);
		FundamentalValue fund = FundamentalValue.create(Mock.stats, timeline, kappa, meanVal, var, Rand.create());
		
		FinalFundamentalEstimator estimator = FinalFundamentalEstimator.create(fund.getView(TimeStamp.ZERO), Mock.timeline, simLength, kappa, meanVal);
		
		// high margin of error here because of rounding issues
		assertEquals(0.005920529*fund.getValueAt(TimeStamp.of(124)).doubleValue() + 99407.9,
				estimator.getFundamentalEstimate().doubleValue(), 0.75);
		
	}
}
