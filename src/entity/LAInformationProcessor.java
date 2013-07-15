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
	
	public String toString() {
		// FIXME System Data
//		return new String("IP_LA number " + this.getID() + ", model number " 
//				+ data.getMarket(marketID)); // not number though!
		return "";
	}
}