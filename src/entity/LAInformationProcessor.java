package entity;

import event.TimeStamp;


/**
 * Specific Information Processor for a single market, 
 * used by HFT agents
 * 
 * @author ewah
 */
public class LAInformationProcessor extends IP {
	
	private LAAgent laagent;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public LAInformationProcessor(int ID, int marketID, TimeStamp latency, Market mkt, LAAgent laagent) {
		super(ID, marketID, latency, mkt);
		this.laagent = laagent;
	}
	
	public LAAgent getLAAgent() {
		return this.laagent;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new String("IP_LA number " + this.getID() + ", model number " 
				+ data.getMarket(marketID)); // not number though!
	}
}