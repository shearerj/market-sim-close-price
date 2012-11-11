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
									  EntityProperties params,
									  Log l) { 
		
		if (type.equals(Consts.getMarketType("CDAMarket"))) {
			return new CDAMarket(marketID, data, params, l);
		} else if (type.equals(Consts.getMarketType("CallMarket"))) {
			return new CallMarket(marketID, data, params, l);
		} else {
			return null;
		}
	}

}
