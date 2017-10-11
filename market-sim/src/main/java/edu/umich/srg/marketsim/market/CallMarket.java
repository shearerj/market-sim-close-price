package edu.umich.srg.marketsim.market;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.fourheap.RandomProRataSelector;
import edu.umich.srg.marketsim.Keys.ClearInterval;
import edu.umich.srg.marketsim.Keys.Pricing;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Pricing of 0 prices at the lowest bid, 1 at the highest ask, and 0.5, the average between them.
 */
public class CallMarket extends AbstractMarket {

  private final long clearInterval;
  private boolean nextClearScheduled;

  private CallMarket(Sim sim, Fundamental fundamental, CallPricing pricing, long clearInterval,
      Random rand) {
    super(sim, fundamental, pricing, RandomProRataSelector.create(rand));
    this.clearInterval = clearInterval;
    this.nextClearScheduled = false;
  }

  public static CallMarket create(Sim sim, Fundamental fundamental, double pricing,
      long clearInterval, Random rand) {
    return new CallMarket(sim, fundamental, new CallPricing(pricing), clearInterval, rand);
  }

  public static CallMarket create(Sim sim, Fundamental fundamental, long clearInterval,
      Random rand) {
    return create(sim, fundamental, 0.5, clearInterval, rand);
  }

  public static CallMarket createFromSpec(Sim sim, Fundamental fundamental, Spec spec,
      Random rand) {
    return create(sim, fundamental, spec.get(Pricing.class), spec.get(ClearInterval.class), rand);
  }

  public long getClearingInterval() {
    return clearInterval;
  }

  /*
   * This function will schedule the next clear at the multiple of clearInterval strictly greater
   * than the current time. The down side is that if an event happens at a clear interval time, that
   * wasn't already scheduled, it will happen at the beginning of the next interval. Normally, it
   * would have a random change of occurring before or after other events. This shouldn't be a big
   * impact when the intervals are large because the probability of an event landing on an interval
   * is small, and the probability of the clear already begin scheduled anyways is high. This
   * shouldn't be a big deal when the interval is small, because likely the next clear will occur
   * before the order book has changed much. The real impact is on medium intervals where it might
   * happen somewhat frequently but it can still make a big difference in outcome.
   */
  private void scheduleClear() {
    if (!nextClearScheduled) {
      long current = sim.getCurrentTime().get();
      long next = (current / clearInterval + 1) * clearInterval;
      sim.scheduleIn(TimeStamp.of(next - current), this::clear);
      nextClearScheduled = true;
    }
  }

  @Override
  AbstractMarketOrder submitOrder(AbstractMarketView submitter, OrderType buyOrSell, Price price,
      int quantity) {
    scheduleClear();
    return super.submitOrder(submitter, buyOrSell, price, quantity);
  }

  @Override
  void withdrawOrder(AbstractMarketOrder order, int quantity) {
    scheduleClear();
    super.withdrawOrder(order, quantity);
  }

  @Override
  public void clear() {
    this.nextClearScheduled = false;
    super.clear();
    updateQuote();
    incrementMarketTime();
  }

  @Override
  public JsonObject getFeatures() {
    JsonObject features = super.getFeatures();
    features.add("type", new JsonPrimitive("call"));
    return features;
  }

  @Override
  public String toString() {
    return "CALL " + super.toString();
  }

  private static class CallPricing implements PricingRule {

    private final double pricing;

    private CallPricing(double pricing) {
      checkArgument(pricing >= 0 && pricing <= 1,
          "Pricing must be between 0 and 1 inclusive, was %d", pricing);
      this.pricing = pricing;
    }

    @Override
    public Iterable<Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price>> apply(
        Collection<MatchedOrders<Price, Long, AbstractMarketOrder>> matches) {
      double sell =
          matches.stream().mapToDouble(m -> m.getSell().getPrice().doubleValue()).max().orElse(0);
      double buy =
          matches.stream().mapToDouble(m -> m.getBuy().getPrice().doubleValue()).min().orElse(0);
      Price price = Price.of(pricing * sell + (1 - pricing) * buy);

      return () -> matches.stream()
          .<Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price>>map(
              m -> new AbstractMap.SimpleImmutableEntry<//
                  MatchedOrders<Price, Long, AbstractMarketOrder>, Price>(m, price))
          .iterator();
    }

  }

  private static final long serialVersionUID = 1L;

}
