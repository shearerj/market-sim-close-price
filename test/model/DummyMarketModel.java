package model;

import java.util.HashMap;
import java.util.Map;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import entity.Agent;
import entity.Market;

public class DummyMarketModel extends MarketModel {

	//
	// Constructors
	//
	public DummyMarketModel(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			EntityProperties modelProps, JsonObject playerConfig, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, playerConfig, rand);
		// TODO Auto-generated constructor stub
	}
	
	public DummyMarketModel(int modelID) {
		this(modelID, new DummyFundamental(100000), 
				new HashMap<AgentProperties,Integer>(),
				new EntityProperties(), new JsonObject(), 
				new RandPlus());
	}
	
	public DummyMarketModel() {
		this(0);
	}

	//
	// Dummy Implemented methods
	//
	@Override
	public String getConfig() {
		return "Dummy Market";
	}
	
	@Override
	protected void setupMarkets(EntityProperties modelProps) {
	}
	
	//
	// Configuration methods
	//
	public void addMarket(Market market) {
		this.markets.add(market);
	}

	public void addAgent(Agent agent) {
		this.agents.add(agent);
	}


}
