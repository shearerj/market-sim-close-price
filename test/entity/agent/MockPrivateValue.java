package entity.agent;

import java.util.Arrays;
import java.util.List;

import logger.Logger;

import entity.market.Price;

public class MockPrivateValue extends PrivateValue {
	
	/**
	 * Mock Private Value to make testing of ZIstrategy easier
	 * 
	 * Initialized to all zero, so private value will equal fundamental
	 * -OR-
	 * Initialized with a list of values (Will check if maxPosition is correct)
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	
	public MockPrivateValue(){
		super();

	}

	
	public MockPrivateValue(int maxPosition, List<Price> prices){
		super(maxPosition, prices);
		Logger.log(Logger.Level.DEBUG, "MockPrivateValue elements: " + Arrays.toString(prices.toArray()));
		
	}
	


}
