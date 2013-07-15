package model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import com.google.gson.JsonObject;

import utils.RandPlus;
import data.AgentProperties;
import data.FundamentalValue;
import data.EntityProperties;
import entity.CDAMarket;
import entity.LAAgent;
import entity.LAInformationProcessor;
import event.TimeStamp;
import entity.Market;

/**
 * TWOMARKET
 * 
 * Class implementing the two-market model of latency arbitrage.
 * 
 * Configurations in the Two-Market Model specify what types of strategies are
 * allowed. Possible values for the configuration include:
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
	@Deprecated
	public String getConfig() {
		return config;
	}

	@Deprecated
	public Market getAlternateMarket(Market mainMarket) {
		if (!markets.contains(mainMarket)) return null; // XXX Shouldn't
														// happen...
		Collection<Market> alternateMarkets = new HashSet<Market>(markets);
		alternateMarkets.remove(mainMarket);
		// XXX This should only work if markets has two unique markets...
		return alternateMarkets.iterator().next();
	}

	@Override
	protected void setupMarkets(EntityProperties modelProps) {
		markets.add(new CDAMarket(1, this, this.getipIDgen()));
		markets.add(new CDAMarket(2, this, this.getipIDgen()));
	}

	@Override
	protected void setupAgents(EntityProperties modelProps, // these are all of
															// the key value
															// pairs from sim
															// spec.json
			Map<AgentProperties, Integer> agentProps) {
		super.setupAgents(modelProps, agentProps);
		// Look at model parameters and set up LA
		int numla = modelProps.getAsInt("numla", 0);
		for (int i = 0; i < numla; i++) {
			TimeStamp latency = new TimeStamp(modelProps.getAsInt("lat"
					+ (i + 1), 100));
			// TODO entity properties should be set,defaults currently not
			// handled
			// FIXME should pull sleeptime, sleepvar, alpha, and ticksize
			// appropriately, instead of using the values I just inserted...
			new LAAgent(this.agentIDgen.next(), this, 0, 0, 0.0001, latency,
					new RandPlus(rand.nextLong()), 1000);
		}
	}

}
