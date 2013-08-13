package data;

import systemmanager.Consts.MarketType;

public class MarketProperties extends EntityProperties {
	
	protected final MarketType type;
	
	public MarketProperties(MarketType type) {
		super();
		this.type = type;
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
	
	public MarketType getModelType() {
		return type;
	}

}
