package entity;

import java.util.Iterator;
import java.util.List;

import data.ObjectProperties;

import model.MarketModel;

import event.TimeStamp;

import systemmanager.Consts.AgentType;
import utils.RandPlus;

public class BackgroundAgentFactory implements Iterator<BackgroundAgent> {

	protected final RandPlus rand;
	protected final List<Market> markets;
	protected final MarketModel model;
	protected int id;

	public BackgroundAgentFactory(MarketModel model, int initialID, RandPlus rand) {
		this.rand = rand;
		this.model = model;
		this.id = initialID;
		this.markets = model.getMarkets();
	}
	
	public BackgroundAgentFactory(MarketModel model, RandPlus rand) {
		this(model, 0, rand);
	}

	protected BackgroundAgent createAgent(AgentType type, ObjectProperties props) {
		switch (type) {
		case ZI:
			return new ZIAgent(id++, new TimeStamp(0), model, randomMarket(),
					new RandPlus(rand.nextLong()), props);
		default:
			return null;
		}
	}

	protected final Market randomMarket() {
		return markets.get(rand.nextInt(markets.size()));
	}
	
	public final int nextID() {
		return id;
	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public BackgroundAgent next() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove not supported");
	}

}
