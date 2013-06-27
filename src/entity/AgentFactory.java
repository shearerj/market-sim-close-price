package entity;

import data.ObjectProperties;
import data.SystemData;
import systemmanager.Consts.AgentType;

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
	public static Agent createAgent(AgentType type, Integer agentID,
			Integer modelID, SystemData data, ObjectProperties params) {
		switch (type) {
		case DUMMY:
			return new DummyAgent(agentID, modelID, data, params);
		case LA:
			return new LAAgent(agentID, modelID, data, params);
		case BASICMM:
			return new BasicMarketMaker(agentID, modelID, data, params);
		case ZI:
			return new ZIAgent(agentID, modelID, data, params);
		case ZIR:
			return new ZIRAgent(agentID, modelID, data, params);
		case ZIP:
			return new ZIPAgent(agentID, modelID, data, params);
		case AA:
			return new AAAgent(agentID, modelID, data, params);
		default:
			return null;
		}
	}
}
