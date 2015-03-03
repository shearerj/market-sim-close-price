package data;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import org.apache.commons.math3.distribution.BinomialDistribution;

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
 * if F(t) is the fundamental at time t
 * s^2 is the shock variance
 * m is the fundamental mean and
 * kc is 1 - kappa then
 *
 * F(t+1) ~ N(m*(1-kc) + F(t)*kc, s^2)
 * 
 * In other words, F(t+1) = kappa m + (1 - kappa) F(t) + N(0, s^2).
 *
 * which implies that
 *
 * F(t+d) ~ N(m*(1-kc^d) + F(t)*kc^d,
 * s^2 * (1 - kc^(2d)) / (1 - kc^2)) if kc in [0, 1)
 *
 * or
 *
 * F(t+d) ~ N(F(t), d * s^2) if kc = 1.
 * 
 * In other words, if kappa > 0, so (1 - kappa) < 1,
 * F(t+d) has expected value (1 - kappa)^d F(t) + (1 - (1 - kappa)^d) m.
 * It is distributed as a Gaussian, with variance:
 * s^2 * (1 - kc^(2d)) / (1 - kc^2).
 * 
 * If kappa == 0, so (1 - kappa) == 1,
 * F(t+d) has expected value F(t), is still a Gaussian,
 * and has variance d s^2 (Bienayme formula, variance of sum of uncorrelated variables
 * is sum of the variances).
 * 
 * If shockProb < 1, then mean reversion occurs in exactly those time steps in which
 * a jump occurs. This means that if we advance 5 time steps and have jumps in 2 and 4,
 * the pdf of future fundamental value is the same as when advancing 2 time steps and
 * jumping in both. That is, we simply replace "d" with "numJumps" in the pdfs above,
 * where "numJumps" is a binomial random variable, with n := d and p := shockVar.
 *
 * @author ewah
 */
public final class FundamentalValue implements Serializable {

	private static final Ordering<TimeStamp> tord = Ordering.natural();

	private final Timeline timeline;

	private final Stats stats;
	private final Rand rand;

	// kappa will typically be 0.05, which is the factor multiplied by the mean
	// kappac will typically be 0.95, which is the factor multiplied by the previous value
	private final double kappac; // kappa's complement (1 - kappa)
	private final double mean;
	private final double shockVar;
	private final double shockProb; // used for jump processes. if not a jump process, should be 1.0

	private TimeStamp lastTime;
	private Price lastPrice;


	/**
	 * @param kappa rate which the process reverts to the mean value
	 * @param mean mean
	 * @param var gaussian variance
	 */
	private FundamentalValue(Stats stats, Timeline timeline, double kappa, 
		double mean, double var, double prob, Rand rand
	) {
		checkArgument(Range.closed(0d, 1d).contains(kappa), "Kappa (%.4f) not in [0, 1]", kappa);
		this.stats = stats;
		this.timeline = timeline;
		this.rand = rand;
		this.kappac = 1 - kappa;
		this.mean = mean;
		this.shockVar = var;
		this.shockProb = prob;

		// stochastic initial conditions for random process
		lastPrice = Price.of(rand.nextGaussian(mean, shockVar)).nonnegative();
		lastTime = TimeStamp.ZERO;

		// Post statistics
		stats.postTimed(lastTime, Stats.FUNDAMENTAL, lastPrice.doubleValue());
		stats.post(Stats.CONTROL_FUNDAMENTAL, lastPrice.doubleValue());
	}

	/** Creates a mean reverting Gaussian Process that supports random access to small (int) TimeStamps. */
	public static FundamentalValue create(Stats stats, Timeline timeline, double kap,
		double meanVal, double var, double prob, Rand rand
	) {
		return new FundamentalValue(stats, timeline, kap, meanVal, var, prob, rand);
	}

	// mean reversion only occurs in time steps in which a jump occurs.
	// so if we advance 5 time steps, and there is a jump in steps 2 and 4,
	// we can sample the same pdf as when advancing 2 time steps and jumping in both.
	// the degree of mean reversion is based only on the number of time steps that have a jump.
	/** Returns the global fundamental value at TimeStamp time. */
	public Price getValueAt(TimeStamp time) {
		checkArgument(time.afterOrOn(lastTime), "Must query sequential times");
		if (time.after(lastTime)) {
			// deltat stores the number of ticks that elapsed, not necessarily the
			// number of jumps
			final long deltat = time.getInTicks() - lastTime.getInTicks();

			// sample binomial distribution over n = deltat ticks, with jump probability p = shockProb,
			// to get the number of jumps that will occur, based on shockProb probability of a jump
			// in a given time step (that is, tick).
			if (deltat > Integer.MAX_VALUE) {
				throw new IllegalStateException("Integer overflow to BinomialDistribution.");
			}
			final BinomialDistribution binomial = new BinomialDistribution((int) deltat, shockProb);
			final int numberOfJumps = binomial.sample();

			// Effective value of mean reversion factor, based on actual number of jumps occurring
			// this will be 1 if shockProb == 0.0
			double kappaToPower = Math.pow(kappac, numberOfJumps);
			
			// if kappac == 1, this means there is no mean reversion.
			double varianceFactor = numberOfJumps;
			if (kappac != 1) {
				// varianceFactor will be 0 if shockProb == 0.0
				varianceFactor = (1 - kappaToPower * kappaToPower) / (1 - kappac * kappac);
			}

			// Post "assuming" fundamental was previous value
			stats.post(Stats.CONTROL_FUNDAMENTAL, lastPrice.doubleValue(), deltat - 1);

			lastPrice =
				Price.of(
					rand.nextGaussian(
						(1 - kappaToPower) * mean
							+ kappaToPower * lastPrice.doubleValue(),
						shockVar * varianceFactor
					)
				).nonnegative();
			lastTime = time;

			stats.postTimed(lastTime, Stats.FUNDAMENTAL, lastPrice.doubleValue());
			stats.post(Stats.CONTROL_FUNDAMENTAL, lastPrice.doubleValue());
		}
		return lastPrice;
	}

	// TODO memoize? Lots of views with the same latency. Extra objects
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
			return FundamentalValue.this.getValueAt(
				tord.max(timeline.getCurrentTime().minus(latency), TimeStamp.ZERO)
			);
		}

		@Override
		public final TimeStamp getLatency() {
			return latency;
		}
	}

	private static final long serialVersionUID = 6764216196138108452L;
}
