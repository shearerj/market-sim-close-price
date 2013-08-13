package model;

import java.util.Map;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import data.Keys;
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
	
	private static final long serialVersionUID = -6945232235329268406L;

	public CentralCDA(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			EntityProperties modelProps, JsonObject playerConfig, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, playerConfig, rand);
	}

	@Override
	protected void setupMarkets(EntityProperties modelProps) {
		TimeStamp latency = new TimeStamp(modelProps.getAsLong(Keys.MARKET_LATENCY, -1));
		markets.add(new CDAMarket(1, this, latency));
	}

}
