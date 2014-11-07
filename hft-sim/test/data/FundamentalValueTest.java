package data;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import systemmanager.Defaults;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import utils.Mock;
import utils.Rand;
import utils.SummStats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import entity.market.Price;
import event.TimeStamp;

public class FundamentalValueTest {
	private static final double eps = 1e-6;
	private static final Rand rand = Rand.create();
	
	@Before
	public void defaultSetup() {
		
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void immutableTest() {
		FundamentalValue fund = Mock.fundamental;
		fund.computeFundamentalTo(1000);
		Iterator<Double> it = fund.iterator();
		it.next();
		it.remove();
	}
	
	@Test
	public void zeroJumpTest() {
		int mean = rand.nextInt(100000);
		FundamentalValue fund = FundamentalValue.create(Stats.create(), Mock.timeline, 0, mean, 0, rand);
		
		Price meanPrice = Price.of(mean);
		for (int time = 0; time < 100; time++)
			assertEquals(meanPrice, fund.getValueAt(TimeStamp.of(time)));
	}
	
	@Test
	public void postRandFundamentalStatTest() {
		Stats stats = Stats.create();
		FundamentalValue fund = FundamentalValue.create(stats, Mock.timeline,
				Defaults.get(FundamentalKappa.class), Defaults.get(FundamentalMean.class), Defaults.get(FundamentalShockVar.class), rand);
		fund.computeFundamentalTo(100);

		assertEquals(ImmutableList.copyOf(fund),
				ImmutableList.copyOf(Iterables.limit(stats.getTimeStats().get(Stats.FUNDAMENTAL), 101)));
		assertEquals(SummStats.on(fund).mean(), stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).mean(), eps);
		assertEquals(SummStats.on(fund).stddev(), stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(), eps);
	}
	
	@Test
	public void postStaticFundamentalStatTest() {
		int mean = rand.nextInt(100000);
		Stats stats = Stats.create();
		FundamentalValue fund = FundamentalValue.create(stats, Mock.timeline, 0, mean, 0, rand);
		
		fund.computeFundamentalTo(100);

		assertEquals(ImmutableList.copyOf(fund),
				ImmutableList.copyOf(Iterables.limit(stats.getTimeStats().get(Stats.FUNDAMENTAL), 101)));
		assertEquals(mean, stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).mean(), eps);
		assertEquals(0, stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(), eps);
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			defaultSetup();
			zeroJumpTest();
			defaultSetup();
			postRandFundamentalStatTest();
		}
	}

}
