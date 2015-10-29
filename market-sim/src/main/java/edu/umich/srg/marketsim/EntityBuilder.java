package edu.umich.srg.marketsim;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.NoOpAgent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.agent.ShockAgent;
import edu.umich.srg.marketsim.agent.ZILAgent;
import edu.umich.srg.marketsim.agent.ZIRAgent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CDAMarket;
import edu.umich.srg.marketsim.market.Market;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

class EntityBuilder {

  static AgentCreator getAgentCreator(String name) {
    return checkNotNull(agentNameMap.get(name),
        "\"%s\" is not a defined agent name in EntityBuilder", name);
  }

  static MarketCreator getMarketCreator(String name) {
    return checkNotNull(marketNameMap.get(name),
        "\"%s\" is not a defined market name in EntityBuilder", name);
  }

  private static final Map<String, AgentCreator> agentNameMap =
      ImmutableMap.<String, AgentCreator>builder() //
          .put("noise", NoiseAgent::createFromSpec) // Noise agentas for testings
          .put("noop", NoOpAgent::createFromSpec) // No op agents that do nothing
          .put("zir", ZIRAgent::createFromSpec) // Standard ZI agents with re-entry
          .put("zil", ZILAgent::createFromSpec) // ZI agents that learn from noisy observations
          .put("shock", ShockAgent::createFromSpec) // Shock agent that buys a lot at a random time
          .build();

  private static final Map<String, MarketCreator> marketNameMap =
      ImmutableMap.<String, MarketCreator>builder() //
          .put("cda", CDAMarket::createFromSpec) // CDA Market
          .build();

  static interface AgentCreator {

    Agent createAgent(Sim sim, Fundamental fundamental, Collection<Market> markets, Market market,
        Spec spec, Random rand);

  }

  static interface MarketCreator {

    Market createMarket(Sim sim, Spec spec);

  }

}
