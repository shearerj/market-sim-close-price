package data;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import utils.Rand;

import com.google.common.collect.Ordering;
import com.google.common.collect.Range;

import entity.View;
import entity.market.Price;
import event.TimeStamp;
import event.Timeline;

/**
 * Class to store and compute a stochastic process used as a base to determine
 * the private valuations of background agents.
 * 
 * @author ewah
 */
public class FundamentalValue implements Serializable {

	private static final Ordering<TimeStamp> tord = Ordering.natural();
	
	private final Timeline timeline;
	private final Stats stats;
	private final Rand rand;
	
	private final double kappac; // kappa compliment (1 - kappa)
	private final double mean;
	private final double shockVar;
	
	private TimeStamp lastTime;
	private Price lastPrice;
	

	/**
	 * @param kappa rate which the process reverts to the mean value
	 * @param mean mean
	 * @param var gaussian variance
	 */
	private FundamentalValue(Stats stats, Timeline timeline, double kappa, double mean, double var, Rand rand) {
		checkArgument(Range.closed(0d, 1d).contains(kappa), "Kappa (%.4f) not in [0, 1]", kappa);
		this.stats = stats;
		this.timeline = timeline;
		this.rand = rand;
		this.kappac = 1 - kappa;
		this.mean = mean;
		this.shockVar = var;

		// stochastic initial conditions for random process
		lastPrice = Price.of(rand.nextGaussian(mean, shockVar)).nonnegative();
		lastTime = TimeStamp.ZERO;

		// Post statistics
		stats.postTimed(lastTime, Stats.FUNDAMENTAL, lastPrice.doubleValue());
		stats.post(Stats.CONTROL_FUNDAMENTAL, lastPrice.doubleValue());
	}
	
	/** Creates a mean reverting Gaussian Process that supports random access to small (int) TimeStamps */
	public static FundamentalValue create(Stats stats, Timeline timeline, double kap, double meanVal, double var, Rand rand) {
		return new FundamentalValue(stats, timeline, kap, meanVal, var, rand);
	}

	/** Returns the global fundamental value at time ts. */
	public Price getValueAt(TimeStamp time) {
		checkArgument(time.afterOrOn(lastTime), "Must query sequential times");
		if (time.after(lastTime)) {
			long deltat = time.getInTicks() - lastTime.getInTicks();
			double kappae = Math.pow(kappac, deltat); // Effective kappa
			double varScale = kappac == 1 ? deltat : (1 - kappae * kappae) / (1 - kappac * kappac);
			
			stats.post(Stats.CONTROL_FUNDAMENTAL, lastPrice.doubleValue(), deltat - 1); // Post "assuming" fundamental was previous value
			
			lastPrice = Price.of(rand.nextGaussian((1 - kappae) * mean + kappae * lastPrice.doubleValue(), shockVar * varScale))
					.nonnegative();
			lastTime = time;
			
			stats.postTimed(lastTime, Stats.FUNDAMENTAL, lastPrice.doubleValue());
			stats.post(Stats.CONTROL_FUNDAMENTAL, lastPrice.doubleValue());
		}
		return lastPrice;
	}

	// TODO memeoize? Lots of views with the same latency. Extra objects
	/** Get a possibly delayed view of the fundamental process */
	public FundamentalValueView getView(final TimeStamp latency) {
		return new FundamentalValueView(latency);
	}
	
	/** A view of this process from some entity with limited information */
	public class FundamentalValueView implements View {
		private TimeStamp latency;
		
		protected FundamentalValueView(TimeStamp latency) {
			this.latency = latency;
		}
		
		/** Gets most recent known value of the fundamental for an entity in the simulation */
		public Price getValue() {
			return FundamentalValue.this.getValueAt(tord.max(timeline.getCurrentTime().minus(latency), TimeStamp.ZERO));
		}
		
		@Override
		public TimeStamp getLatency() {
			return latency;
		}
	}

	private static final long serialVersionUID = 6764216196138108452L;
}
