package model;

import java.util.HashMap;
import java.util.Map;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import entity.Agent;
import entity.Market;

public class DummyMarketModel extends MarketModel {

	public DummyMarketModel(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			EntityProperties modelProps, JsonObject playerConfig, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, playerConfig, rand);
		// TODO Auto-generated constructor stub
	}
	
	public DummyMarketModel(int modelID) {
		this(modelID, new FundamentalValue(0, 0, 0, new RandPlus()), 
				new HashMap<AgentProperties,Integer>(),
				new EntityProperties(), new JsonObject(), 
				new RandPlus());
	}
	
	public DummyMarketModel() {
		this(0);
	}

	@Override
	protected void setupMarkets(EntityProperties modelProps) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	public void addMarket(Market market) {
		this.markets.add(market);
	}

	public void addAgent(Agent agent) {
		this.agents.add(agent);
	}
}
