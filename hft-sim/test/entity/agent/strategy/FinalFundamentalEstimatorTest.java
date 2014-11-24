package entity.agent.strategy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import utils.Mock;
import data.FundamentalValue;
import event.TimeStamp;

public class FinalFundamentalEstimatorTest {
	
	// FIXME In general test that this is estimating the liquidation fundamental. (Check that it equals at the end of the simulation?)
	
	@Test
	public void estimatedFundamental() {
		double kappa = 0.05;
		int meanVal = 100000;
		int simLength = 125;
		FundamentalValue fund = Mock.fundamental(200000);
		
		FinalFundamentalEstimator estimator = FinalFundamentalEstimator.create(fund.getView(TimeStamp.ZERO), Mock.timeline, simLength, kappa, meanVal);
		
		// high margin of error here because of rounding issues
		assertEquals(100172.87295506145, estimator.getFundamentalEstimate().doubleValue(), 1);
		
	}
}
