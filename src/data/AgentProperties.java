package data;

import systemmanager.Consts.SMAgentType;

public class AgentProperties extends EntityProperties {

	protected final SMAgentType type;

	public AgentProperties(SMAgentType type) {
		super();
		this.type = type;
	}

	public AgentProperties(AgentProperties copy) {
		super(copy);
		this.type = copy.type;
	}

	public AgentProperties(SMAgentType type, String config) {
		super(config);
		this.type = type;
	}

	public AgentProperties(String config) {
		this(SMAgentType.valueOf(config.substring(0, config.indexOf(':'))),
				config.substring(config.indexOf(':') + 1));
	}

	public AgentProperties(SMAgentType type, EntityProperties def, String config) {
		super(def, config);
		this.type = type;
	}

	public SMAgentType getAgentType() {
		return type;
	}

}
