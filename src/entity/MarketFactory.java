package entity;

import data.ObjectProperties;
import data.SystemData;
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
	public static Market createMarket(String type, Integer marketID,
			SystemData data, ObjectProperties params) {

		switch (Consts.MarketType.valueOf(type)) {
		case CDA:
			return new CDAMarket(marketID, data, params);
		case CALL:
			return new CallMarket(marketID, data, params);
		default:
			return null;
		}
	}

}
