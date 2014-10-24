package entity.agent;

import static logger.Log.Level.DEBUG;
import static utils.Tests.checkSingleOrderRange;

import java.io.IOException;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.BidRangeMax;
import systemmanager.Keys.BidRangeMin;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.MockSim;

import com.google.common.collect.Iterables;

import data.Props;
import entity.market.Market;
import entity.market.Price;
import event.TimeStamp;

/**
 * ZIAgent Test
 * 
 * Testing of ZI strategy 
 * Uses loops and randomized parameters to account for randomized bid ranges and private values
 * 
 * NOTE: Just because a test fails *does not* mean that the ZIAgent is not performing to spec,
 * 		 because of the normal distribution of private values. There is a 0.3% chance of 
 * 		 a private value falling outside the +/- 3 stdev range. This corresponds to
 * 		 1 in 370 tests.
 * 
 * @author yngchen
 * 
 */
public class ZIAgentTest {	

	private static final Random rand = new Random();
	private static final Props defaults = Props.fromPairs(
			PrivateValueVar.class, 1e8,
			BidRangeMin.class, 0,
			BidRangeMax.class, 1000);

	private MockSim sim;
	private Market market;

	@Before
	public void defaultSetup() throws IOException {
		setup(10000);
	}
	
	public void setup(double shockVar) throws IOException {
		sim = MockSim.createCDA(getClass(), Log.Level.NO_LOGGING, 1);
		market = Iterables.getOnlyElement(sim.getMarkets());
	}
	// FIXME Test that agent strategy does what it's supposed to do
	
	@SuppressWarnings("deprecation")
	@Test
	public void initialPriceZITest() {
		sim.log(DEBUG, "Testing ZI submitted bid range is correct");
		ZIAgent agent = ziAgent();
		agent.agentStrategy();
		/*
		 * Fundamental = 100000 ($100.00), Stdev = sqrt(100000000) = 10000
		 * ($10.000) Bid Range = 0, 1000 ($0.00, $1.00) 99.7% of bids should
		 * fall between 100000 +/- (3*10000 + 1000) = 70000, 13000
		 */
		checkSingleOrderRange(agent.activeOrders, Price.of(70000), Price.of(130000), 1);
	}
	
	@SuppressWarnings("deprecation")
	private ZIAgent ziAgent(){
		return ZIAgent.create(sim, TimeStamp.ZERO, market, rand, defaults);
	}
	
}
