package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

/**
 * Class to store and compute a stochastic process used as a base to determine
 * the private valuations of agents.
 */
public interface Fundamental {

	Price getValueAt(TimeStamp time);
	
}
