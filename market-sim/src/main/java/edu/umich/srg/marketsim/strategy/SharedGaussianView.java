package edu.umich.srg.marketsim.strategy;

import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianFundamentalView.GaussableView;

import java.lang.ref.WeakReference;
import java.util.Random;

public final class SharedGaussianView {

  private static LoadingCache<Key, GaussianFundamentalView> singletons =
      CacheBuilder.newBuilder().weakValues().build(CacheLoader.from(SharedGaussianView::create));

  private SharedGaussianView() {}

  public static GaussianFundamentalView create(Sim sim, Fundamental fundamental, Random rand,
      double observationVariance) {
    return singletons.getUnchecked(new Key(sim, fundamental, rand, observationVariance));
  }

  private static GaussianFundamentalView create(Key key) {
    return ((GaussableView) key.fundamental.get().getView(key.sim.get())).addNoise(key.rand.get(),
        key.observationVariance);
  }

  private static class Key {

    private final WeakReference<Sim> sim;
    private final WeakReference<Fundamental> fundamental;
    private final WeakReference<Random> rand;
    private final double observationVariance;

    private Key(Sim sim, Fundamental fundamental, Random rand, double observationVariance) {
      this.sim = new WeakReference<>(sim);
      this.fundamental = new WeakReference<>(fundamental);
      this.rand = new WeakReference<>(rand);
      this.observationVariance = observationVariance;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(sim.get(), fundamental.get(), observationVariance);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof Key) {
        Key that = (Key) other;
        return Objects.equal(this.sim.get(), that.sim.get())
            && Objects.equal(this.fundamental.get(), that.fundamental.get())
            && this.observationVariance == that.observationVariance;
      } else {
        return false;
      }
    }
  }

}
