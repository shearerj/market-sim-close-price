package model;

import data.ObjectProperties;
import data.SystemData;
import systemmanager.Consts.ModelType;

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
	public static MarketModel createModel(ModelType type, int modelID, ObjectProperties props,
										  SystemData data, int sipID) {		
		switch (type) {
		case TWOMARKET:
			return new TwoMarket(modelID, props, data, sipID);
		case CENTRALCDA:
			return new CentralCDA(modelID, props, data, sipID);
		case CENTRALCALL:
			return new CentralCall(modelID, props, data, sipID);
		default:
			return null; // Maybe not appropriate?
		}
	}
}
