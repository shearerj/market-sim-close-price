package data;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import logger.Log;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.MockSim;
import utils.SummStats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import entity.market.Price;
import event.TimeStamp;

public class FundamentalValueTest {
	private static final double eps = 1e-6;
	private static final Random rand = new Random();
	private MockSim sim;
	
	@Before
	public void defaultSetup() throws IOException {
		setup(Props.fromPairs());
	}
	
	public void setup(Props parameters) throws IOException {
		sim = MockSim.create(getClass(), Log.Level.NO_LOGGING, parameters);
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void immutableTest() {
		sim.getFundamental().computeFundamentalTo(1000);
		Iterator<Double> it = sim.getFundamental().iterator();
		it.next();
		it.remove();
	}
	
	@Test
	public void zeroJumpTest() throws IOException {
		int mean = rand.nextInt(100000);
		setup(Props.fromPairs(FundamentalMean.class, mean, FundamentalShockVar.class, 0d));
		
		Price meanPrice = Price.of(mean);
		for (int time = 0; time < 100; time++)
			assertEquals(meanPrice, sim.getFundamental().getValueAt(TimeStamp.of(time)));
	}
	
	@Test
	public void postRandFundamentalStatTest() {
		FundamentalValue fund = sim.getFundamental();
		fund.computeFundamentalTo(100);

		assertEquals(ImmutableList.copyOf(fund),
				ImmutableList.copyOf(Iterables.limit(sim.getStats().getTimeStats().get(Stats.FUNDAMENTAL), 101)));
		assertEquals(SummStats.on(fund).mean(), sim.getStats().getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).mean(), eps);
		assertEquals(SummStats.on(fund).stddev(), sim.getStats().getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(), eps);
	}
	
	@Test
	public void postStaticFundamentalStatTest() throws IOException {
		int mean = rand.nextInt(100000);
		setup(Props.fromPairs(FundamentalMean.class, mean, FundamentalShockVar.class, 0d));
		
		FundamentalValue fund = sim.getFundamental();
		fund.computeFundamentalTo(100);

		assertEquals(ImmutableList.copyOf(fund),
				ImmutableList.copyOf(Iterables.limit(sim.getStats().getTimeStats().get(Stats.FUNDAMENTAL), 101)));
		assertEquals(mean, sim.getStats().getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).mean(), eps);
		assertEquals(0, sim.getStats().getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(), eps);
	}
	
	// FIXME Assert that fundamental is never referenced past final simulation time...
	
	@Test
	public void extraTest() throws IOException {
		for (int i = 0; i < 100; i++) {
			defaultSetup();
			zeroJumpTest();
			defaultSetup();
			postRandFundamentalStatTest();
		}
	}

}
