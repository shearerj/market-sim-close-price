package entity;

import model.MarketModel;

public class MockMarket extends Market {
	
	public MockMarket(int marketID, MarketModel model) {
		super(marketID, model);
	}
	
	public MockMarket(MarketModel model) {
		this(0, model);
	}

}
