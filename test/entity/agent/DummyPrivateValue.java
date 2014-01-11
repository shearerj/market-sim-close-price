package entity.agent;

import java.util.Arrays;
import java.util.Collection;

import logger.Logger;
import entity.market.Price;

/**
 * DummyPrivateValue 
 * 
 * Helper class for ZIAgentTest
 * 
 * Initialized to all zero, so private value will equal fundamental
 * -OR-
 * Initialized with a list of predefined  values (Will check if maxPosition is correct)
 * 
 * Prints contents into log file. 
 * 
 * @author yngchen
 * 
 */
public class DummyPrivateValue extends PrivateValue {
	
	private static final long serialVersionUID = 1L;
	
	public DummyPrivateValue(int absMaxPosition, Collection<Price> prices){
		super(absMaxPosition, prices);
		Logger.log(Logger.Level.DEBUG, "DummyPrivateValue elements: " + Arrays.toString(prices.toArray()));
	}
}
