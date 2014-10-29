package entity.agent;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import logger.Log;

import org.junit.Test;

import utils.Mock;
import data.Props;
import data.Stats;
import entity.market.Market;
import event.EventQueue;
import event.TimeStamp;

public class NoOpAgentTest {
	private static Random rand = new Random();

	// FIXME Check that it still liquidates and posts appropriate stats
	
	@Test
	public void strategyTest() {
		EventQueue timeline = EventQueue.create(Log.nullLogger(), rand);
		Stats stats = Stats.create();
		
		Market market = Mock.market(timeline);
		
		ZIRAgent zir = ZIRAgent.create(0, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, market, Props.fromPairs());
		NoOpAgent noop = NoOpAgent.create(1, stats, Mock.timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, Props.fromPairs());
		
		zir.agentStrategy();
		noop.agentStrategy();
		// NoOp agent doesn't know about the market, and should never have agent strategy called again...
		
		for (int i = 0; i < 6000; ++i) {
			timeline.executeUntil(TimeStamp.of(i));
			assertEquals(0, noop.getPayoff(), 0);		// No profit
			assertEquals(0, noop.getPosition());		// No transactions
		}
		
	}
	
}
