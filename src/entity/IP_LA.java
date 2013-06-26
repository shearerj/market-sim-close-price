package entity;

import event.*;
import systemmanager.*;


/**
 * Specific Information Processor for a single market, 
 * used by HFT agents
 * 
 * @author ewah
 */
public class IP_LA extends IP_Single_Market {
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public IP_LA(int ID, SystemData d, Log l, int marketID) {
		super(ID, d, l, marketID);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new String("IP_LA number " + this.getID() + ", model number " 
				+ data.getMarket(marketID).getModelID());
	}
}