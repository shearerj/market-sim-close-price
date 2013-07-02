package model;

import java.util.Map;

import data.AgentProperties;
import data.FundamentalValue;
import data.ObjectProperties;
import data.SystemData;
import entity.CallMarket;
import event.TimeStamp;
import systemmanager.*;
import utils.RandPlus;

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

	public CentralCall(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			ObjectProperties modelProps, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, rand);
	}
	
	public CentralCall(int modelID, ObjectProperties p, SystemData d) {
		super(modelID, p, d);
		
		config = p.getAsString(Consts.MODEL_CONFIG_KEY);
		if (!config.equals(Consts.MODEL_CONFIG_NONE) && !config.equals("0")) {
			
			ObjectProperties mktProperties = Consts.getProperties(Consts.MarketType.CALL);
			
			// Set clearing frequency to be NBBO latency or a constant
			if (config.equals("NBBO")) {
				mktProperties.put(CallMarket.CLEAR_FREQ_KEY, data.nbboLatency.toString());
				
			} else if (config.contains("CONST")){
				// Add substring immediately after "CONST"
				mktProperties.put(CallMarket.CLEAR_FREQ_KEY, config.substring(5));
			}
			
			addMarketPropertyPair(Consts.MarketType.CALL, mktProperties);
		}
	}
	
	@Override
	public String getConfig() {
		return config;
	}

	@Override
	protected void setupMarkets(ObjectProperties modelProps) {
		// FIXME These default values are probably not correct.
		float pricingPolicy = modelProps.getAsFloat(CallMarket.PRICING_POLICY_KEY, 0);
		TimeStamp clearFreq = new TimeStamp(modelProps.getAsLong(CallMarket.CLEAR_FREQ_KEY, 1000));
		markets.add(new CallMarket(1, this, pricingPolicy, clearFreq));
	}

}
