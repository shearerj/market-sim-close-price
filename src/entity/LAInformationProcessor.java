package entity;

import systemmanager.*;
import data.SystemData;


/**
 * Specific Information Processor for a single market, 
 * used by HFT agents
 * 
 * @author ewah
 */
public class LAInformationProcessor extends IP_Single_Market {
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public LAInformationProcessor(int ID, SystemData d, int marketID) {
		super(ID, d, marketID);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new String("IP_LA number " + this.getID() + ", model number " 
				+ data.getMarket(marketID).getModelID());
	}
}