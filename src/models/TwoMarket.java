package models;

import systemmanager.*;

/**
 * TWOMARKET
 * 
 * Class implementing the two-market model of latency arbitrage.
 * 
 * @author ewah
 */
public class TwoMarket extends MarketModel {

	public TwoMarket(ObjectProperties p) {
		super(p);
		// Add two CDA markets with default settings
		this.addMarketPropertyPair("CDA");
		this.addMarketPropertyPair("CDA");
	}
}
