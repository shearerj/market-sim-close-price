package edu.umich.srg.marketsim;

import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Uniform;
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
import edu.umich.srg.marketsim.market.Market.AgentInfo;
import edu.umich.srg.util.PositionalSeed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

  /**
   * Run the market-sim simulation with a given simspec, log, and observation number. This is the
   * main entry point for executing the simulator from java.
   * 
   * @param simNum This is the unique observation number for one simulation, not the number of
   *        observations to produce. This method returns a single observation.
   */
  public static Observation simulate(SimSpec spec, int simNum) {
    Spec configuration = spec.configuration.withDefault(Keys.DEFAULT_KEYS);
    long seed = PositionalSeed.with(configuration.get(RandomSeed.class)).getSeed(simNum);
    Random rand = new Random(seed);

    Fundamental fundamental = GaussianMeanReverting.create(new Random(rand.nextLong()),
        configuration.get(FundamentalMean.class), configuration.get(FundamentalMeanReversion.class),
        configuration.get(FundamentalShockVar.class),
        configuration.get(FundamentalShockProb.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, new Random(rand.nextLong()));

    List<Market> markets = addMarkets(sim, spec.configuration.get(Markets.class), configuration);
    List<PlayerInfo> playerInfo =
        addPlayers(sim, fundamental, spec.assignment, markets, configuration, rand.nextLong());

    sim.initialize();
    sim.executeUntil(TimeStamp.of(configuration.get(SimLength.class)));

    // Update player observations
    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    for (PlayerInfo info : playerInfo) {
      info.payoff = payoffs.get(info.agent).getProfit();
      info.features = info.agent.getFeatures();
    }

    return new Observation() {

      @Override
      public Collection<? extends Player> getPlayers() {
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

  /**
   * In order for agent seeds to be identical independent of the order the agents were added in,
   * each pair of role and strategy get's it's own random seed that's based off of the hash of the
   * role and strategy. Then, to make sure that agent's get added to the simulation in a
   * deterministic order, we put every player into a list and sort by a random double (with role,
   * strategy, and number as tie breakers). That way the order of players is random, but entirely
   * determined by the random seed. Importantly, by generating the order this way, it keeps the
   * order roughly the same between similar runs, e.g. if you add a new agent, it will go somewhere
   * in the ordering, but the relative orderings of everything else will remain the same. This is
   * critical, because ties in arrivals are critically dependent on the order in which the agents
   * arrived, and slightly different arrivals produce drastically different results. Since the
   * initial order is the order they're defined in here, the initial order is important, and can't
   * be dependent on the simulation spec. This has a side effect of making player output also
   * deterministic based off of random seed, meaning the output of identical specs should be closer
   * to identical.
   */
  private static List<PlayerInfo> addPlayers(MarketSimulator sim, Fundamental fundamental,
      Multiset<RoleStrat> assignment, Collection<Market> markets, Spec configuration,
      long baseSeed) {
    PositionalSeed seed = PositionalSeed.with(baseSeed);
    Uniform<Market> marketSelection = Uniform.over(markets);

    ArrayList<PlayerOrder> players = new ArrayList<>();
    for (Entry<RoleStrat> roleStratCounts : assignment.entrySet()) {
      RoleStrat roleStrat = roleStratCounts.getElement();
      String strategy = roleStrat.getStrategy();
      AgentCreator creator = EntityBuilder.getAgentCreator(getType(strategy));
      Spec agentSpec = getSpec(strategy).withDefault(configuration);
      Random rand = new Random(seed.getSeed(roleStratCounts.getElement().hashCode()));

      for (int i = 0; i < roleStratCounts.getCount(); ++i) {
        Agent agent = creator.createAgent(sim, fundamental, markets, marketSelection.sample(rand),
            agentSpec, new Random(rand.nextLong()));
        players.add(
            new PlayerOrder(rand.nextDouble(), roleStrat, i, new PlayerInfo(roleStrat, agent)));
      }
    }
    Collections.sort(players);

    ImmutableList.Builder<PlayerInfo> playerInfoBuilder = ImmutableList.builder();
    for (PlayerOrder player : players) {
      playerInfoBuilder.add(player.player);
      sim.addAgent(player.player.agent);
    }
    return playerInfoBuilder.build();
  }

  private static class PlayerOrder implements Comparable<PlayerOrder> {

    private final double order;
    private final RoleStrat group;
    private final int num;

    private final PlayerInfo player;

    private PlayerOrder(double order, RoleStrat group, int num, PlayerInfo player) {
      this.order = order;
      this.group = group;
      this.num = num;
      this.player = player;
    }

    @Override
    public int compareTo(PlayerOrder that) {
      return ComparisonChain.start().compare(this.order, that.order).compare(this.group, that.group)
          .compare(this.num, that.num).result();
    }

  }

  private static String getType(String strategy) {
    int index = strategy.indexOf(':');
    return (index < 0 ? strategy : strategy.substring(0, index)).toLowerCase();
  }

  private static Spec getSpec(String strategy) {
    int index = strategy.indexOf(':');
    if (index < 0) {
      return Spec.empty();
    } else {
      return Spec.fromPairs(keyPrefix, keyCaseFormat,
          specSplitter.split(strategy.substring(index + 1)));
    }
  }

  private static class PlayerInfo implements Player {

    private final String role;
    private final String strategy;
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
