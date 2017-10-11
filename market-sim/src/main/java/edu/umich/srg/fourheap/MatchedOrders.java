package edu.umich.srg.fourheap;

public class MatchedOrders<P, O extends IOrder<P>> {

  private final O buy;
  private final O sell;
  private final int quantity;

  MatchedOrders(O buy, O sell, int quantity) {
    assert buy.getType() == OrderType.BUY;
    assert sell.getType() == OrderType.SELL;

    this.buy = buy;
    this.sell = sell;
    this.quantity = quantity;
  }

  public O getBuy() {
    return buy;
  }

  public O getSell() {
    return sell;
  }

  public int getQuantity() {
    return quantity;
  }

  @Override
  public String toString() {
    return "<buy=" + buy + ", sell=" + sell + ", quantity=" + quantity + ">";
  }

}
