package entity.agent;

import java.util.Arrays;
import java.util.List;

import logger.Logger;

import entity.market.Price;

public class DummyPrivateValue extends PrivateValue {
	
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
	
	private static final long serialVersionUID = 1L;
	
	public DummyPrivateValue(){
		super();

	}

	
	public DummyPrivateValue(int maxPosition, List<Price> prices){
		super(maxPosition, prices);
		Logger.log(Logger.Level.DEBUG, "DummyPrivateValue elements: " + Arrays.toString(prices.toArray()));
		
	}
	


}
