package data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.collect.Maps;

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
	
	protected AgentProperties(AgentType type, Map<String, String> backedProperties) {
		super(backedProperties);
		this.type = type;
	}
	
	public static AgentProperties fromConfigString(String configString) {
		int index = checkNotNull(configString).indexOf(':');
		if (index == -1)
			return new AgentProperties(AgentType.valueOf(configString), Maps.<String, String> newHashMap());
		return new AgentProperties(AgentType.valueOf(configString.substring(0, index)),
				parseConfigString(configString.substring(index + 1)));
	}
	
	public static AgentProperties create(AgentType type, EntityProperties defaults, String configString) {
		Map<String, String> props = Maps.newHashMap(checkNotNull(defaults).properties);
		props.putAll(parseConfigString(configString));
		return new AgentProperties(type, props);
	}

	public AgentType getAgentType() {
		return type;
	}

	@Override
	public String toString() {
		return type + " " + super.toString();
	}

}
