package model;

import entity.CallMarket;
import systemmanager.*;

/**
 * CENTRALCALL
 * 
 * Class implementing the centralized (unfragmented) call market. By default,
 * the clearing frequency is set to the NBBO update latency, unless otherwise
 * specified.
 * 
 * Configurations in the Central Call market specifies the clearing frequency.
 * Possible values for the configuration include: "NBBO", "CONST[clear_freq]".
 * 
 * @author ewah
 */
public class CentralCall extends MarketModel {

	public CentralCall(int modelID, ObjectProperties p, SystemData d) {
		super(modelID, p, d);
		
		config = p.get(Consts.MODEL_CONFIG_KEY);
		if (!config.equals(Consts.MODEL_CONFIG_NONE) && !config.equals("0")) {
			
			ObjectProperties mktProperties = Consts.getProperties(Consts.CALL);
			
			// Set clearing frequency to be NBBO latency or a constant
			if (config.equals("NBBO")) {
				mktProperties.put(CallMarket.CLEAR_FREQ_KEY, data.nbboLatency.toString());
				
			} else if (config.contains("CONST")){
				// Add substring immediately after "CONST"
				mktProperties.put(CallMarket.CLEAR_FREQ_KEY, config.substring(5));
			}
			
			addMarketPropertyPair(Consts.CALL, mktProperties);
		}
	}
	
	@Override
	public String getConfig() {
		return config;
	}
}
