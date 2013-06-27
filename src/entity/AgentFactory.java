package entity;

import data.ObjectProperties;
import data.SystemData;
import systemmanager.*;

/**
 * Factory class for creating Agents.
 * 
 * @author ewah
 */
public class AgentFactory {

	/**
	 * Creates a new agent based on type parameter.
	 * 
	 * @param type
	 * @param agentID
	 * @param modelID
	 * @param data
	 * @param params
	 * @return
	 */
	public static Agent createAgent(String type, Integer agentID,
			Integer modelID, SystemData data, ObjectProperties params) {
		
		switch (type) {
		case Consts.DUMMY:
			return new DummyAgent(agentID, modelID, data, params);
		case Consts.LA:
			return new LAAgent(agentID, modelID, data, params);
		case Consts.BASICMARKETMAKER:
			return new BasicMarketMaker(agentID, modelID, data, params);
		case Consts.ZI:
			return new ZIAgent(agentID, modelID, data, params);
		case Consts.ZIR:
			return new ZIRAgent(agentID, modelID, data, params);
		case Consts.ZIP:
			return new ZIPAgent(agentID, modelID, data, params);
		case Consts.AA:
			return new AAAgent(agentID, modelID, data, params);
		default:
			return null;
		}
	}
}
