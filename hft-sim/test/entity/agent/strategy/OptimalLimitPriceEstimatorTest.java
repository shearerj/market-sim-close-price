package entity.agent.strategy;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import logger.Log;

import org.junit.Test;

import utils.Mock;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import entity.agent.Agent;
import entity.agent.position.PrivateValues;
import event.EventQueue;
import event.TimeStamp;

public class OptimalLimitPriceEstimatorTest {

	private static final Rand rand = Rand.create();
	private static final double eps = 1e-6;
	
	// FIXME Test that this handles latency appropriately
	
	@Test
	public void getEstimatedValuationBasic() {
		int simulationLength = 60000;
		int mean = 100000;
		double kappa = 0.005;
		
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		FundamentalValue fundamental = FundamentalValue.create(Mock.stats, timeline, kappa, mean, 1e8, rand);
		Agent agent = new Agent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, fundamental, PrivateValues.zero(),
				TimeStamp.ZERO, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
			@Override protected void agentStrategy() { }
		};
		
		LimitPriceEstimator estimator = OptimalLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO), timeline,
				simulationLength, kappa, mean);

		double kappaToPower = Math.pow(kappa, simulationLength);
		double rHat = fundamental.getValueAt(TimeStamp.ZERO).doubleValue() * kappaToPower + mean * (1 - kappaToPower);
		
		// Verify valuation (where PV = 0)
		assertEquals(rHat, estimator.getLimitPrice(BUY, 1).doubleValue(), eps);
		assertEquals(rHat, estimator.getLimitPrice(SELL, 1).doubleValue(), eps);

		for (int t = 0; t < simulationLength; t++) {
			TimeStamp time = TimeStamp.of(t);
			timeline.executeUntil(time);
			double value = fundamental.getValueAt(time).doubleValue();
			
			rHat = estimator.getLimitPrice(SELL, 1).doubleValue();
			assertTrue(Math.abs(rHat - mean) <= Math.abs(value - mean));
		}
	}

}
