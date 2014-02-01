package data;

import static com.google.common.base.Preconditions.checkNotNull;
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
	
	public MarketProperties(MarketType type) {
		super();
		this.type = checkNotNull(type, "Market Type");
	}

	public MarketProperties(MarketProperties copy) {
		super(copy);
		this.type = copy.type;
	}

	public MarketProperties(MarketType type, String config) {
		super(config);
		this.type = type;
	}
	
	public MarketProperties(MarketType type, EntityProperties def, String config) {
		super(def, config);
		this.type = type;
	}
	
	public MarketType getMarketType() {
		return type;
	}
	
	@Override
	public String toString() {
		return type + " " + super.toString();
	}

}
