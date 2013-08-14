package entity.market;

import model.MarketModel;
import data.MarketProperties;

public class MarketFactory {

	protected final MarketModel model;

	public MarketFactory(MarketModel model) {
		this.model = model;
	}

	public Market createMarket(MarketProperties props) {
		switch (props.getMarketType()) {
		case CDA:
			return new CDAMarket(model, props);
		case CALL:
			return new CallMarket(model, props);
		default:
			throw new IllegalArgumentException("Can't create MarketType: "
					+ props.getMarketType());
		}
	}

}
