package entity;

import java.util.List;

import model.MarketModel;

import data.AgentProperties;
import event.TimeStamp;

import systemmanager.Consts.AgentType;
import utils.RandPlus;

public class BackgroundAgentFactory {

	protected final RandPlus rand;
	protected final List<Market> markets;
	protected final MarketModel model;
	protected int id;

	public BackgroundAgentFactory(RandPlus rand, MarketModel model) {
		this.rand = rand;
		this.model = model;
		this.markets = model.getMarkets();
	}

	protected BackgroundAgent createAgent(AgentType type, AgentProperties props) {
		switch (type) {
		case ZI:
			return new ZIAgent(id++, new TimeStamp(0), model, randomMarket(),
					props.getAsInt("bid_range", 2000), new RandPlus(
							rand.nextLong()));
		default:
			return null;
		}
	}

	protected final Market randomMarket() {
		return markets.get(rand.nextInt(markets.size()));
	}
	
}
