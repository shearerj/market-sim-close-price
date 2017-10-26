package edu.umich.srg.marketsim;


import static edu.umich.srg.fourheap.OrderType.BUY;
import static edu.umich.srg.fourheap.OrderType.SELL;
import static edu.umich.srg.testing.Asserts.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.CaseFormat;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.CommandLineOptions;
import edu.umich.srg.egtaonline.Observation;
import edu.umich.srg.egtaonline.Observation.Player;
import edu.umich.srg.egtaonline.SimSpec;
import edu.umich.srg.egtaonline.SimSpec.RoleStrat;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.egtaonline.spec.Value;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.ClearInterval;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.Markets;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.RandomSeed;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CallMarket;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.testing.MockAgent;
import edu.umich.srg.testing.TestInts;

import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RunWith(Theories.class)
public class IntegrationTest {

  private static final Random rand = new Random();
  private static final Gson gson = new Gson();
  private static final Package keyPackage = Keys.class.getPackage();
  private static final double tol = 1e-8;

  // FIXME Add test of known small games, i.e. two agents one clear

  // FIXME Test that independent of call or cda, zi submissions are identical

  @Test
  public void simpleMinimalTest() {
    int numAgents = 10;

    Fundamental fundamental = ConstantFundamental.create(0, 100);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim, fundamental));
    for (int i = 0; i < numAgents; ++i)
      sim.addAgent(new NoiseAgent(sim, cda, Spec.fromPairs(ArrivalRate.class, 0.5), rand));
    sim.initialize();
    sim.executeUntil(TimeStamp.of(10));
    sim.after();

    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    assertEquals(numAgents, payoffs.size());
    assertEquals(0, payoffs.values().stream().mapToDouble(AgentInfo::getProfit).sum(), tol);
    assertTrue(payoffs.values().stream().mapToInt(AgentInfo::getSubmissions).allMatch(s -> s >= 0));
  }

  @Test
  public void simpleSpecTest() {
    int numAgents = 10;
    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);

    Observation obs = CommandLineInterface.simulate(spec, 0);

    Collection<? extends Player> players = obs.getPlayers();
    assertEquals(numAgents, players.size());
    assertEquals(0, players.stream().mapToDouble(p -> p.getPayoff()).sum(), tol);
  }

  @Test
  public void simpleEgtaTest() {
    int numAgents = 10;
    StringWriter obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 1, 1, 1, true,
        false, keyPackage);

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

  /**
   * Test that the the multi job pipeline does the correct thing. This is only run with one
   * observation, so the multiple threads don't actually do anything.
   */
  @Test
  public void multiThreadEgtaTest() {
    int numAgents = 10;
    StringWriter obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 1, 1, 2, true,
        false, keyPackage);

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
    StringWriter obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 1, 1, 1, false,
        false, keyPackage);

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
    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 10, 1, 1, false,
        false, keyPackage);

    // Save the results
    List<JsonObject> obs1s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).collect(Collectors.toList());

    // Reset
    obsData = new StringWriter();
    specReader = toReader(spec);


    // Run the simulation again
    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 10, 1, 1, false,
        false, keyPackage);

    // Save the results
    List<JsonObject> obs2s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).collect(Collectors.toList());

    // Verify identical output
    // If this fails, that does't mean they weren't identical, but more care will need to be taken
    // for the comparison, e.g. lfoating point things.
    assertEquals(obs1s, obs2s);
  }

  /** Tests that num jobs doesn't change order of observations. */
  @Test
  public void raceConditionTest() {
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

    // Run the simulation once with one job
    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 10, 1, 1, false,
        false, keyPackage);

    // Save the results
    List<JsonObject> obs1s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).collect(Collectors.toList());

    // Reset
    obsData = new StringWriter();
    specReader = toReader(spec);


    // Run the simulation again with two jobs
    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 10, 1, 2, false,
        false, keyPackage);

    // Save the results
    List<JsonObject> obs2s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).collect(Collectors.toList());

    // Verify identical output
    // If this fails, that does't mean they weren't identical, but more care will need to be taken
    // for the comparison, e.g. floating point stuff
    assertEquals(obs1s, obs2s);
  }

  /** Tests that simsPerObs appropriately aggregates information. */
  @Test
  public void simsPerObsTest() {
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

    // Run the simulation ten times
    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 20, 1, 1, true,
        false, keyPackage);

    // Save the average payoff per role strategy, the only thing that can be guaranteed
    List<Map<RoleStrat, DoubleSummaryStatistics>> payoffsNormal =
        aggregateStringObservations(obsData.toString(), 10);

    // Reset
    obsData = new StringWriter();
    specReader = toReader(spec);

    // Run the simulation again but only return one aggregate observation
    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 2, 10, 1, true,
        false, keyPackage);

    List<Map<RoleStrat, DoubleSummaryStatistics>> payoffsObsPerSim =
        aggregateStringObservations(obsData.toString(), 1);

    // Verify identical mean payoffs for role and strategy
    Iterator<Map<RoleStrat, DoubleSummaryStatistics>> it1 = payoffsNormal.iterator();
    Iterator<Map<RoleStrat, DoubleSummaryStatistics>> it2 = payoffsObsPerSim.iterator();
    while (it1.hasNext() && it2.hasNext()) {
      Map<RoleStrat, DoubleSummaryStatistics> m1 = it1.next();
      Map<RoleStrat, DoubleSummaryStatistics> m2 = it2.next();

      assertEquals("Both maps don't have the same keys", m1.keySet(), m2.keySet());
      for (Map.Entry<RoleStrat, DoubleSummaryStatistics> entry : m1.entrySet()) {
        assertEquals(entry.getValue().getAverage(), m2.get(entry.getKey()).getAverage(),
            Math.max(Math.abs(tol * entry.getValue().getAverage()), tol));
        assertEquals(entry.getValue().getCount(), m2.get(entry.getKey()).getCount() * 10);
      }
    }

    // Check that iterators are the same length
    assertFalse(it1.hasNext());
    assertFalse(it2.hasNext());
  }

  /** Tests that differently order spec files produce identical results. */
  @Theory
  public void specOrderingTest(@TestInts({1, 10}) int numObs, @TestInts({1, 10}) int simsPerObs) {

    String spec1 = "{\"assignment\":{\"role\":{" // Base assignment
        + "\"noise:arrivalRate_0.2\":8," // Agent type one
        + "\"noise:arrivalRate_0.5\":5," // Agent type two
        + "\"noise:arrivalRate_0.8\":2" // Agent type three
        + "}},\"configuration\":{\"randomSeed\":1234,\"markets\":\"cda\","
        + "\"simLength\":10,\"fundamentalMean\":1500,\"fundamentalMeanReversion\":0,"
        + "\"fundamentalShockVar\":0}}";
    Reader specReader1 = new StringReader(spec1);
    StringWriter obsData1 = new StringWriter();
    CommandLineOptions.run(CommandLineInterface::simulate, specReader1, obsData1, numObs,
        simsPerObs, 1, false, false, keyPackage);
    // All results
    List<JsonObject> results1 = Arrays.stream(obsData1.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).collect(Collectors.toList());
    // Just player payoffs
    List<List<Double>> payoffs1 = results1.stream()
        .map(j -> StreamSupport.stream(j.get("players").getAsJsonArray().spliterator(), false)
            .map(p -> p.getAsJsonObject().get("payoff").getAsDouble()).collect(Collectors.toList()))
        .collect(Collectors.toList());
    // Sorted payoffs, immune to player ordering in results
    List<List<Double>> sortedPayoffs1 =
        payoffs1.stream().map(pays -> pays.stream().sorted().collect(Collectors.toList()))
            .collect(Collectors.toList());

    String spec2 = "{\"assignment\":{\"role\":{" // Base assignment
        + "\"noise:arrivalRate_0.5\":5," // Agent type two
        + "\"noise:arrivalRate_0.8\":2," // Agent type three
        + "\"noise:arrivalRate_0.2\":8" // Agent type one
        + "}},\"configuration\":{\"randomSeed\":1234,\"markets\":\"cda\","
        + "\"simLength\":10,\"fundamentalMean\":1500,\"fundamentalMeanReversion\":0,"
        + "\"fundamentalShockVar\":0}}";
    Reader specReader2 = new StringReader(spec2);
    StringWriter obsData2 = new StringWriter();
    CommandLineOptions.run(CommandLineInterface::simulate, specReader2, obsData2, numObs,
        simsPerObs, 1, false, false, keyPackage);
    // All results
    List<JsonObject> results2 = Arrays.stream(obsData2.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).collect(Collectors.toList());
    // Just player payoffs
    List<List<Double>> payoffs2 = results2.stream()
        .map(j -> StreamSupport.stream(j.get("players").getAsJsonArray().spliterator(), false)
            .map(p -> p.getAsJsonObject().get("payoff").getAsDouble()).collect(Collectors.toList()))
        .collect(Collectors.toList());
    // Sorted payoffs, immune to player ordering in results
    List<List<Double>> sortedPayoffs2 =
        payoffs2.stream().map(pays -> pays.stream().sorted().collect(Collectors.toList()))
            .collect(Collectors.toList());

    assertEquals("Unique payoffs produced by identical seeds were not identical", sortedPayoffs1,
        sortedPayoffs2);
    assertEquals("Unique payoffs were identical, but not produced in the same order", payoffs1,
        payoffs2);
    assertEquals("Payoffs were identical, but the rest of the spec was not", results1, results2);
  }

  @Test
  public void printKeysTest() {
    StringWriter writer = new StringWriter();
    Spec.printKeys(CommandLineInterface.class.getPackage(), writer);
    assertFalse(writer.toString().isEmpty(), "print keys text was empty");
  }

  @Test
  /** Agent must inherit from global spec otherwise it'd be ill specified. */
  public void inheritanceTest1() {
    Spec configuration = Spec.builder() //
        .put(SimLength.class, 10l) //
        .put(Markets.class, ImmutableList.of("cda")) //
        .put(FundamentalMeanReversion.class, 0d) //
        .put(FundamentalShockVar.class, 0d) //
        .put(ArrivalRate.class, 1d) //
        .put(MaxPosition.class, 10) //
        .put(PrivateValueVar.class, 0d) //
        .put(Rmin.class, 0) //
        .put(Rmax.class, 0) //
        .build();

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("zir", Spec.empty())));
    SimSpec spec = SimSpec.create(assignment, configuration);
    CommandLineInterface.simulate(spec, 0);
  }

  @Test
  /**
   * If agent inherits from spec, they should never trade, so cda prices will be empty
   * 
   * This test isn't perfect as very unlikely random events could result in them not trading
   * spontaneously, or if private values are very high, they could trade despite the shading. That
   * being said, I think the probability is low wnough to justify this test.
   */
  public void inheritanceTest2() {
    Spec configuration = Spec.builder() //
        .put(SimLength.class, 1000l) //
        .put(Markets.class, ImmutableList.of("cda")) //
        .put(FundamentalMeanReversion.class, 0d) //
        .put(FundamentalShockVar.class, 0d) //
        .put(ArrivalRate.class, 1d) //
        .put(MaxPosition.class, 10) //
        .put(PrivateValueVar.class, 0d) //
        .put(Rmin.class, 100000) //
        .put(Rmax.class, 100000) //
        .build();
    Spec truthful = Spec.fromPairs(Rmin.class, 0, Rmax.class, 0);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("zir", Spec.empty())));
    assignment.add(RoleStrat.of("role", toStratString("zir", truthful)));
    SimSpec spec = SimSpec.create(assignment, configuration);
    Observation obs = CommandLineInterface.simulate(spec, 0);

    JsonArray prices = obs.getFeatures().get("markets").getAsJsonArray().get(0).getAsJsonObject()
        .get("prices").getAsJsonArray();
    assertEquals(prices.size(), 0);
  }

  @Test(expected = NullPointerException.class)
  public void invalidAgentTest() {
    String spec = "{\"assignment\":{\"role\":{" // Base assignment
        + "\"invalid\":1" // Agent type one
        + "}},\"configuration\":{\"randomSeed\":1234,\"markets\":\"cda\","
        + "\"simLength\":10,\"fundamentalMean\":1500,\"fundamentalMeanReversion\":0,"
        + "\"fundamentalShockVar\":0}}";

    Reader specReader = new StringReader(spec);
    StringWriter obsData = new StringWriter();
    CommandLineOptions.run(CommandLineInterface::simulate, specReader, obsData, 1, 1, 1, true,
        false, keyPackage);
  }

  @Test
  public void longCallMarketTest() {
    Fundamental fundamental = ConstantFundamental.create(0, 100);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market call = sim.addMarket(CallMarket.create(sim, fundamental, 0.5, 1000, rand));
    sim.addAgent(new MockAgent() {
      @Override
      public void initilaize() {
        MarketView view = call.getView(this);
        sim.scheduleIn(TimeStamp.ZERO, () -> {
          view.submitOrder(BUY, Price.of(1000), 1);
        });
      }
    });
    sim.addAgent(new MockAgent() {
      @Override
      public void initilaize() {
        MarketView view = call.getView(this);
        sim.scheduleIn(TimeStamp.ZERO, () -> {
          view.submitOrder(SELL, Price.of(1000), 1);
        });
      }
    });

    sim.initialize();
    sim.executeUntil(TimeStamp.of(10));
    sim.after();

    assertTrue(sim.getAgentPayoffs().values().stream().allMatch(p -> p.getHoldings() != 0));
  }

  // There's a small chance this will fail, but it's 2^-99
  @Test
  public void longCallSpecTest() {
    int numAgents = 100;
    Spec configuration = Spec.builder() //
        .put(SimLength.class, 10L) //
        .put(Markets.class, ImmutableList.of("call")) //
        .put(FundamentalMeanReversion.class, 0d) //
        .put(FundamentalShockVar.class, 0d) //
        .put(ArrivalRate.class, 0.5) //
        .put(ClearInterval.class, 11L) //
        .build();

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", "noise"), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);

    Observation obs = CommandLineInterface.simulate(spec, 0);

    Collection<? extends Player> players = obs.getPlayers();
    assertEquals(numAgents, players.size());
    assertTrue(players.stream().anyMatch(p -> p.getPayoff() != 0));
  }

  @Theory
  public void medianSpreadTest(@TestInts({2}) int simLength) {
    Fundamental fundamental = ConstantFundamental.create(0, simLength);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market call = sim.addMarket(CdaMarket.create(sim, fundamental));
    sim.addAgent(new MockAgent() {
      @Override
      public void initilaize() {
        MarketView view = call.getView(this);
        sim.scheduleIn(TimeStamp.of(simLength / 2), () -> {
          view.submitOrder(BUY, Price.of(8), 1);
        });
      }
    });
    sim.addAgent(new MockAgent() {
      @Override
      public void initilaize() {
        MarketView view = call.getView(this);
        sim.scheduleIn(TimeStamp.of(simLength / 2), () -> {
          view.submitOrder(SELL, Price.of(12), 1);
        });
      }
    });

    sim.initialize();
    sim.executeUntil(TimeStamp.of(simLength));
    sim.after();

    assertEquals(4, sim.getFeatures().get("markets").getAsJsonArray().get(0).getAsJsonObject()
        .get("median_spread").getAsDouble(), 1e-7);
  }

  private static String toStratString(String name, Spec spec) {
    return name + ':' + spec.entrySet().stream()
        .map(e -> e.getKey().getSimpleName() + '_' + e.getValue()).collect(Collectors.joining("_"));
  }

  private static Reader toReader(SimSpec spec) {
    JsonObject json = new JsonObject();

    JsonObject assignment = new JsonObject();
    for (Entry<RoleStrat> entry : spec.assignment.entrySet()) {
      String role = entry.getElement().getRole();
      JsonElement strategies = assignment.get(role);
      if (strategies == null) {
        strategies = new JsonObject();
        assignment.add(role, strategies);
      }
      strategies.getAsJsonObject().addProperty(entry.getElement().getStrategy(), entry.getCount());
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

  private static List<Map<RoleStrat, DoubleSummaryStatistics>> aggregateStringObservations(
      String observations, int perGroup) {
    // Save the average payoff per role strategy, the only thing that can be guaranteed
    return Lists
        // Group by 10
        .partition(Arrays.asList(observations.split("\n")), perGroup).stream()
        // Aggregate each group of 10
        .map(
            // Turn all observations into a stream of player JsonObjects
            obsStrings -> obsStrings.stream().map(line -> gson.fromJson(line, JsonObject.class))
                .flatMap(obs -> StreamSupport
                    .stream(obs.get("players").getAsJsonArray().spliterator(), false))
                .map(JsonElement::getAsJsonObject)
                // Collect into map to a double summary stats object
                .collect(Collectors.groupingBy(
                    p -> RoleStrat.of(p.get("role").getAsString(), p.get("strategy").getAsString()),
                    Collectors.summarizingDouble(p -> p.get("payoff").getAsDouble()))))
        .collect(Collectors.toList());
  }

}
