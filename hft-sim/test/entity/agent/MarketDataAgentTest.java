package entity.agent;

import static org.junit.Assert.assertTrue;
import static utils.Tests.checkSingleOrder;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.MockSim;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import data.Props;
import entity.agent.MarketDataParser.MarketAction;
import entity.market.Market;
import entity.market.Price;
import event.Activity;
import event.TimeStamp;

// TODO More complicated tests

public class MarketDataAgentTest {
	private static final Random rand = new Random();
	private MockSim sim;
	private Market market;

	@Before
	public void setupTest() throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 1);
		market = Iterables.getOnlyElement(sim.getMarkets());
	}

	@Test
	public void nyseSimpleTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNYSE(new StringReader("A,0,1,P,B,5742346,SRG1,3249052,0,88,O,AARCA,\n"));
		MarketDataAgent agent = marketDataAgent(actions);
		
		sim.executeUntil(TimeStamp.of(87));
		assertTrue(agent.activeOrders.isEmpty());
		
		sim.executeUntil(TimeStamp.of(88));
		checkSingleOrder(agent.activeOrders, Price.of(3249052), 5742346, TimeStamp.of(88), TimeStamp.of(88));
	}
	
	
	
	@Test
	public void nyseDeleteTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNYSE(new StringReader(
				"A,0,1,B,B,981477,SRG2,5516081,0,0,L,AARCA,\n" +
				"D,0,1,2,0,SRG2,B,L,AARCA,B,\n"));
		MarketDataAgent agent = marketDataAgent(actions);
		
		sim.executeUntil(TimeStamp.IMMEDIATE);
		assertTrue(agent.activeOrders.isEmpty());
		
		sim.executeUntil(TimeStamp.ZERO);
		checkSingleOrder(agent.activeOrders, Price.of(5516081), 981477, TimeStamp.ZERO, TimeStamp.ZERO);
		
		sim.executeUntil(TimeStamp.of(1999));
		checkSingleOrder(agent.activeOrders, Price.of(5516081), 981477, TimeStamp.ZERO, TimeStamp.ZERO);

		sim.executeUntil(TimeStamp.of(2000));
		assertTrue(agent.activeOrders.isEmpty());
		assertTrue(agent.refNumbers.isEmpty()); // Implementation dependent, but no way to tell other than checking
	}
	
	@Test
	public void nasdaqAddTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNasdaq(new StringReader(
				"T,0\n" +
				"A,16382751,1,B,3748742,SRG1,5630815\n"));
		MarketDataAgent agent = marketDataAgent(actions);
		
		sim.executeUntil(TimeStamp.of(15));
		assertTrue(agent.activeOrders.isEmpty());

		sim.executeUntil(TimeStamp.of(16));
		checkSingleOrder(agent.activeOrders, Price.of(5630815), 3748742, TimeStamp.of(16), TimeStamp.of(16));
	}
	
	@Test
	public void nasdaqDeleteTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNasdaq(new StringReader(
				"T,0\n" +
				"A,16382751,1,B,3748742,SRG1,5630815\n" +
				"D,20000000,1\n"));
		MarketDataAgent agent = marketDataAgent(actions);
		
		sim.executeUntil(TimeStamp.of(15));
		assertTrue(agent.activeOrders.isEmpty());
		
		sim.executeUntil(TimeStamp.of(16));
		checkSingleOrder(agent.activeOrders, Price.of(5630815), 3748742, TimeStamp.of(16), TimeStamp.of(16));
		
		sim.executeUntil(TimeStamp.of(19));
		checkSingleOrder(agent.activeOrders, Price.of(5630815), 3748742, TimeStamp.of(16), TimeStamp.of(16));

		sim.executeUntil(TimeStamp.of(20));
		assertTrue(agent.activeOrders.isEmpty());
		assertTrue(agent.refNumbers.isEmpty()); // Implementation dependent, but no way to tell other than checking
		
	}
	
	private MarketDataAgent marketDataAgent(Iterator<MarketAction> actions) {
		PeekingIterator<MarketAction> peekable = Iterators.peekingIterator(actions);
		TimeStamp arrivalTime = peekable.hasNext() ? peekable.peek().getScheduledTime() : TimeStamp.ZERO;
		final MarketDataAgent agent = new MarketDataAgent(sim, arrivalTime, market, rand, peekable,
				Props.fromPairs());
		sim.scheduleActivityIn(agent.getArrivalTime(), new Activity() {
			@Override public void execute() { agent.agentStrategy(); }
		});
		return agent;
	}
}

