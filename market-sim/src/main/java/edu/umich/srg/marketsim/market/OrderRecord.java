package edu.umich.srg.marketsim.market;


import edu.umich.srg.fourheap.OrderType;
import edu.umich.srg.marketsim.Price;

public interface OrderRecord {

  OrderType getType();

  Price getPrice();

}
