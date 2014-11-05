package entity.agent;

import static org.junit.Assert.assertEquals;
import logger.Log;

import org.junit.Test;

import utils.Mock;
import utils.Rand;
import data.Props;
import data.Stats;
import entity.market.Market;
import event.EventQueue;
import event.TimeStamp;

public class NoOpAgentTest {
	private static Rand rand = Rand.create();

	// FIXME Check that it still liquidates and posts appropriate stats
	
	@Test
	public void strategyTest() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		Stats stats = Stats.create();
		
		Market market = Mock.market(timeline);
		
		ZIRAgent.create(0, stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs());
		NoOpAgent noop = NoOpAgent.create(1, stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, Props.fromPairs());
		// NoOp agent doesn't know about the market, and should never have agent strategy called again...
		
		for (int i = 0; i < 6000; ++i) {
			timeline.executeUntil(TimeStamp.of(i));
			assertEquals(0, noop.getPayoff(), 0);		// No profit
			assertEquals(0, noop.getPosition());		// No transactions
		}
		
	}

	/*
	 * FIXME Somehow the ZIR agent is withdrawing an order before it gets to the
	 * market, and as a result the market tries to process an order with zero
	 * quantity. This is partially a problem with the way withdrawls happen.
	 * Potentially, quantity should only get removed from orders by the agent
	 * view. Otherwise withdrawing an order does it effectively instantaniously
	 * if it hasn't reached the market yet.
	 */
	@Test
	public void randomTest() {
		for (int i = 0; i < 100; ++i) {
			strategyTest();
		}
	}
	
}
