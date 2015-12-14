package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;

import java.util.Random;

public abstract class Beta implements DoubleDistribution {

  private Beta() {}

  public static Beta with(double alpha, double beta) {
    if (alpha == 1 && beta == 1) {
      return new Uniform();
    } else if (alpha == beta && alpha > 1.5) {
      return new AhrensDieterBS(alpha);
    } else {
      throw new IllegalArgumentException(
          "Alpha = " + alpha + " and Beta = " + beta + " not implemented yet");
    }
  }

  private static class Uniform extends Beta {

    @Override
    public double sample(Random rand) {
      return rand.nextDouble();
    }

  }

  /**
   * Rejection Sampling method from:
   * 
   * s
   * 
   * This method is an approximation that is better at larger values of alpha
   */
  private static class AhrensDieterBS extends Beta {

    private final double a, A, t;

    private AhrensDieterBS(double alpha) {
      this.a = alpha;
      this.A = alpha - 1;
      this.t = Math.sqrt(a);
    }

    @Override
    public double sample(Random rand) {
      double x, s, s4, u;
      do {
        do {
          s = rand.nextGaussian();
          x = 0.5 * (1 + s / t);
        } while (x < 0 || x > 1);
        u = rand.nextDouble();
        s4 = s * s * s * s;
      } while (u > 1 - s4 / (8 * a - 12)
          && (u >= 1 - s4 / (8 * a - 8) + 0.5 * (s4 / (8 * a - 8)) * (s4 / (8 * a - 8))
              || Math.log1p(u - 1) > A * Math.log1p(4 * x * (1 - x) - 1) + s * s / 2));
      return x;
    }
  }

}
