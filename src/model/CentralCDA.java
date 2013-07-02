package model;

import java.util.Map;

import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import data.ObjectProperties;
import entity.CDAMarket;

/**
 * CENTRALCDA
 * 
 * Class implementing the centralized (unfragmented) CDA market. Uses default
 * properties as given in Consts class.
 * 
 * @author ewah
 */
public class CentralCDA extends MarketModel {
	
	public CentralCDA(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			ObjectProperties modelProps, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, rand);
	}
	
	@Override
	public String getConfig() {
		return "";
	}

	@Override
	protected void setupMarkets(ObjectProperties modelProps) {
		markets.add(new CDAMarket(1, this));
	}
	
}
