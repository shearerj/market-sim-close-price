package models;

import systemmanager.*;

/**
 * CENTRALCALL
 * 
 * Class implementing the centralized (unfragmented) call market. By default,
 * the clearing frequency is set to the NBBO update latency, unless otherwise
 * specified.
 * 
 * @author ewah
 */
public class CentralCall extends MarketModel {

	public CentralCall() {
		super();
		this.addMarketPropertyPair("CALL");
	}
	
	/**
	 * Create centralized call market model with given clearing frequency.
	 * @param clearFreq
	 */
	public CentralCall(int clearFreq) {
		super();
		EntityProperties mktProperties = Consts.getProperties("CALL");
		mktProperties.put("clearFreq", ((Integer) clearFreq).toString());
		this.addMarketPropertyPair("CALL", mktProperties);
	}
}
