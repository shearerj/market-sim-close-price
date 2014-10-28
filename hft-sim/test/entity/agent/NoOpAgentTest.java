package entity.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.MockSim;

import com.google.common.collect.Iterables;

import data.Props;
import entity.market.Market;
import event.TimeStamp;

public class NoOpAgentTest {
	private static Random rand = new Random();
	private MockSim sim;
	private Market market;

	@Before
	public void setup() throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 1);
		market = Iterables.getOnlyElement(sim.getMarkets());
	}
	
	@Test
	public void strategyTest() {
		Agent noop = NoOpAgent.create(sim, rand, Props.fromPairs());
		Agent zir  = ZIRAgent.create(sim, TimeStamp.ZERO, market, rand, Props.fromPairs());
		
		zir.agentStrategy();
		noop.agentStrategy();
		// NoOp agent doesn't know about the market, and should never have agent strategy called again...
		
		for (int i = 0; i < 6000; ++i) {
			sim.executeUntil(TimeStamp.of(i));
			assertEquals(0, noop.getPayoff(), 0);		// No profit
			assertTrue(noop.activeOrders.isEmpty());	// No orders
			assertEquals(0, noop.getPosition());		// No transactions
			assertTrue(noop.activeOrders.isEmpty());	// No orders
		}
		
	}
	
}
