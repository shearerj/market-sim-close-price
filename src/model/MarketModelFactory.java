package model;

import java.util.Map;

import utils.RandPlus;
import data.AgentProperties;
import data.ModelProperties;

public class MarketModelFactory {

	protected final RandPlus rand;
	protected final Map<AgentProperties, Integer> props;
	protected int nextID;

	public MarketModelFactory(MarketModel2 model, Map<AgentProperties, Integer> props, int initialID, RandPlus rand) {
		this.rand = rand;
		this.props = props;
		this.nextID = initialID;
	}

	protected MarketModel2 createModel(ModelProperties props) {
		switch (props.getModelType()) {
		// TODO Other model types
		default:
			return null;
		}
	}
	
	public final int nextID() {
		return nextID;
	}

}
