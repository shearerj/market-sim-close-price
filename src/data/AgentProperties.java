package data;

import systemmanager.Consts.AgentType;

public class AgentProperties extends ObjectProperties {

	protected final AgentType type;
	
	public AgentProperties(AgentType type) {
		super();
		this.type = type;
	}

	public AgentProperties(AgentProperties copy) {
		super(copy);
		this.type = copy.type;
	}

	public AgentProperties(AgentType type, String config) {
		super(config);
		this.type = type;
	}
	
	public AgentProperties(AgentType type, ObjectProperties def, String config) {
		super(def, config);
		this.type = type;
	}
	
	public AgentType getAgentType() {
		return type;
	}

}
