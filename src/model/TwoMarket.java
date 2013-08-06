package model;

import java.util.Map;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import entity.LAAgent;
import entity.market.CDAMarket;
import event.TimeStamp;

/**
 * TWOMARKET
 * 
 * Class implementing the two-market model of latency arbitrage.
 * 
 * Configurations in the Two-Market Model specify what types of strategies are allowed. Possible
 * values for the configuration include:
 * 
 * "LA:<strategy_string>" "DUMMY"
 * 
 * @author ewah
 */
public class TwoMarket extends MarketModel {

	public TwoMarket(int modelID, FundamentalValue fundamental,
			Map<AgentProperties, Integer> agentProps,
			EntityProperties modelProps, JsonObject playerConfig, RandPlus rand) {
		super(modelID, fundamental, agentProps, modelProps, playerConfig, rand);
	}

	@Override
	protected void setupMarkets(EntityProperties modelProps) {
		markets.add(new CDAMarket(1, this, this.nextIPID()));
		markets.add(new CDAMarket(2, this, this.nextIPID()));
	}

	@Override
	protected void setupAgents(EntityProperties modelProps,
			Map<AgentProperties, Integer> agentProps) {
		super.setupAgents(modelProps, agentProps);
		// Look at model parameters and set up LA
		int numla = modelProps.getAsInt("numla", 0);
		for (int i = 0; i < numla; i++) {
			TimeStamp latency = new TimeStamp(modelProps.getAsInt("lat"
					+ (i + 1), 100));
			// FIXME should pull alpha, and ticksize appropriately, instead of using the values I
			// just inserted...
			new LAAgent(this.agentIDgen.next(), this, 0.0001, latency,
					new RandPlus(rand.nextLong()), 1000);
		}
	}

}
