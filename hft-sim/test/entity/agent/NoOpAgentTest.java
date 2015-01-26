package entity.agent;

import static org.junit.Assert.assertEquals;
import logger.Log;

import org.junit.Test;

import utils.Mock;
import utils.Rand;
import data.Props;
import data.Stats;
import entity.market.Market;
import entity.market.Price;
import event.EventQueue;
import event.TimeStamp;

public class NoOpAgentTest {
	private static final Rand rand = Rand.create();
	private static final double eps = 1e-6;
	private static final String noopstr = NoOpAgent.class.getSimpleName().toLowerCase();

	/** Check that noop does nothing, and that it posts proper stats when liquidating */
	@Test
	public void strategyTest() {
		Stats stats = Stats.create();
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		Market market = Mock.market(timeline);
		
		ZIRAgent.create(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs());
		Agent noop = NoOpAgent.create(1, stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, Props.fromPairs());
		// NoOp agent doesn't know about the market, and should never have agent strategy called again...
		
		for (int i = 0; i < 6000; ++i) {
			timeline.executeUntil(TimeStamp.of(i));
			assertEquals(0, noop.getPayoff(), 0);		// No profit
			assertEquals(0, noop.getPosition());		// No transactions
		}
		
		noop.liquidateAtPrice(Price.of(123456));
		
		// Verify end of simulation stats
		assertEquals(0, stats.getSummaryStats().get(Stats.TOTAL_PROFIT).sum(), eps);
		assertEquals(0, stats.getSummaryStats().get(Stats.NUM_TRANS_TOTAL).sum(), eps);
		assertEquals(0, stats.getSummaryStats().get(Stats.NUM_TRANS + noopstr).sum(), eps);
		
		assertEquals(0, stats.getSummaryStats().get(Stats.SURPLUS + "0").sum(), eps);
		assertEquals(0, stats.getSummaryStats().get(Stats.SURPLUS + "0_" + noopstr).sum(), eps);
	}
	
}
