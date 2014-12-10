package data;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import systemmanager.Defaults;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import utils.Maths;
import utils.Mock;
import utils.Rand;
import utils.Sparse;
import utils.Sparse.SparseElement;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

import entity.market.Price;
import event.TimeStamp;

public class FundamentalValueTest {
	private static final double eps = 1e-6;
	private static final Rand rand = Rand.create();
	
	@Test
	public void zeroJumpTest() {
		int mean = rand.nextInt(100000);
		FundamentalValue fund = FundamentalValue.create(Stats.create(), Mock.timeline, 0, mean, 0, rand);
		
		Price meanPrice = Price.of(mean);
		for (int time = 0; time < 100; time++)
			assertEquals(meanPrice, fund.getValueAt(TimeStamp.of(time)));
	}
	
	@Test
	public void postSimpleRandFundamentalStatTest() {
		Stats stats = Stats.create();
		FundamentalValue fund = FundamentalValue.create(stats, Mock.timeline,
				Defaults.get(FundamentalKappa.class), Defaults.get(FundamentalMean.class), Defaults.get(FundamentalShockVar.class), rand);
		ImmutableList.Builder<Double> vals = ImmutableList.builder();
		TimeSeries ts = TimeSeries.create();
		for (int time = 0; time < 100; ++time) {
			double price = fund.getValueAt(TimeStamp.of(time)).doubleValue();
			vals.add(price);
			ts.add(TimeStamp.of(time), price);
		}
		List<Double> v = vals.build();
		
		assertEquals(ImmutableList.copyOf(ts), ImmutableList.copyOf(stats.getTimeStats().get(Stats.FUNDAMENTAL)));
		assertEquals(DoubleMath.mean(v), stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).mean(), eps);
		assertEquals(Maths.stddev(v), stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(), eps);
	}
	
	@Test
	public void postRandFundamentalStatTest() {
		Stats stats = Stats.create();
		FundamentalValue fund = FundamentalValue.create(stats, Mock.timeline,
				Defaults.get(FundamentalKappa.class), Defaults.get(FundamentalMean.class), Defaults.get(FundamentalShockVar.class), rand);
		TimeSeries ts = TimeSeries.create();
		long time = 0;
		ts.add(TimeStamp.ZERO, fund.getValueAt(TimeStamp.ZERO).doubleValue());
		for (int i = 0; i < 5; ++i) {
			time += rand.nextInt(10) + 1;
			double price = fund.getValueAt(TimeStamp.of(time)).doubleValue();
			ts.add(TimeStamp.of(time), price);
		}
		
		assertEquals(ImmutableList.copyOf(ts), ImmutableList.copyOf(stats.getTimeStats().get(Stats.FUNDAMENTAL)));
		assertEquals(Sparse.stddev(ts, 1, time + 1), stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(), eps);
	}
	
	@Test
	public void postStaticFundamentalStatTest() {
		int mean = rand.nextInt(100000);
		Stats stats = Stats.create();
		FundamentalValue fund = FundamentalValue.create(stats, Mock.timeline, 0, mean, 0, rand);
		fund.getValueAt(TimeStamp.of(100));

		assertEquals(
				ImmutableList.of(SparseElement.create(0, (double) mean)),
				ImmutableList.copyOf(stats.getTimeStats().get(Stats.FUNDAMENTAL)));
		assertEquals(mean, stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).mean(), eps);
		assertEquals(0, stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(), eps);
	}
	
	@Test
	public void extraTest() {
		for (int i = 0; i < 100; i++) {
			zeroJumpTest();
			postSimpleRandFundamentalStatTest();
			postRandFundamentalStatTest();
		}
	}

}
