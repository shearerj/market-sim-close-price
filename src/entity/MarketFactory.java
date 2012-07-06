package entity;

import systemmanager.*;

/**
 * Factory class for creating Markets.
 * 
 * @author ewah
 */
public class MarketFactory {
	
	/**
	 * Create a new Market based on type parameter.
	 * 
	 * @param type
	 * @param marketID
	 * @param data
	 * @return
	 */
	public static Market createMarket(String type, Integer marketID, SystemData data) { 
		
		if (type.toLowerCase().equals("cda")) {
			return new CDAMarket(marketID, data);
		} else {
			return null;
		}
	}

}
