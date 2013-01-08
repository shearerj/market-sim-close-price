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
	 * @param modelID
	 * @param props
	 * @param data
	 * @return
	 */
	public static MarketModel createModel(String type, int modelID, ObjectProperties props,
										  SystemData data) { 
		
		if (type.equals(Consts.getModelType("TwoMarket"))) {
			return new TwoMarket(modelID, props, data);
		} else if (type.equals(Consts.getModelType("CentralCDA"))) {
			return new CentralCDA(modelID, props, data);
		} else if (type.equals(Consts.getModelType("CentralCall"))) {
			return new CentralCall(modelID, props, data);
		} else {
			return null;
		}
	}
}
