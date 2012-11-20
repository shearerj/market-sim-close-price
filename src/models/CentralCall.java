package models;

import systemmanager.*;

/**
 * CENTRALCALL
 * 
 * Class implementing the centralized (unfragmented) call market. By default,
 * the clearing frequency is set to the NBBO update latency, unless otherwise
 * specified.
 * 
 * Type in the Central Call market specifies the clearing frequency. Possible
 * values for the model type include: "NBBO", "CONST[clear_freq]".
 * 
 * @author ewah
 */
public class CentralCall extends MarketModel {

	public CentralCall(ObjectProperties p, SystemData d) {
		super(p, d);
		
		String type = p.get(Consts.MODEL_TYPE_KEY);
		if (!type.equals(Consts.MODEL_TYPE_NONE) && !type.equals("0")) {
			ObjectProperties mktProperties = Consts.getProperties("CALL");
			// Set clearing frequency to be NBBO latency or a constant
			if (type.equals("NBBO")) {
				mktProperties.put("clearFreq", data.nbboLatency.toString());
			} else if (type.contains("CONST")){
				// Add substring immediately after "CONST"
				mktProperties.put("clearFreq", type.substring(5));
			}
			addMarketPropertyPair("CALL", mktProperties);
		}
	}
	
	@Override
	public void setAgentPermissions() {
		permitAllSMAgents();
	}
}
