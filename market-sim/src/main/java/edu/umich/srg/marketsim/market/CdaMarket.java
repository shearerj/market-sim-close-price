package edu.umich.srg.marketsim.market;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.fourheap.PrioritySelector;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

public class CdaMarket extends AbstractMarket {

  private static final PricingRule cdaPricing = matches -> {
    Iterator<MatchedOrders<Price, Long, AbstractMarketOrder>> it = matches.iterator();
    return () -> new Iterator<Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price>>() {

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public Entry<MatchedOrders<Price, Long, AbstractMarketOrder>, Price> next() {
        MatchedOrders<Price, Long, AbstractMarketOrder> match = it.next();
        // less than is okay here, because two orders will never have the same "time"
        Price price = match.getBuy().getTime() < match.getSell().getTime()
            ? match.getBuy().getPrice() : match.getSell().getPrice();
        return Maps.immutableEntry(match, price);
      }

    };
  };

  private CdaMarket(Sim sim, Fundamental fundamental) {
    // We can use an arbitrary selector, since there won't be ties on time
    super(sim, fundamental, cdaPricing, PrioritySelector.create());
  }

  public static CdaMarket create(Sim sim, Fundamental fundamental) {
    return new CdaMarket(sim, fundamental);
  }

  public static CdaMarket createFromSpec(Sim sim, Fundamental fundamental, Spec spec, Random rand) {
    return new CdaMarket(sim, fundamental);
  }

  @Override
  AbstractMarketOrder submitOrder(AbstractMarketView submitter, OrderType buyOrSell, Price price,
      int quantity) {
    AbstractMarketOrder order = super.submitOrder(submitter, buyOrSell, price, quantity);
    clear();
    incrementMarketTime();
    return order;
  }

  @Override
  void withdrawOrder(AbstractMarketOrder order, int quantity) {
    super.withdrawOrder(order, quantity);
    updateQuote();
    incrementMarketTime();
  }

  @Override
  void clear() {
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
