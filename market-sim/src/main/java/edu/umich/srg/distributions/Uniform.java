package edu.umich.srg.distributions;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.math.LongMath;

/**
 * An interface with a lot on convenience methods for creating different uniform
 * distributions
 */
public final class Uniform<T> implements Distribution<T> {
	
	private final List<T> options;
	
	private Uniform(Iterator<? extends T> options) {
		this.options = ImmutableList.copyOf(options);
	}
	
	@Override
	public T sample(Random rand) {
		return options.get(rand.nextInt(options.size()));
	}
	
	public static <T> Uniform<T> over(Iterator<? extends T> options) {
		return new Uniform<T>(options);
	}
	
	public static <T> Uniform<T> over(Iterable<? extends T> options) {
		return new Uniform<T>(options.iterator());
	}
	
	public static <T> Uniform<T> over(@SuppressWarnings("unchecked") T... options) {
		return new Uniform<T>(Arrays.asList(options).iterator());
	}
	
	// Int
	
	public static IntUniform discreteClosed(int minInclusive, int maxInclusive) {
		return new IntUniform(minInclusive, BoundType.CLOSED, maxInclusive, BoundType.CLOSED);
	}
	
	public static IntUniform discreteClosedOpen(int minInclusive, int maxExclusive) {
		return new IntUniform(minInclusive, BoundType.CLOSED, maxExclusive, BoundType.OPEN);
	}
	
	public static IntUniform discreteOpenClosed(int minExclusive, int maxInclusive) {
		return new IntUniform(minExclusive, BoundType.OPEN, maxInclusive, BoundType.CLOSED);
	}
	
	public static IntUniform discreteOpen(int minExclusive, int maxExclusive) {
		return new IntUniform(minExclusive, BoundType.OPEN, maxExclusive, BoundType.OPEN);
	}
	
	public static class IntUniform implements IntDistribution {
		// FIXME this will fail if the range is larger than Integer.MAX_VALUE;
		private final int range, offset;
		
		private IntUniform(int min, BoundType minBound, int max, BoundType maxBound) {
			this.range = max - min - (minBound == BoundType.OPEN ? 1 : 0) + (maxBound == BoundType.CLOSED ? 1 : 0);
			checkArgument(range > 0, "Must have a non zero range to sample from");
			this.offset = min;
		}
		
		@Override
		public int sample(Random rand) {
			return rand.nextInt(range) + offset;
		}
		
	}
	
	// Long
	
	public static LongUniform discreteClosed(long minInclusive, long maxInclusive) {
		return new LongUniform(minInclusive, BoundType.CLOSED, maxInclusive, BoundType.CLOSED);
	}
	
	public static LongUniform discreteClosedOpen(long minInclusive, long maxExclusive) {
		return new LongUniform(minInclusive, BoundType.CLOSED, maxExclusive, BoundType.OPEN);
	}
	
	public static LongUniform discreteOpenClosed(long minExclusive, long maxInclusive) {
		return new LongUniform(minExclusive, BoundType.OPEN, maxInclusive, BoundType.CLOSED);
	}
	
	public static LongUniform discreteOpen(long minExclusive, long maxExclusive) {
		return new LongUniform(minExclusive, BoundType.OPEN, maxExclusive, BoundType.OPEN);
	}
	
	public static class LongUniform implements LongDistribution {

		private final long min, max, range, offset;
		
		private LongUniform(long min, BoundType minBound, long max, BoundType maxBound) {
			// TODO Might be able to use unsigned math for this...
			this.range = LongMath.checkedAdd(LongMath.checkedSubtract(max, min) - (minBound == BoundType.OPEN ? 1 : 0), (maxBound == BoundType.CLOSED ? 1 : 0));
			checkArgument(range > 0, "Must have a non zero range to sample from");
			this.offset = min;
			this.min = Long.MIN_VALUE - (Long.MIN_VALUE % range);
			this.max = Long.MAX_VALUE - (Long.MAX_VALUE % range);
		}
		
		@Override
		public long sample(Random rand) {
			long result;
			do {
				result = rand.nextLong();
			} while (result >= min && result < max);
			return result % range + offset;
		}
		
	}
	
	// Double
	
	public static ContinuousUniform continuous(double minInclusive, double maxExclusive) {
		return new ContinuousUniform(minInclusive, maxExclusive);
	}
	
	public static ContinuousUniform continuous() {
		return new ContinuousUniform(0, 1);
	}

	public static class ContinuousUniform implements DoubleDistribution {

		private final double range, offset;
		
		private ContinuousUniform(double minInclusive, double maxExclusive) {
			this.range = maxExclusive - minInclusive;
			this.offset = minInclusive;
		}
		
		@Override
		public double sample(Random rand) {
			return rand.nextDouble() * range + offset;
		}
		
	}
	
}
