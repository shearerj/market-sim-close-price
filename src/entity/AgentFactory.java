package entity;

import systemmanager.*;

/**
 * Factory class for creating Agents.
 * 
 * @author ewah
 */
public class AgentFactory {

	/**
	 * Creates a new multi-market agent based on type parameter.
	 * 
	 * @param type
	 * @param agentID
	 * @param modelID
	 * @param data
	 * @param params
	 * @return
	 */
	public static Agent createMMAgent(String type,
									Integer agentID,
									Integer modelID,
									SystemData data,
									ObjectProperties params,
									Log l) {
		
		if (type.equals(Consts.getAgentType("DummyAgent"))) {
			return new DummyAgent(agentID, modelID, data, params, l);
		} else if (type.equals(Consts.getAgentType("LAAgent"))) {
			return new LAAgent(agentID, modelID, data, params, l);
		} else {
			return null;
		}
	}
	
	
	/**
	 * Creates a new single market agent based on type parameter.
	 * 
	 * @param type
	 * @param agentID
	 * @param modelID
	 * @param data
	 * @param params
	 * @return
	 */
	public static Agent createSMAgent(String type,
									Integer agentID,
									Integer modelID,
									SystemData data,
									ObjectProperties params,
									Log l,
									Integer mktID) {
		
		if (type.equals(Consts.getAgentType("MarketMakerAgent"))) {
			return new MarketMakerAgent(agentID, modelID, data, params, l, mktID);
		} else if (type.equals(Consts.getAgentType("ZIAgent"))) {
			return new ZIAgent(agentID, modelID, data, params, l, mktID);
		} else if (type.equals(Consts.getAgentType("ZIPAgent"))) {
			return new ZIPAgent(agentID, modelID, data, params, l, mktID);
		} else {
			return null;
		}
	}
}
