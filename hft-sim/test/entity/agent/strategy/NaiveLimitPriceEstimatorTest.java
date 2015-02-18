package entity.agent.strategy;

import static fourheap.Order.OrderType.BUY;
import static fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import logger.Log;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import utils.Mock;
import utils.Rand;
import data.FundamentalValue;
import data.Props;
import entity.agent.Agent;
import entity.agent.position.ListPrivateValue;
import entity.agent.position.PrivateValue;
import entity.agent.position.PrivateValues;
import entity.market.Price;
import event.EventQueue;
import event.TimeStamp;

public class NaiveLimitPriceEstimatorTest {
	
	private static final Rand rand = Rand.create();
	
	private EventQueue timeline;
	private FundamentalValue fundamental;
	
	@Before
	public void setup() {
		timeline = EventQueue.create(Log.nullLogger(), rand);
		fundamental = FundamentalValue.create(Mock.stats, timeline, 0.005, 100000, 1e8, 1.0, rand);
	}

	// FIXME Test that this handles latency appropriately
	
	/** Verify limit price (where PV = 0) */
	@Test
	public void getLimitPriceBasic() {
		Agent agent = agent(PrivateValues.zero());
		LimitPriceEstimator estimator = NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO));
		
		for (int t = 0; t < 60000; ++t) {
			TimeStamp time = TimeStamp.of(t);
			timeline.executeUntil(time);
			assertEquals(fundamental.getValueAt(time), estimator.getLimitPrice(BUY, 1));
			assertEquals(fundamental.getValueAt(time), estimator.getLimitPrice(SELL, 1));
			assertEquals(fundamental.getValueAt(time), estimator.getLimitPrice(BUY, 2));
			assertEquals(fundamental.getValueAt(time), estimator.getLimitPrice(SELL, 2));
		}
	}
	
	@Test
	public void getValuationConstPV() {
		Agent agent = agent(ListPrivateValue.create(Price.of(100), Price.of(10)));
		LimitPriceEstimator estimator = NaiveLimitPriceEstimator.create(agent, fundamental.getView(TimeStamp.ZERO));

		// Verify valuation (current position of 0)
		Price fundPrice = fundamental.getValueAt(TimeStamp.ZERO);
		assertEquals(fundPrice.intValue() + 10, estimator.getLimitPrice(BUY, 1).intValue());
		assertEquals(fundPrice.intValue() + 100, estimator.getLimitPrice(SELL, 1).intValue());
	}
	
	private Agent agent(PrivateValue privateValue) {
		return new Agent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, fundamental, privateValue,
				ImmutableList.<TimeStamp> of().iterator(), Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
		};
	}

}
