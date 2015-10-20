package edu.umich.srg.distributions;

import java.io.Serializable;
import java.util.Random;

import static com.google.common.base.Preconditions.*;

import edu.umich.srg.distributions.Distribution.IntDistribution;

public abstract class Hypergeometric implements IntDistribution, Serializable {
	
	public static Hypergeometric with(int populationSize, int numSuccesses, int draws) {
		checkArgument(0 <= populationSize);
		checkArgument(0 <= numSuccesses && numSuccesses <= populationSize);
		checkArgument(0 <= draws && draws <= populationSize);
		
		int offset = 0, sign = 1;
		if (2 * draws > populationSize) {
			draws = populationSize - draws;
			offset += sign * numSuccesses;
			sign *= -1;
		}
		if (2 * numSuccesses > populationSize) {
			numSuccesses = populationSize - numSuccesses;
			offset += sign * draws;
			sign *= -1;
		}
		return new OffsetHypergeometric(hypergeometricSwitch(populationSize, numSuccesses, draws), offset, sign);
	}
	
	private static Hypergeometric hypergeometricSwitch(int populationSize, int numSuccesses, int draws) {
		if (numSuccesses == 0 || draws == 0)
			return new ConstantHypergeometric(0);
		else if (draws == populationSize)
			return new ConstantHypergeometric(numSuccesses);
		else if (numSuccesses == populationSize)
			return new ConstantHypergeometric(draws);
		else if (populationSize > 15)
			return new InverseCMFHypergeometric(populationSize, numSuccesses, draws);
		else
			return new BruteHypergeometric(populationSize, numSuccesses, draws);
	}
	
	private static class InverseCMFHypergeometric extends Hypergeometric {

		private final InverseCMF invCmf;
		
		/**
		 * @param bn N population size
		 * @param bk K number of successes in population
		 * @param n  n number of draws
		 */
		private InverseCMFHypergeometric(int bn, int bk, int n) {
			double nn = bn - n,
					nk = bn - bk,
					nkn = bn - bk - n,
					p0 = Math.exp(
							(nk + 0.5) * Math.log(nk)
							+ (nn + 0.5) * Math.log(nn)
							- (nkn + 0.5) * Math.log(nkn)
							- (bn + 0.5) * Math.log(bn)
						);
			this.invCmf = new InverseCMF(p0, (InverseCMF.PmfFunction & Serializable)
					(pk, k) -> pk * (bk - k + 1) * (n - k + 1) / (k * (nkn + k)));
		}
		
		@Override
		public int sample(Random rand) {
			return invCmf.sample(rand);
		}
		
		private static final long serialVersionUID = 1;
		
	}
	
	private static class BruteHypergeometric extends Hypergeometric {

		private final int populationSize, numSuccesses, draws;
		
		private BruteHypergeometric(int populationSize, int numSuccesses, int draws) {
			this.populationSize = populationSize;
			this.numSuccesses = numSuccesses;
			this.draws = draws;
		}
		
		@Override
		public int sample(Random rand) {
			int result = 0, populationLeft = populationSize, successesLeft = numSuccesses;
			for (int i = 0; i < draws; ++i) {
				if (rand.nextInt(populationLeft) < successesLeft) {
					++result;
					--successesLeft;
				}
				--populationLeft;
			}
			return result;
		}
		
		private static final long serialVersionUID = 1;
	
	}
	
	private static class ConstantHypergeometric extends Hypergeometric {
		
		private final int constant;
		
		private ConstantHypergeometric(int constant) {
			this.constant = constant;
		}

		@Override
		public int sample(Random rand) {
			return constant;
		}
		
		private static final long serialVersionUID = 1;
		
	}
	
	private static class OffsetHypergeometric extends Hypergeometric {
		private final Hypergeometric other;
		private final int offset, sign;
		
		private OffsetHypergeometric(Hypergeometric other, int offset, int sign) {
			this.other = other;
			this.offset = offset;
			this.sign = sign;
		}
	
		@Override
		public int sample(Random rand) {
			return offset + sign * other.sample(rand);
		}
		
		private static final long serialVersionUID = 1;
	}
	
	private static final long serialVersionUID = 1;
	
}
