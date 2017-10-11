package edu.umich.srg.fourheap;

/** An order meant for use in a fourheap. */
public interface IOrder<P> {

  P getPrice();

  OrderType getType();

}
