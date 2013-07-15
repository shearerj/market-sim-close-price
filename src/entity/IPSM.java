package entity;

import event.TimeStamp;


/**
 * Specific Information Processor for a single market, 
 * used by Single Market agents
 * 
 * @author ewah
 */
public class IPSM extends IP {
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public IPSM(int ID, int marketID, TimeStamp latency, Market mkt) {
		super(ID, marketID, latency, mkt);
	}
	
	public String toString() {
		// FIXME Market ID; don't need to call new string constructor
//		return new String("IP_SM number " + this.getID() + ", model number " 
//				+ data.getMarket(marketID)); // not number though
		return "";
	}
}