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
	public static Market createMarket(String type,
									  Integer marketID,
									  SystemData data,
									  ObjectProperties params,
									  Log l, int ipID) { 
		
		if (type.equals(Consts.CDA)) {
			return new CDAMarket(marketID, data, params, l, ipID);
		} else if (type.equals(Consts.CALL)) {
			return new CallMarket(marketID, data, params, l, ipID);
		} else {
			return null;
		}
	}

}
