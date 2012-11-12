package models;

import systemmanager.*;

/**
 * CENTRALCDA
 * 
 * Class implementing the centralized (unfragmented) CDA market.
 * 
 * @author ewah
 */
public class CentralCDA extends MarketModel {

	public CentralCDA(ObjectProperties p) {
		super(p);
		this.addMarketPropertyPair("CDA");
	}
}
