package entity.agent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static utils.Tests.j;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Consts.MarketType;
import systemmanager.Keys;
import systemmanager.MockSim;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import data.Props;
import entity.market.Market;
import event.TimeStamp;

public class ReentryAgentTest {

	private static final Random rand = new Random();
	private MockSim sim;
	private Market market;
	
	@Before
	public void setup() throws IOException {
		sim = MockSim.create(getClass(),
				Log.Level.NO_LOGGING, Keys.FUNDAMENTAL_MEAN,
				100000, Keys.FUNDAMENTAL_SHOCK_VAR,
				0, MarketType.CDA, j.join(Keys.NUM_MARKETS, 1));
		market = Iterables.getOnlyElement(sim.getMarkets());
	}
	
	@Test
	public void reentryTest() {
		PeekingIterator<TimeStamp> reentries = Iterators.peekingIterator(AgentFactory.exponentials(0.1, rand));
		ReentryAgent agent = reentryAgent(reentries);
		
		// Test reentries
		assertTrue(reentries.hasNext());
		TimeStamp next = reentries.peek();
		assertTrue(next.getInTicks() >= 0);
		
		// Test agent strategy
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		assertTrue(reentries.hasNext());
	}
	
	@Test
	public void reentryRateZeroTest() {
		// Test reentries - note should never iterate past INFINITE b/c it 
		// will never execute
		ReentryAgent agent = reentryAgent(AgentFactory.exponentials(0, rand));
		Iterator<TimeStamp> reentries = agent.reentry;
		assertFalse(reentries.hasNext());

		// Now test for agent, which should not arrive at time 0
		sim.executeUntil(TimeStamp.of(100));
		agent.agentStrategy();
		assertFalse(reentries.hasNext());
	}
	
	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i <= 1000; i++) {
			setup();
			reentryTest();
		}
	}
	
	private ReentryAgent reentryAgent(Iterator<TimeStamp> reentry, Object... properties) {
		return new ReentryAgent(sim, TimeStamp.ZERO, market, rand, reentry, Props.fromPairs(properties)) {
			private static final long serialVersionUID = 1L;
		};
	}
}
