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
	
	public DummyMarketModel(int modelID) {
		super(modelID, new DummyFundamental(100000), 
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
