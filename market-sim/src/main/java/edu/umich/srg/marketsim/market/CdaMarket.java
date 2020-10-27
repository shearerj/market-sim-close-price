package edu.umich.srg.marketsim.market;

import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.fourheap.PrioritySelector;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Benchmark.BenchmarkStyle;
import edu.umich.srg.marketsim.Keys.BenchmarkType;
import edu.umich.srg.marketsim.Keys.Markets;
import edu.umich.srg.marketsim.Keys.Rmin;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Random;

public class CdaMarket extends AMarket {

  private static final Ordering<AOrder> priceOrder = Ordering.natural().onResultOf(o -> o.sequence);

  private static Iterable<Entry<MatchedOrders<Price, AOrder>, Price>> pricingRule(
      Collection<MatchedOrders<Price, AOrder>> matches) {
    return () -> matches.stream().map(match -> Maps.immutableEntry(match,
        priceOrder.min(match.getBuy(), match.getSell()).getPrice())).iterator();
  }

  private CdaMarket(Sim sim, Fundamental fundamental, BenchmarkStyle benchmarkType) {
    // We can use an arbitrary selector, since there won't be ties on time
    super(sim, fundamental, CdaMarket::pricingRule, PrioritySelector.create(), benchmarkType);
  }

  public static CdaMarket create(Sim sim, Fundamental fundamental) {
    return new CdaMarket(sim, fundamental, BenchmarkStyle.VWAP);
  }

  public static CdaMarket createFromSpec(Sim sim, Fundamental fundamental, Spec spec, Random rand) {
    return new CdaMarket(sim, fundamental, spec.get(BenchmarkType.class));
  }

  @Override
  AOrder submitOrder(AMarketView submitter, OrderType buyOrSell, Price price, int quantity) {
    AOrder order = super.submitOrder(submitter, buyOrSell, price, quantity);
    clear();
    return order;
  }

  @Override
  void withdrawOrder(AOrder order, int quantity) {
    super.withdrawOrder(order, quantity);
    updateQuote();
  }

  @Override
  public void clear() {
    super.clear();
    updateQuote();
  }

  @Override
  public JsonObject getFeatures() {
    JsonObject features = super.getFeatures();
    features.add("type", new JsonPrimitive("cda"));
    return features;
  }

  @Override
  public String toString() {
    return "CDA " + super.toString();
  }

  private static final long serialVersionUID = 1L;

}
