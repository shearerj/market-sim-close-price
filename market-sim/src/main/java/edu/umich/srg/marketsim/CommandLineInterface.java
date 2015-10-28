package edu.umich.srg.marketsim;

import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.egtaonline.Log;
import edu.umich.srg.egtaonline.Observation;
import edu.umich.srg.egtaonline.Observation.Player;
import edu.umich.srg.egtaonline.Runner;
import edu.umich.srg.egtaonline.SimSpec;
import edu.umich.srg.egtaonline.SimSpec.RoleStrat;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.EntityBuilder.AgentCreator;
import edu.umich.srg.marketsim.EntityBuilder.MarketCreator;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalShockProb;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.Markets;
import edu.umich.srg.marketsim.Keys.RandomSeed;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.Market;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CommandLineInterface {

  private static final String keyPrefix = "edu.umich.srg.marketsim.Keys$";
  private static final CaseFormat keyCaseFormat = CaseFormat.LOWER_CAMEL;
  private static final Splitter specSplitter = Splitter.on('_').omitEmptyStrings();

  public static void main(String[] args) throws IOException {
    Runner.run(CommandLineInterface::simulate, args, keyPrefix, keyCaseFormat);
  }

  public static Observation simulate(SimSpec spec, Log log, long obsNum) {
    Spec configuration = spec.configuration.withDefault(Keys.DEFAULT_KEYS);
    Random rand = new Random(configuration.get(RandomSeed.class) + obsNum);

    Fundamental fundamental = GaussianMeanReverting.create(new Random(rand.nextLong()),
        configuration.get(FundamentalMean.class), configuration.get(FundamentalMeanReversion.class),
        configuration.get(FundamentalShockVar.class),
        configuration.get(FundamentalShockProb.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, log, new Random(rand.nextLong()));
    log.setPrefix(l -> String.format("%6d | ", sim.getCurrentTime().get()));

    List<Market> markets = addMarkets(sim, spec.configuration.get(Markets.class), configuration);
    List<PlayerInfo> playerInfo = addPlayers(sim, fundamental, spec.assignment, markets,
        configuration, new Random(rand.nextLong()));

    sim.initialize();
    sim.executeUntil(TimeStamp.of(configuration.get(SimLength.class)));

    // Update player observations
    Map<Agent, Double> payoffs = sim.getAgentPayoffs();
    for (PlayerInfo info : playerInfo) {
      info.payoff = payoffs.get(info.agent);
      info.features = info.agent.getFeatures();
    }

    return new Observation() {

      @Override
      public Iterable<? extends Player> getPlayers() {
        return playerInfo;
      }

      @Override
      public JsonObject getFeatures() {
        return sim.computeFeatures();
      }

    };
  }

  private static List<Market> addMarkets(MarketSimulator sim, Iterable<String> marketSpecs,
      Spec configuration) {
    ImmutableList.Builder<Market> marketBuilder = ImmutableList.builder();
    for (String stringSpec : marketSpecs) {
      MarketCreator creator = EntityBuilder.getMarketCreator(getType(stringSpec));
      Spec marketSpec = getSpec(stringSpec).withDefault(configuration);

      Market market = creator.createMarket(sim, marketSpec);
      sim.addMarket(market);
      marketBuilder.add(market);
    }
    return marketBuilder.build();
  }

  private static List<PlayerInfo> addPlayers(MarketSimulator sim, Fundamental fundamental,
      Multiset<RoleStrat> assignment, Collection<Market> markets, Spec configuration, Random rand) {
    Random marketRand = new Random(rand.nextLong());
    Uniform<Market> marketSelection = Uniform.over(markets);

    ImmutableList.Builder<PlayerInfo> playerInfoBuilder = ImmutableList.builder();
    for (Entry<RoleStrat> roleStratCounts : assignment.entrySet()) {
      String strategy = roleStratCounts.getElement().getStrategy();
      AgentCreator creator = EntityBuilder.getAgentCreator(getType(strategy));
      Spec agentSpec = getSpec(strategy).withDefault(configuration);

      for (int i = 0; i < roleStratCounts.getCount(); ++i) {
        Agent agent = creator.createAgent(sim, fundamental, markets,
            marketSelection.sample(marketRand), agentSpec, rand);
        sim.addAgent(agent);
        playerInfoBuilder.add(new PlayerInfo(roleStratCounts.getElement(), agent));
      }
    }
    return playerInfoBuilder.build();
  }

  private static String getType(String strategy) {
    int index = strategy.indexOf(':');
    return (index < 0 ? strategy : strategy.substring(0, index)).toLowerCase();
  }

  private static Spec getSpec(String strategy) {
    int index = strategy.indexOf(':');
    if (index < 0)
      return Spec.empty();
    return Spec.fromPairs(keyPrefix, keyCaseFormat,
        specSplitter.split(strategy.substring(index + 1)));
  }

  private static class PlayerInfo implements Player {

    private final String role, strategy;
    private final Agent agent;
    private double payoff;
    private JsonObject features;

    private PlayerInfo(RoleStrat roleAndStrategy, Agent agent) {
      this.role = roleAndStrategy.getRole();
      this.strategy = roleAndStrategy.getStrategy();
      this.agent = agent;
      this.payoff = 0;
      this.features = null;
    }

    @Override
    public String getRole() {
      return role;
    }

    @Override
    public String getStrategy() {
      return strategy;
    }

    @Override
    public double getPayoff() {
      return payoff;
    }

    @Override
    public JsonObject getFeatures() {
      return features;
    }

    @Override
    public String toString() {
      return role + ": " + strategy + " (" + payoff + ") " + features;
    }

  }

}
