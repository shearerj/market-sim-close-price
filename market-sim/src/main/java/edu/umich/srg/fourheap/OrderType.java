package edu.umich.srg.fourheap;

public enum OrderType {
  BUY {

    @Override
    public int sign() {
      return 1;
    }

    @Override
    public OrderType opposite() {
      return OrderType.SELL;
    }

  },
  SELL {

    @Override
    public int sign() {
      return -1;
    }

    @Override
    public OrderType opposite() {
      return OrderType.BUY;
    }

  };

  public abstract int sign();

  public abstract OrderType opposite();

}
