package model;

import data.ObjectProperties;
import data.SystemData;
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
										  SystemData data, int sipID, Log log) { 
		
		if (type.equals(Consts.TWOMARKET)) {
			return new TwoMarket(modelID, props, data, sipID, log);
		} else if (type.equals(Consts.CENTRALCDA)) {
			return new CentralCDA(modelID, props, data, sipID, log);
		} else if (type.equals(Consts.CENTRALCALL)) {
			return new CentralCall(modelID, props, data, sipID, log);
		} else {
			return null;
		}
	}
}
