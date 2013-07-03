package model;

import java.util.Map;

import systemmanager.Consts;
import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import data.ObjectProperties;
import data.SystemData;
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

	public CentralCDA(int modelID, ObjectProperties p, SystemData d, int sipID) {
		super(modelID, p, d, sipID);
		
		config = p.getAsString(Consts.MODEL_CONFIG_KEY);
		if (!config.equals(Consts.MODEL_CONFIG_NONE) && !config.equals("0")) {
			addMarketPropertyPair(Consts.MarketType.CDA);
		}
	}
	
	@Override
	public String getConfig() {
		return "";
	}

	@Override
	protected void setupMarkets(ObjectProperties modelProps) {
		markets.add(new CDAMarket(1, this));
	}

	@Override
	protected void setupModelAgents(ObjectProperties modelProps) {
		// Do nothing
	}
}
