package model;

import java.util.Map;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import entity.market.CDAMarket;
import event.TimeStamp;

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
			EntityProperties modelProps, JsonObject playerConfig, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, playerConfig, rand);
	}

	@Override
	protected void setupMarkets(EntityProperties modelProps) {
		// FIXME Get latency from settings
		markets.add(new CDAMarket(1, this, TimeStamp.IMMEDIATE));
	}

}
