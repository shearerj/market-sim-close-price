package entity.market;

import java.util.Random;

import systemmanager.Keys;
import data.MarketProperties;
import entity.infoproc.SIP;

public class MarketFactory {

	protected final SIP sip;
	protected final Random rand;

	public MarketFactory(SIP sip, Random rand) {
		this.sip = sip;
		this.rand = rand;
	}

	public Market createMarket(MarketProperties props) {
		switch (props.getMarketType()) {
		case CDA:
			return new CDAMarket(sip, new Random(rand.nextLong()), props);
		case CALL:
			if (props.getAsInt(Keys.MARKET_LATENCY) == -1) {
				return new CDAMarket(sip, new Random(rand.nextLong()), props);
			}
			return new CallMarket(sip, new Random(rand.nextLong()), props);
		default:
			throw new IllegalArgumentException("Can't create MarketType: " + props.getMarketType());
		}
	}

}
