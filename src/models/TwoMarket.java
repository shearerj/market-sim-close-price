package models;

/**
 * TWOMARKET
 * 
 * Class implementing the two-market model of latency arbitrage.
 * 
 * @author ewah
 */
public class TwoMarket extends MarketModel {

	public TwoMarket() {
		super();
		this.addMarketPropertyPair("CDA");
		this.addMarketPropertyPair("CDA");
	}
}
