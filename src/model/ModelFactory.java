package model;

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
	 * @param data
	 * @return
	 */
	public static MarketModel createModel(String type, ObjectProperties props,
										  SystemData data) { 
		
		if (type.equals(Consts.getModelType("TwoMarket"))) {
			return new TwoMarket(props, data);
		} else if (type.equals(Consts.getModelType("CentralCDA"))) {
			return new CentralCDA(props, data);
		} else if (type.equals(Consts.getModelType("CentralCall"))) {
			return new CentralCall(props, data);
		} else {
			return null;
		}
	}
}
