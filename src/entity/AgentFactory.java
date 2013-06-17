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
	public static Agent createAgent(String type,
									Integer agentID,
									Integer modelID,
									SystemData data,
									ObjectProperties params,
									Log l) {
		
		if (type.equals(Consts.DUMMY)) {
			return new DummyAgent(agentID, modelID, data, params, l);
		} else if (type.equals(Consts.LA)) {
			return new LAAgent(agentID, modelID, data, params, l);
		} else if (type.equals(Consts.BASICMARKETMAKER)) {
			return new BasicMarketMaker(agentID, modelID, data, params, l);
		} else if (type.equals(Consts.ZI)) {
			return new ZIAgent(agentID, modelID, data, params, l);
		} else if (type.equals(Consts.ZIR)) {
			return new ZIRAgent(agentID, modelID, data, params, l);
		} else if (type.equals(Consts.ZIP)) {
			return new ZIPAgent(agentID, modelID, data, params, l);
		} else if (type.equals(Consts.AA)) {
			return new AAAgent(agentID, modelID, data, params, l);
		} else {
			return null;
		}
	}
}
