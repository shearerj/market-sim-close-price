package entity;

import java.util.Collection;

import model.MarketModel;
import systemmanager.Consts;

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
			MarketModel model, int ipID, Collection<LAIP> ip_las) {

		switch (Consts.MarketType.valueOf(type)) {
		case CDA:
			return new CDAMarket(marketID, model, ipID);
		case CALL:
			//return new CallMarket(marketID, model, ipID);
		default:
			return null;
		}
	}

}
