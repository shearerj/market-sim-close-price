package data;

import systemmanager.Consts.ModelType;

public class ModelProperties extends EntityProperties {
	
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
	
	public ModelProperties(ModelType type, EntityProperties def, String config) {
		super(def, config);
		this.type = type;
	}
	
	public ModelType getModelType() {
		return type;
	}

}
