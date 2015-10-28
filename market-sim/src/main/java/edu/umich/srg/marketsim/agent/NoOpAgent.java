package edu.umich.srg.marketsim.agent;

import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;

import java.util.Collection;
import java.util.Random;

public class NoOpAgent implements Agent {

  public static NoOpAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new NoOpAgent();
  }

  @Override
  public void initilaize() {}

  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  @Override
  public JsonObject getFeatures() {
    return new JsonObject();
  }

  // Notifications

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {}

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {}

  @Override
  public void notifyQuoteUpdated(MarketView market) {}

  @Override
  public void notifyTransaction(MarketView market, Price price, int quantity) {}

}
