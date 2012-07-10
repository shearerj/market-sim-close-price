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
	 * @return
	 */
	public static Agent createAgent(String type, Integer agentID, SystemData data) { 
		
		if (type.toLowerCase().equals("zi")) {
			return new ZIAgent(agentID, data);
		} else if (type.toLowerCase().equals("hft")) {
			return new HFTAgent(agentID, data);
		} else if (type.toLowerCase().equals("mm")) {
			return new MarketMaker(agentID, data);
		} else if (type.toLowerCase().equals("nbbo")) {
			return new NBBOAgent(agentID, data);
		} else {
			return null;
		}
	}
}
