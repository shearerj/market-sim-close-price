package model;

import java.util.Collections;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.DummyFundamental;
import data.EntityProperties;
import data.FundamentalValue;
import data.MarketProperties;
import entity.agent.Agent;
import entity.market.Market;

public class MockMarketModel extends MarketModel {

	private static final long serialVersionUID = 1L;

	public MockMarketModel(FundamentalValue fundamental) {
		super(fundamental, new EntityProperties(),
				Collections.<MarketProperties> emptySet(),
				Collections.<AgentProperties> emptySet(), new JsonObject(),
				new RandPlus());
	}
	
	public MockMarketModel() {
		this(new DummyFundamental(100000));
	}
	
	public void addMarket(Market market) {
		markets.add(market);
	}

	public void addAgent(Agent agent) {
		agents.add(agent);
	}

}
