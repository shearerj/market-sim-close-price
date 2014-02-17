package data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.collect.Maps;

import systemmanager.Consts.MarketType;

/**
 * Entity properties bundled with MarketType
 * 
 * @author erik
 * 
 */
/*
 * XXX Thought, potentially instead of market and agent type, this could have a
 * generic enum type
 */
public class MarketProperties extends EntityProperties {
	
	private static final long serialVersionUID = -8634339070031699521L;
	
	protected final MarketType type;
	
	protected MarketProperties(MarketType type, Map<String, String> properties) {
		super(properties);
		this.type = type;
	}
	
	public static MarketProperties empty(MarketType type) {
		return new MarketProperties(type, Maps.<String, String> newHashMap());
	}
	
	public static MarketProperties create(MarketType type, EntityProperties defaults, String configString) {
		Map<String, String> props = Maps.newHashMap(checkNotNull(defaults).properties);
		props.putAll(parseConfigString(configString));
		return new MarketProperties(type, props);
	}
	
	public MarketType getMarketType() {
		return type;
	}
	
	@Override
	public String toString() {
		return type + " " + super.toString();
	}

}
