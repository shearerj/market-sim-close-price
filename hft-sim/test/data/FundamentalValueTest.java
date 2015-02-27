package data;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import systemmanager.Defaults;
import systemmanager.Keys.FundamentalKappa;
import systemmanager.Keys.FundamentalMean;
import systemmanager.Keys.FundamentalShockVar;
import utils.Maths;
import utils.Mock;
import utils.Rand;
import utils.Repeat;
import utils.RepeatRule;
import utils.Sparse;
import utils.Sparse.SparseElement;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

import entity.market.Price;
import event.TimeStamp;

public class FundamentalValueTest {

	private static final double EPS = 1e-6;
	private static final Rand RAND = Rand.create();

	@Rule
	public RepeatRule repeatRule = new RepeatRule();

	@Test
	@Repeat(100)
	public void zeroJumpTest() {
		int mean = RAND.nextInt(100000);
		FundamentalValue fund =
				FundamentalValue.create(Stats.create(), 
						Mock.timeline, 
						Defaults.get(FundamentalKappa.class), // kappa
						mean,
						Defaults.get(FundamentalShockVar.class), // jump variance
						0.0, // jump probability is zero
						RAND
						);

		// can't compare to mean, because there is an initial jump
		// with variance Defaults.get(FundamentalShockVar.class)
		// before the run starts, away from the mean
		final Price initialPrice = fund.getValueAt(TimeStamp.of(0));
		for (int time = 1; time < 100; time++) {
			assertEquals(
					initialPrice, 
					fund.getValueAt(TimeStamp.of(time))
					);
		}
	}

	@Test
	@Repeat(100)
	public void someNotAllJumpTest() {
		int mean = RAND.nextInt(100000);
		FundamentalValue fund =
				FundamentalValue.create(Stats.create(), 
						Mock.timeline, 
						Defaults.get(FundamentalKappa.class), // kappa
						mean,
						Defaults.get(FundamentalShockVar.class), // jump variance
						0.5, // jump probability is 0.5
						RAND
						);

		// some, but not all, of 100 time steps should have a jump.
		// the probability of failure if implemented correctly is
		// 1/2^100 + 1/2^100 = 1/2^99, or 1 in about 10^30.
		Price previousPrice = fund.getValueAt(TimeStamp.of(0));
		boolean hasJump = false;
		boolean hasNoJump = false;
		for (int time = 1; time < 100; time++) {
			Price currentPrice = fund.getValueAt(TimeStamp.of(time));
			if (previousPrice.equals(currentPrice)) {
				hasNoJump = true;
			} else {
				hasJump = true;
			}

			previousPrice = currentPrice;
		}

		assertEquals(hasNoJump, true);
		assertEquals(hasJump, true);
	}

	/*
   @Test
   public void someNotAllJumpTimeSeriesTest() {
       int mean = RAND.nextInt(100000);
       FundamentalValue fund =
           FundamentalValue.create(Stats.create(), 
           Mock.timeline, 
           Defaults.get(FundamentalKappa.class), // kappa
           mean,
           Defaults.get(FundamentalShockVar.class), // jump variance
           0.5, // jump probability is 0.5
           RAND
       );

       final List<Integer> priceSeries = new ArrayList<Integer>();
       for (int time = 0; time < 100; time++) {
           priceSeries.add(fund.getValueAt(TimeStamp.of(time)).intValue());
       }

       for (Integer price: priceSeries) {
           System.out.print(price + ",");
       }
   }
	 */

	@Test
	@Repeat(100)
	public void zeroProbJumpTest() {
		int mean = RAND.nextInt(100000);
		FundamentalValue fund =
				FundamentalValue.create(
						Stats.create(), 
						Mock.timeline, 
						0, // kappa (multiplied by mean). no mean reversion at 0
						mean, 
						0, // jump variance
						1.0, // jump probability
						RAND
						);

		Price meanPrice = Price.of(mean);

		for (int time = 0; time < 100; time++) {
			assertEquals(meanPrice, fund.getValueAt(TimeStamp.of(time)));
		}
	}

	@Test
	@Repeat(100)
	public void postSimpleRandFundamentalStatTest() {
		Stats stats = Stats.create();
		FundamentalValue fund = FundamentalValue.create(stats, Mock.timeline,
				Defaults.get(FundamentalKappa.class),
				Defaults.get(FundamentalMean.class),
				Defaults.get(FundamentalShockVar.class), 1.0, RAND);
		ImmutableList.Builder<Double> vals = ImmutableList.builder();
		TimeSeries ts = TimeSeries.create();

		for (int time = 0; time < 100; ++time) {
			double price = fund.getValueAt(TimeStamp.of(time)).doubleValue();
			vals.add(price);
			ts.add(TimeStamp.of(time), price);
		}
		List<Double> v = vals.build();

		assertEquals(
				ImmutableList.copyOf(ts),
				ImmutableList.copyOf(stats.getTimeStats().get(Stats.FUNDAMENTAL))
				);
		assertEquals(
				DoubleMath.mean(v),
				stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).mean(),
				EPS
				);
		assertEquals(
				Maths.stddev(v),
				stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(),
				EPS
				);
	}

	@Test
	@Repeat(100)
	public void postRandFundamentalStatTest() {
		Stats stats = Stats.create();
		FundamentalValue fund = FundamentalValue.create(stats, Mock.timeline,
				Defaults.get(FundamentalKappa.class),
				Defaults.get(FundamentalMean.class),
				Defaults.get(FundamentalShockVar.class), 1.0, RAND);
		TimeSeries ts = TimeSeries.create();
		long time = 0;
		ts.add(TimeStamp.ZERO, fund.getValueAt(TimeStamp.ZERO).doubleValue());

		for (int i = 0; i < 5; ++i) {
			time += RAND.nextInt(10) + 1;
			double price = fund.getValueAt(TimeStamp.of(time)).doubleValue();
			ts.add(TimeStamp.of(time), price);
		}

		assertEquals(
				ImmutableList.copyOf(ts),
				ImmutableList.copyOf(stats.getTimeStats().get(Stats.FUNDAMENTAL))
				);
		assertEquals(
				Sparse.stddev(ts, 1, time + 1),
				stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(),
				EPS
				);
	}

	@Test
	public void postStaticFundamentalStatTest() {
		int mean = RAND.nextInt(100000);
		Stats stats = Stats.create();
		FundamentalValue fund = FundamentalValue.create(stats, Mock.timeline, 0, mean, 0, 1.0, RAND);
		fund.getValueAt(TimeStamp.of(100));

		assertEquals(
				ImmutableList.of(SparseElement.create(0, (double) mean)),
				ImmutableList.copyOf(stats.getTimeStats().get(Stats.FUNDAMENTAL))
				);
		assertEquals(mean, stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).mean(), EPS);
		assertEquals(0, stats.getSummaryStats().get(Stats.CONTROL_FUNDAMENTAL).stddev(), EPS);
	}

}
