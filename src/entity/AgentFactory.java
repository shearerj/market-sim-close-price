package entity;

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
	 * @param data
	 * @param params
	 * @return
	 */
	public static Agent createAgent(String type,
									Integer agentID,
									SystemData data,
									AgentProperties params,
									Log l) {
		
		if (type.equals(Consts.getAgentType("DummyAgent"))) {
			return new DummyAgent(agentID, data, params, l);
		} else if (type.equals(Consts.getAgentType("LAAgent"))) {
			return new LAAgent(agentID, data, params, l);
		} else if (type.equals(Consts.getAgentType("MarketMakerAgent"))) {
			return new MarketMakerAgent(agentID, data, params, l);
		} else if (type.equals(Consts.getAgentType("ZIAgent"))) {
			return new ZIAgent(agentID, data, params, l);
		} else {
			return null;
		}
	}
	
	
//	/**
//	 * Creates a new agent based on type parameter. This constructor
//	 * uses the default AgentProperties setting.
//	 * 
//	 * @param type
//	 * @param agentID
//	 * @param data
//	 * @return
//	 */
//	public static Agent createAgent(String type,
//									Integer agentID,
//									SystemData data,
//									Log l) {
//		
//		AgentProperties params = Consts.getProperties(type);
//		if (type.toLowerCase().equals("zi")) {
//			return new TestAgent(agentID, data, params, l);
//		} else if (type.toLowerCase().equals("hft")) {
//			return new LAAgent(agentID, data, params, l);
//		} else if (type.toLowerCase().equals("mm")) {
//			return new MarketMakerAgent(agentID, data, params, l);
//		} else if (type.toLowerCase().equals("nbbo")) {
//			return new ZIAgent(agentID, data, params, l);
//		} else {
//			return null;
//		}
//	}
}
