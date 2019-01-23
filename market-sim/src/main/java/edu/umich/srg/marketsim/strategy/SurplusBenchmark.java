package edu.umich.srg.marketsim.strategy;

import static edu.umich.srg.fourheap.OrderType.BUY;

import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Quote;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SurplusBenchmark {
  private static final Map<Double, SurplusBenchmark> memoized = new HashMap<>();

  private final double threshold;

  private SurplusBenchmark(double threshold) {
    this.threshold = threshold;
  }
  
  public static SurplusBenchmark create(double threshold) {
    return memoized.computeIfAbsent(threshold, SurplusBenchmark::new);
  }
  
  /*
  // This class is memoized on the assumption that not many different thresholds will be used
  private static final Map<Integer, SurplusBenchmark> memoized = new HashMap<>();

  private final int contract;

  private SurplusBenchmark(int contract) {
    this.contract = contract;
  }

  public static SurplusBenchmark create(int contract) {
    return memoized.computeIfAbsent(contract, SurplusBenchmark::new);
  }
  */

  /** Get the price to submit conditioned on the demanded surplus and the market state. */
  public double benchPrice(OrderType type, Quote quote, double estimatedValue,
      double demandedSurplus, double contract) {
    Optional<Price> marketPrice = type == BUY ? quote.getAskPrice() : quote.getBidPrice();
    
    double benchSurplus = demandedSurplus - type.sign() * contract;
	if (marketPrice.isPresent()
		&& type.sign() * (estimatedValue - marketPrice.get().doubleValue()) < Math.max(benchSurplus, 0)
		&& type.sign() * (estimatedValue - marketPrice.get().doubleValue()) > Math.min(benchSurplus, benchSurplus * threshold)) {
		return marketPrice.get().doubleValue();
	} else { // Submit order that won't transact immediately
	      // Round beneficially
		return estimatedValue - type.sign() * benchSurplus;
	}
  }
}
