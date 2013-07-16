package entity;

import event.TimeStamp;


/**
 * Specific Information Processor for a single market, 
 * used by HFT agents
 * 
 * @author cnris
 */
// TODO Somewhere this needs to schedule it's agent's strategy
public class LAIP extends SMIP {
	
	protected final LAAgent laagent;
	
	/**
	 * Constructor
	 * @param ID
	 * @param d
	 */
	public LAIP(int ID, TimeStamp latency, Market mkt, LAAgent laagent) {
		super(ID, latency, mkt);
		this.laagent = laagent;
	}
	
	public LAAgent getLAAgent() {
		return this.laagent;
	}
	
	public String toString() {
		return super.toString() + ", LA: " + laagent;
	}
}