package edu.umich.srg.marketsim.market;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.MatchedOrders;
import edu.umich.srg.fourheap.Order;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;

import java.util.Iterator;
import java.util.Map.Entry;

public class CdaMarket extends AbstractMarket {

  private static final PricingRule cdaPricing = matches -> {
    Iterator<MatchedOrders<Price>> it = matches.iterator();
    return () -> new Iterator<Entry<MatchedOrders<Price>, Price>>() {

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public Entry<MatchedOrders<Price>, Price> next() {
        MatchedOrders<Price> match = it.next();
        // less than is okay here, because two orders will never have the same time
        return Maps.immutableEntry(match,
            match.getBuy().getSubmitTime() < match.getSell().getSubmitTime()
                ? match.getBuy().getPrice() : match.getSell().getPrice());
      }

    };
  };

  private CdaMarket(Sim sim) {
    super(sim, cdaPricing);
  }

  public static CdaMarket create(Sim sim) {
    return new CdaMarket(sim);
  }

  public static CdaMarket createFromSpec(Sim sim, Spec spec) {
    return new CdaMarket(sim);
  }

  @Override
  Order<Price> submitOrder(AbstractMarketView submitter, OrderType buyOrSell, Price price,
      int quantity) {
    Order<Price> order = super.submitOrder(submitter, buyOrSell, price, quantity);
    clear();
    return order;
  }

  @Override
  void withdrawOrder(Order<Price> order, int quantity) {
    super.withdrawOrder(order, quantity);
    updateQuote();
  }

  @Override
  void clear() {
    super.clear();
    updateQuote();
  }

  @Override
  public JsonObject getFeatures(Fundamental fundamental) {
    JsonObject features = super.getFeatures(fundamental);
    features.add("type", new JsonPrimitive("cda"));
    return features;
  }

  @Override
  public String toString() {
    return "CDA " + super.toString();
  }


  private static final long serialVersionUID = 1L;

}
