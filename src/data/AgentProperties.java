package data;

import static com.google.common.base.Preconditions.checkNotNull;
import systemmanager.Consts.AgentType;

/**
 * Entity properties that is bundeled with an AgentType.
 * 
 * @author erik
 * 
 */
public class AgentProperties extends EntityProperties {

	private static final long serialVersionUID = -8267036814743083118L;
	
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
		this(config.indexOf(':') == -1 ? AgentType.valueOf(config) : AgentType.valueOf(config.substring(0, config.indexOf(':'))),
				config.indexOf(':') == -1 ? "" : config.substring(config.indexOf(':') + 1));
	}

	public AgentProperties(AgentType type, EntityProperties def, String config) {
		super(def, config);
		this.type = type;
	}

	public AgentType getAgentType() {
		return type;
	}

	@Override
	public String toString() {
		return type + " " + super.toString();
	}

}
