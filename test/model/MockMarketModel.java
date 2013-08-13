package model;

import java.util.HashMap;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import entity.agent.Agent;
import entity.market.Market;

public class MockMarketModel extends MarketModel {

	private static final long serialVersionUID = 1L;

	public MockMarketModel(int modelID, FundamentalValue fundamental) {
		super(modelID, fundamental, 
				new HashMap<AgentProperties,Integer>(),
				new EntityProperties(), new JsonObject(), 
				new RandPlus());
	}
	
	public MockMarketModel(int modelID) {
		this(modelID, new DummyFundamental(100000));
	}
	
	public MockMarketModel() {
		this(0);
	}
	
	@Override
	protected void setupMarkets(EntityProperties modelProps) { }
	
	public void addMarket(Market market) {
		markets.add(market);
	}

	public void addAgent(Agent agent) {
		agents.add(agent);
	}

}
