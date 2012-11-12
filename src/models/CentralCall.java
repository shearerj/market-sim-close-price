package models;

import systemmanager.*;

/**
 * CENTRALCALL
 * 
 * Class implementing the centralized (unfragmented) call market. By default,
 * the clearing frequency is set to the NBBO update latency, unless otherwise
 * specified.
 * 
 * Parameters:
 * 	- clearFreq		clearing frequency of the centralized call market
 * 
 * @author ewah
 */
public class CentralCall extends MarketModel {

	public CentralCall(ObjectProperties p) {
		super(p);
		this.addMarketPropertyPair("CALL");
		ObjectProperties mktProperties = Consts.getProperties("CALL");
		mktProperties.put("clearFreq", p.get("clearFreq"));
		this.addMarketPropertyPair("CALL", mktProperties);
	}
}
