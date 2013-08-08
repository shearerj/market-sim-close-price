package model;

import java.util.Map;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import data.Keys;
import entity.market.CallMarket;
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
	protected void setupMarkets(EntityProperties modelProps) {
		double pricingPolicy = modelProps.getAsDouble(
				Keys.PRICING_POLICY, 0.5d);
		TimeStamp clearFreq = new TimeStamp(modelProps.getAsLong(
				Keys.CLEAR_FREQ, 1000));
		TimeStamp latency = new TimeStamp(modelProps.getAsLong(Keys.MARKET_LATENCY, -1));
		int tickSize = modelProps.getAsInt(Keys.TICK_SIZE, 1);
		markets.add(new CallMarket(1, this, pricingPolicy, clearFreq, latency, tickSize));
	}

}
