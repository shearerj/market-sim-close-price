package model;

import java.util.Map;

import utils.RandPlus;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.EntityProperties;
import data.FundamentalValue;
import data.Keys;
import entity.agent.LAAgent;
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
		TimeStamp latency = new TimeStamp(modelProps.getAsLong(Keys.MARKET_LATENCY, -1));
		markets.add(new CDAMarket(1, this, latency));
		markets.add(new CDAMarket(2, this, latency));
	}

	@Override
	protected void setupAgents(EntityProperties modelProps,
			Map<AgentProperties, Integer> agentProps) {
		super.setupAgents(modelProps, agentProps);
		// Look at model parameters and set up LA
		int numla = modelProps.getAsInt(Keys.NUM_LA, 0);
		double alpha = modelProps.getAsDouble(Keys.ALPHA, 0.001);
		int tickSize = modelProps.getAsInt(Keys.TICK_SIZE, 1);
		for (int i = 0; i < numla; i++) {
			TimeStamp latency = new TimeStamp(modelProps.getAsInt(Keys.LA_LATENCY
					+ (i + 1), -1));
			
			new LAAgent(this.agentIDgen.next(), this, alpha, latency,
					new RandPlus(rand.nextLong()), tickSize);
		}
	}

}
