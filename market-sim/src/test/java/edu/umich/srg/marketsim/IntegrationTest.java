package edu.umich.srg.marketsim;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.junit.Ignore;
import org.junit.Test;

import edu.umich.srg.egtaonline.Log;
import edu.umich.srg.egtaonline.Log.Level;
import edu.umich.srg.egtaonline.Observation;
import edu.umich.srg.egtaonline.Observation.Player;
import edu.umich.srg.egtaonline.Runner;
import edu.umich.srg.egtaonline.SimSpec;
import edu.umich.srg.egtaonline.SimSpec.RoleStrat;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.egtaonline.spec.Value;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.Markets;
import edu.umich.srg.marketsim.Keys.RandomSeed;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;
import edu.umich.srg.marketsim.testing.NullWriter;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.stream.StreamSupport;

public class IntegrationTest {

  private static final Random rand = new Random();
  private static final Gson gson = new Gson();
  private static final Joiner stratJoiner = Joiner.on('_');
  private static final String keyPrefix = "edu.umich.srg.marketsim.Keys$";
  private static final CaseFormat keyCaseFormat = CaseFormat.LOWER_CAMEL;
  private static final double tol = 1e-8;

  @Test
  public void simpleMinimalTest() {
    int numAgents = 10;
    StringWriter logData = new StringWriter();
    Log log = Log.create(Level.DEBUG, logData, l -> l + ") ");

    MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), log, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    for (int i = 0; i < numAgents; ++i)
      sim.addAgent(new NoiseAgent(sim, cda, Spec.fromPairs(ArrivalRate.class, 0.5), rand));
    sim.initialize();
    sim.executeUntil(TimeStamp.of(10));

    log.flush();

