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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new String("IP_SM number " + this.getID() + ", model number " 
				+ data.getMarket(marketID)); // not number though
	}
}