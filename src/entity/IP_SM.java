package entity;

import systemmanager.*;
import data.SystemData;


/**
 * Specific Information Processor for a single market, 
 * used by Single Market agents
 * 
 * @author ewah
 */
public class IP_SM extends IP_Single_Market {
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public IP_SM(int ID, SystemData d, int marketID) {
		super(ID, d, marketID);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new String("IP_SM number " + this.getID() + ", model number " 
				+ data.getMarket(marketID).getModelID());
	}
}