package edu.umich.srg.marketsim.testing;

import com.google.gson.JsonObject;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderRecord;

public class MockAgent implements Agent {

  public int transactions;
  public int transactedUnits;
  public Price lastTransactionPrice;

  public MockAgent() {
    this.transactions = 0;
    this.lastTransactionPrice = null;
  }

  @Override
  public void initilaize() {}

  @Override
  public JsonObject getFeatures() {
    return new JsonObject();
  }

  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {}

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    transactions++;
    transactedUnits += quantity;
    lastTransactionPrice = price;
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {}

  @Override
  public void notifyTransaction(MarketView market, Price price, int quantity) {}

}
