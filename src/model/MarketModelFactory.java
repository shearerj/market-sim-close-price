package model;

import generators.Generator;
import generators.IDGenerator;

import java.util.Map;

import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import data.ModelProperties;

public class MarketModelFactory {

	protected final RandPlus rand;
	protected final Map<AgentProperties, Integer> agentProps;
	protected final Generator<Integer> modelIDs;
	protected final FundamentalValue fundamental;

	public MarketModelFactory(Map<AgentProperties, Integer> props, FundamentalValue fundamental, RandPlus rand) {
		this.rand = rand;
		this.agentProps = props;
		this.fundamental = fundamental;
		this.modelIDs = new IDGenerator();
	}

	public MarketModel createModel(ModelProperties modelProps) {
		switch (modelProps.getModelType()) {
		case CENTRALCDA:
			return new CentralCDA(modelIDs.next(), fundamental, agentProps, modelProps, new RandPlus(rand.nextLong()));
		case CENTRALCALL:
			return null; // FIXME change
		case TWOMARKET:
			return null; // FIXME change
		default:
			return null;
		}
	}

}
