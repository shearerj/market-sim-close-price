package edu.umich.srg.fourheap;

/** An order meant for use in a fourheap. */
public interface Order<P, T> {

  P getPrice();

  T getTime();

  OrderType getType();

}