    assertFalse(logData.toString().isEmpty());

    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    assertEquals(numAgents, payoffs.size());
    assertEquals(0, payoffs.values().stream().mapToDouble(AgentInfo::getProfit).sum(), tol);
    assertTrue(payoffs.values().stream().mapToInt(AgentInfo::getSubmissions).allMatch(s -> s >= 0));
  }

  @Test
  public void simpleSpecTest() {
    int numAgents = 10;
    StringWriter logData = new StringWriter();
    Log log = Log.create(Level.DEBUG, logData, l -> l + ") ");
    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);

    Observation obs = CommandLineInterface.simulate(spec, log, 0);
    log.flush();

    assertFalse(logData.toString().isEmpty());

    Iterable<? extends Player> players = obs.getPlayers();
    assertEquals(numAgents, Iterables.size(players));
    assertEquals(0,
        StreamSupport.stream(players.spliterator(), false).mapToDouble(p -> p.getPayoff()).sum(),
        tol);
  }

  @Test
  public void simpleEgtaTest() {
    int numAgents = 10;
    StringWriter logData = new StringWriter(), obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    Runner.run(CommandLineInterface::simulate, specReader, obsData, logData, 1,
        Level.DEBUG.ordinal(), 1, true, keyPrefix, keyCaseFormat);

    assertFalse(logData.toString().isEmpty());
    assertFalse(obsData.toString().isEmpty());

    JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
    assertFalse(observation.has("features"));
    assertTrue(observation.has("players"));
    assertEquals(10, observation.getAsJsonArray("players").size());
    assertEquals(0,
        StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
            .mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(),
        tol);
    for (JsonElement player : observation.getAsJsonArray("players"))
      assertFalse(player.getAsJsonObject().has("features"));
  }

  @Test
  public void multiThreadEgtaTest() {
    int numAgents = 10;
    StringWriter logData = new StringWriter(), obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    Runner.run(CommandLineInterface::simulate, specReader, obsData, logData, 1,
        Level.DEBUG.ordinal(), 2, true, keyPrefix, keyCaseFormat);

    assertFalse(logData.toString().isEmpty());
    assertFalse(obsData.toString().isEmpty());

    JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
    assertFalse(observation.has("features"));
    assertTrue(observation.has("players"));
    assertEquals(10, observation.getAsJsonArray("players").size());
    assertEquals(0,
        StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
            .mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(),
        tol);
    for (JsonElement player : observation.getAsJsonArray("players"))
      assertFalse(player.getAsJsonObject().has("features"));
  }

  @Ignore // TODO No good way to test lack of features
  @Test
  public void sparseFeatureTest() {
    int numAgents = 10;
    StringWriter logData = new StringWriter(), obsData = new StringWriter();

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", "noop"), numAgents);
    Spec configuration = Spec.fromPairs(Markets.class, ImmutableList.of("cda"));
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    Runner.run(CommandLineInterface::simulate, specReader, obsData, logData, 1,
        Level.DEBUG.ordinal(), 1, false, keyPrefix, keyCaseFormat);

    assertTrue(logData.toString().isEmpty());
    assertFalse(obsData.toString().isEmpty());

    JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
    assertFalse(observation.has("features"));
    assertTrue(observation.has("players"));
    assertEquals(10, observation.getAsJsonArray("players").size());
    assertEquals(0,
        StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
            .mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(),
        tol);
    for (JsonElement player : observation.getAsJsonArray("players"))
      assertFalse(player.getAsJsonObject().has("features"));
  }

  @Test
  public void simpleFullTest() {
    int numAgents = 10;
    StringWriter logData = new StringWriter(), obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    Runner.run(CommandLineInterface::simulate, specReader, obsData, logData, 1,
        Level.DEBUG.ordinal(), 1, false, keyPrefix, keyCaseFormat);

    assertFalse(logData.toString().isEmpty());
    assertFalse(obsData.toString().isEmpty());

    JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
    assertTrue(observation.has("features"));
    assertTrue(observation.has("players"));
    assertEquals(10, observation.getAsJsonArray("players").size());
    assertEquals(0,
        StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
            .mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(),
        tol);
    for (JsonElement player : observation.getAsJsonArray("players"))
      assertTrue(player.getAsJsonObject().has("features"));
  }

  /**
   * Tests to see if identical simulations with the same random seed produce the same result. We
   * can't arbitrarily order the agents, because different entry orders in the event queue will
   * produce different scheduling, and hence, slightly different results.
   * 
   * This test is also run 10 times with two threads so that it will detect race conditions.
   */
  @Test
  public void identicalRandomTest() {
    int numAgentAs = 10, numAgentBs = 5;
    long seed = rand.nextLong();

    StringWriter obsData = new StringWriter();

    Spec aAgentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec bAgentSpec = Spec.fromPairs(ArrivalRate.class, 0.8);
    Spec configuration = Spec.builder().put(SimLength.class, 10l)
        .put(Markets.class, ImmutableList.of("cda")).put(FundamentalMeanReversion.class, 0d)
        .put(FundamentalShockVar.class, 0d).put(RandomSeed.class, seed).build();

    Multiset<RoleStrat> assignment = ImmutableMultiset.<RoleStrat>builder()
        .addCopies(RoleStrat.of("role", toStratString("noise", aAgentSpec)), numAgentAs)
        .addCopies(RoleStrat.of("role", toStratString("noise", bAgentSpec)), numAgentBs).build();
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);

    // Run the simulation once
    Runner.run(CommandLineInterface::simulate, specReader, obsData, NullWriter.get(), 10,
        Level.DEBUG.ordinal(), 2, false, keyPrefix, keyCaseFormat);

    // Save the results
    // Trim because gson complains about trailing newline
    Iterator<JsonObject> obs1s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).iterator();

    // Reset
    obsData = new StringWriter();
    specReader = toReader(spec);


    // Run the simulation again
    Runner.run(CommandLineInterface::simulate, specReader, obsData, NullWriter.get(), 10,
        Level.DEBUG.ordinal(), 3, false, keyPrefix, keyCaseFormat);

    // Save the results
    // Trim because gson complains about trailing newline
    Iterator<JsonObject> obs2s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).iterator();

    // Verify identical output for players
    // If this fails, that does't mean they weren't identical, but more care will need to be taken
    // for the comparison
    while (obs1s.hasNext() && obs2s.hasNext()) {
      JsonObject obs1 = obs1s.next();
      JsonObject obs2 = obs2s.next();

      assertEquals(obs1.get("players"), obs2.get("players"));

      JsonObject obs1Features = obs1.get("features").getAsJsonObject();
      JsonObject obs2Features = obs2.get("features").getAsJsonObject();
      JsonObject obs1Market = removeMarketFeatures(obs1Features);
      JsonObject obs2Market = removeMarketFeatures(obs2Features);

      assertEquals(obs1Features, obs2Features);
      assertEquals(obs1Market, obs2Market);
    }

    assertFalse("Simulation 1 had more observations", obs1s.hasNext());
    assertFalse("Simulation 2 had more observations", obs2s.hasNext());
  }

  // TODO Test that verifies agents inherit specifications from configuration

  // TODO Test that invalid agent names throw exception

  private static JsonObject removeMarketFeatures(JsonObject features) {
    String marketName = features.entrySet().stream().map(Map.Entry::getKey)
        .filter(k -> k.startsWith("cda_")).findAny().get();
    return features.remove(marketName).getAsJsonObject();
  }

  private static String toStratString(String name, Spec spec) {
    StringBuilder strat = new StringBuilder(name).append(':');
    stratJoiner.appendTo(strat,
        spec.entrySet().stream()
            .map(e -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, e.getKey().getSimpleName())
                + '_' + e.getValue())
            .iterator());
    return strat.toString();
  }

  private static Reader toReader(SimSpec spec) {
    JsonObject json = new JsonObject();

    JsonObject assignment = new JsonObject();
    for (Entry<RoleStrat> entry : spec.assignment.entrySet()) {
      JsonArray strategies;
      if (assignment.has(entry.getElement().getRole())) {
        strategies = assignment.getAsJsonArray(entry.getElement().getRole());
      } else {
        strategies = new JsonArray();
        assignment.add(entry.getElement().getRole(), strategies);
      }
      for (int i = 0; i < entry.getCount(); ++i)
        strategies.add(new JsonPrimitive(entry.getElement().getStrategy()));
    }
    json.add("assignment", assignment);

    JsonObject configuration = new JsonObject();
    for (Map.Entry<Class<? extends Value<?>>, Value<?>> entry : spec.configuration.entrySet()) {
      configuration.addProperty(
          CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, entry.getKey().getSimpleName()),
          entry.getValue().toString());
    }
    json.add("configuration", configuration);

    return new StringReader(json.toString());
  }

}
