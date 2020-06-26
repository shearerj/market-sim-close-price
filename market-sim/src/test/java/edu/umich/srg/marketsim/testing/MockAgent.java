package edu.umich.srg.marketsim.testing;

import com.google.gson.JsonObject;

import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;

public class MockAgent implements Agent {

  public final PrivateValue privateValue;
  public final int id;
  public int transactions;
  public int transactedUnits;
  public Price lastTransactionPrice;

  private MockAgent(int id, PrivateValue privateValue) {
    this.privateValue = privateValue;
    this.id = id;
    this.transactions = 0;
    this.lastTransactionPrice = null;
  }

  public MockAgent() {
    this(0, PrivateValues.noPrivateValue());
  }

  public static MockAgent create() {
    return builder().build();
  }

  public static MockAgentBuilder builder() {
    return new MockAgentBuilder();
  }

  @Override
  public void initilaize() {}

  @Override
  public JsonObject getFeatures() {
    return new JsonObject();
  }
  
  @Override
  public int getBenchmarkDir() {
	return 0;
  }
  
  @Override
  public double getContractHoldings() {
	  return 0;
  }
  
  @Override
  public double getRunningPayoff() {
	return -1;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return "MOCK " + Integer.toUnsignedString(id, 36).toUpperCase();
  }

  @Override
  public double payoffForExchange(int position, OrderType type) {
    return privateValue.valueForExchange(position, type);
  }

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    transactions++;
    transactedUnits += quantity;
    lastTransactionPrice = price;
  }

  public static class MockAgentBuilder {

    private PrivateValue privateValue;
    private int id;

    private MockAgentBuilder() {
      this.privateValue = PrivateValues.noPrivateValue();
      this.id = 0;
    }

    public MockAgentBuilder privateValue(PrivateValue privateValue) {
      this.privateValue = privateValue;
      return this;
    }

    public MockAgentBuilder id(int id) {
      this.id = id;
      return this;
    }

    public MockAgent build() {
      return new MockAgent(id, privateValue);
    }

  }

}
