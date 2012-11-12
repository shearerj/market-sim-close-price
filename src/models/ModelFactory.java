package models;

import systemmanager.*;

/**
 * Factory class for creating MarketModels.
 * 
 * @author ewah
 */
public class ModelFactory {

	/**
	 * Create a new MarketModel based on type parameter.
	 * 
	 * @param type
	 * @param props
	 * @return
	 */
	public static MarketModel createModel(String type, ObjectProperties props) { 
		
		if (type.equals(Consts.getModelType("TwoMarket"))) {
			return new TwoMarket(props);
		} else if (type.equals(Consts.getModelType("CentralCDA"))) {
			return new CentralCDA(props);
		} else if (type.equals(Consts.getModelType("CentralCall"))) {
			return new CentralCall(props);
		} else {
			return null;
		}
	}
}
