package entity.market;

import data.MarketProperties;
import entity.infoproc.SIP;

public class MarketFactory {

	protected final SIP sip;

	public MarketFactory(SIP sip) {
		this.sip = sip;
	}

	public Market createMarket(MarketProperties props) {
		switch (props.getMarketType()) {
		case CDA:
			return new CDAMarket(sip, props);
		case CALL:
			return new CallMarket(sip, props);
		default:
			throw new IllegalArgumentException("Can't create MarketType: "
					+ props.getMarketType());
		}
	}

}
