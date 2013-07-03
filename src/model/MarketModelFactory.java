package model;

import generators.Generator;
import generators.IDGenerator;

import java.util.Map;

import com.google.gson.JsonObject;

import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import data.ModelProperties;

public class MarketModelFactory {

	protected final RandPlus rand;
	protected final Map<AgentProperties, Integer> agentProps;
	protected final Generator<Integer> modelIDs;
	protected final FundamentalValue fundamental;
	protected final JsonObject playerConfig;

	public MarketModelFactory(Map<AgentProperties, Integer> props, JsonObject playerConfig, FundamentalValue fundamental, RandPlus rand) {
		this.rand = rand;
		this.agentProps = props;
		this.fundamental = fundamental;
		this.playerConfig = playerConfig;
		this.modelIDs = new IDGenerator();
	}

	public MarketModel createModel(ModelProperties modelProps) {
		switch (modelProps.getModelType()) {
		case CENTRALCDA:
			return new CentralCDA(modelIDs.next(), fundamental, agentProps, modelProps, playerConfig, new RandPlus(rand.nextLong()));
		case CENTRALCALL:
			return null; // FIXME change
		case TWOMARKET:
			return null; // FIXME change
		default:
			return null;
		}
	}

}
