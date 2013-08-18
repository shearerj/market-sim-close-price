package data;

import static com.google.common.base.Preconditions.checkNotNull;
import systemmanager.Consts.AgentType;

public class AgentProperties extends EntityProperties {

	protected final AgentType type;

	public AgentProperties(AgentType type) {
		super();
		this.type = checkNotNull(type, "Type");
	}

	public AgentProperties(AgentProperties copy) {
		super(copy);
		this.type = copy.type;
	}

	public AgentProperties(AgentType type, String config) {
		super(config);
		this.type = type;
	}

	public AgentProperties(String config) {
		this(AgentType.valueOf(config.substring(0, config.indexOf(':'))),
				config.substring(config.indexOf(':') + 1));
	}

	public AgentProperties(AgentType type, EntityProperties def, String config) {
		super(def, config);
		this.type = type;
	}

	public AgentType getAgentType() {
		return type;
	}

}
