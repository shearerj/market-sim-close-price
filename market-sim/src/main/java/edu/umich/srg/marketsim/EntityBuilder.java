package edu.umich.srg.marketsim;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.MarkovAgent;
import edu.umich.srg.marketsim.agent.NoOpAgent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.agent.ShockAgent;
import edu.umich.srg.marketsim.agent.SimpleMarketMaker;
import edu.umich.srg.marketsim.agent.SimpleTrendFollower;
import edu.umich.srg.marketsim.agent.ZirAgent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

final class EntityBuilder {

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
          .put("zir", ZirAgent::createFromSpec) // Standard ZI agents with re-entry
          .put("markov", MarkovAgent::createFromSpec) // ZI agents with markov learning
          .put("smm", SimpleMarketMaker::createFromSpec) // Simple Market Maker
          .put("shock", ShockAgent::createFromSpec) // Shock agent that buys a lot at a random time
          .put("trend", SimpleTrendFollower::createFromSpec) // Simple trend follower
          .build();

  private static final Map<String, MarketCreator> marketNameMap =
      ImmutableMap.<String, MarketCreator>builder() //
          .put("cda", CdaMarket::createFromSpec) // CDA Market
          .build();

  interface AgentCreator {

    Agent createAgent(Sim sim, Fundamental fundamental, Collection<Market> markets, Market market,
        Spec spec, Random rand);

  }

  interface MarketCreator {

    Market createMarket(Sim sim, Spec spec);

  }

}
