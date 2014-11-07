package entity.agent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import utils.Mock;
import utils.Rand;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import data.Props;
import entity.agent.position.PrivateValues;
import event.EventQueue;
import event.TimeStamp;

public class ReentryAgentTest {
	private static final Rand rand = Rand.create();
	private EventQueue timeline;
	
	@Before
	public void setup() {
		timeline = EventQueue.create(Log.nullLogger(), rand);
	}
	
	@Test
	public void reentryTest() {
		PeekingIterator<TimeStamp> reentries = Iterators.peekingIterator(ReentryAgent.exponentials(0.1, rand));
		ReentryAgent agent = reentryAgent(reentries);
		
		// Test reentries
		assertTrue(reentries.hasNext());
		TimeStamp next = reentries.peek();
		assertTrue(next.getInTicks() >= 0);
		
		// Test agent strategy
		timeline.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		assertTrue(reentries.hasNext());
	}
	
	@Test
	public void reentryRateZeroTest() {
		// Test reentries - note should never iterate past INFINITE b/c it 
		// will never execute
		ReentryAgent agent = reentryAgent(ReentryAgent.exponentials(0, rand));
		Iterator<TimeStamp> reentries = agent.reentry;
		assertFalse(reentries.hasNext());

		// Now test for agent, which should not arrive at time 0
		timeline.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		assertFalse(reentries.hasNext());
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i <= 1000; i++) {
			setup();
			reentryTest();
		}
	}
	
	private ReentryAgent reentryAgent(Iterator<TimeStamp> reentry) {
		return new ReentryAgent(0, Mock.stats, timeline, Log.nullLogger(), rand, Mock.sip, Mock.fundamental, PrivateValues.zero(),
				TimeStamp.ZERO, Mock.market(), reentry, Props.fromPairs()) {
			private static final long serialVersionUID = 1L;
		};
	}
}
