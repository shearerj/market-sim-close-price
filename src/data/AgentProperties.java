package data;

import systemmanager.Consts.AgentType;

public class AgentProperties extends EntityProperties {
	
	AgentType type;

	public AgentProperties(AgentType type, String config) {
		super(config);
		this.type = type;
	}

}
