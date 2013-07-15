package model;

import java.util.Collection;
import java.util.Map;

import com.google.gson.JsonObject;

import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import data.EntityProperties;
import entity.CallMarket;
import entity.LAIP;
import event.TimeStamp;

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
			Map<AgentProperties, Integer> agentProps, EntityProperties modelProps, 
			JsonObject playerConfig, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, playerConfig, rand);
	}

	@Override
	public String getConfig() {
		return config;
	}

	@Override
	protected void setupMarkets(EntityProperties modelProps) {
		// FIXME These default values are probably not correct.
		float pricingPolicy = modelProps.getAsFloat(
				CallMarket.PRICING_POLICY_KEY, 0.5f);
		TimeStamp clearFreq = new TimeStamp(modelProps.getAsLong(
				CallMarket.CLEAR_FREQ_KEY, 100));
		markets.add(new CallMarket(1, this, pricingPolicy, clearFreq)); // not sure on numbering...
	}

}
