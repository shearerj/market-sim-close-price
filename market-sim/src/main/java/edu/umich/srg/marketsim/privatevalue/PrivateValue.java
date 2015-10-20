package edu.umich.srg.marketsim.privatevalue;

import edu.umich.srg.fourheap.Order.OrderType;

public interface PrivateValue {
	
	public double valueForExchange(int position, OrderType type);
	
	public double valueAtPosition(int position);
	
}
