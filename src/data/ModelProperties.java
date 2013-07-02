package data;

import systemmanager.Consts.ModelType;

public class ModelProperties extends ObjectProperties {

	protected final ModelType type;
	
	public ModelProperties(ModelType type) {
		super();
		this.type = type;
	}

	public ModelProperties(ModelProperties copy) {
		super(copy);
		this.type = copy.type;
	}

	public ModelProperties(ModelType type, String config) {
		super(config);
		this.type = type;
	}
	
	public ModelProperties(ModelType type, ObjectProperties def, String config) {
		super(def, config);
		this.type = type;
	}
	
	public ModelType getModelType() {
		return type;
	}

	@Override
	public void addConfig(String config) {
		// TODO change config so this makes more sense
		int split = config.indexOf(':');
		super.addConfig(config.substring(split + 1));
		put("LA", "LA".equals(config.substring(0, split)));
	}

}
