package edu.umich.srg.marketsim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Random;
import java.util.stream.StreamSupport;

import org.junit.Test;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
import edu.umich.srg.marketsim.Keys.Markets;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.market.CDAMarket;
import edu.umich.srg.marketsim.market.Market;

public class IntegrationTest {
	
	private static final Random rand = new Random();
	private static final Gson gson = new Gson();
	private static final Joiner stratJoiner = Joiner.on('_');
	private static final String keyPrefix = "edu.umich.srg.marketsim.Keys$";
	private static final CaseFormat keyCaseFormat = CaseFormat.LOWER_CAMEL;
	private static final double eps = 1e-8;

	@Test
	public void simpleMinimalTest() {
		int numAgents = 10;
		StringWriter logData = new StringWriter();
		Log log = Log.create(Level.DEBUG, logData, l -> l + ") ");
		
		MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), log, rand);
		Market cda = sim.addMarket(CDAMarket.create(sim));
		for (int i = 0; i < numAgents; ++i)
			sim.addAgent(new NoiseAgent(sim, cda, Spec.fromPairs(ArrivalRate.class, 0.5), rand));
		sim.initialize();
		sim.executeUntil(TimeStamp.of(10));
		
		log.flush();
		
		assertFalse(logData.toString().isEmpty());
		
		Map<Agent, Double> payoffs = sim.getAgentPayoffs();
		assertEquals(numAgents, payoffs.size());
		assertEquals(0, payoffs.values().stream().mapToDouble(Double::doubleValue).sum(), eps);
	}
	
	@Test
	public void simpleSpecTest() {
		int numAgents = 10;
		StringWriter logData = new StringWriter();
		Log log = Log.create(Level.DEBUG, logData, l -> l + ") ");
		Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
		Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class, ImmutableList.of("cda"));

		Multiset<RoleStrat> assignment = HashMultiset.create(1);
		assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
		SimSpec spec = SimSpec.create(assignment, configuration);
		
		Observation obs = CommandLineInterface.simulate(spec, log, 0);
		log.flush();
		
		assertFalse(logData.toString().isEmpty());

		Iterable<? extends Player> players = obs.getPlayers();
		assertEquals(numAgents, Iterables.size(players));
		assertEquals(0, StreamSupport.stream(players.spliterator(), false).mapToDouble(p -> p.getPayoff()).sum(), eps);
	}
	
	@Test
	public void simpleEgtaTest() {
		int numAgents = 10;
		StringWriter logData = new StringWriter(), obsData = new StringWriter();
		
		Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
		Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class, ImmutableList.of("cda"));

		Multiset<RoleStrat> assignment = HashMultiset.create(1);
		assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
		SimSpec spec = SimSpec.create(assignment, configuration);
		Reader specReader = toReader(spec);
		
		
		Runner.run(CommandLineInterface::simulate, specReader, obsData, logData, 1, Level.DEBUG.ordinal(), true, keyPrefix, keyCaseFormat);
				
		assertFalse(logData.toString().isEmpty());
		assertFalse(obsData.toString().isEmpty());
		
		JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
		assertFalse(observation.has("features"));
		assertTrue(observation.has("players"));
		assertEquals(10, observation.getAsJsonArray("players").size());
		assertEquals(0, StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
				.mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(), eps);
		for (JsonElement player : observation.getAsJsonArray("players"))
			assertFalse(player.getAsJsonObject().has("features"));
	}
	
	@Test
	public void sparseFeatureTest() {
		int numAgents = 10;
		StringWriter logData = new StringWriter(), obsData = new StringWriter();
		
		Multiset<RoleStrat> assignment = HashMultiset.create(1);
		assignment.add(RoleStrat.of("role", "noop"), numAgents);
		Spec configuration = Spec.fromPairs(Markets.class, ImmutableList.of("cda"));
		SimSpec spec = SimSpec.create(assignment, configuration);
		Reader specReader = toReader(spec);
		
		
		Runner.run(CommandLineInterface::simulate, specReader, obsData, logData, 1, Level.DEBUG.ordinal(), false, keyPrefix, keyCaseFormat);
				
		assertTrue(logData.toString().isEmpty());
		assertFalse(obsData.toString().isEmpty());
		
		JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
		assertFalse(observation.has("features"));
		assertTrue(observation.has("players"));
		assertEquals(10, observation.getAsJsonArray("players").size());
		assertEquals(0, StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
				.mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(), eps);
		for (JsonElement player : observation.getAsJsonArray("players"))
			assertFalse(player.getAsJsonObject().has("features"));
	}
	
	@Test
	public void simpleFullTest() {
		int numAgents = 10;
		StringWriter logData = new StringWriter(), obsData = new StringWriter();
		
		Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
		Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class, ImmutableList.of("cda"));

		Multiset<RoleStrat> assignment = HashMultiset.create(1);
		assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
		SimSpec spec = SimSpec.create(assignment, configuration);
		Reader specReader = toReader(spec);
		
		
		Runner.run(CommandLineInterface::simulate, specReader, obsData, logData, 1, Level.DEBUG.ordinal(), false, keyPrefix, keyCaseFormat);
				
		assertFalse(logData.toString().isEmpty());
		assertFalse(obsData.toString().isEmpty());
		
		JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
		assertTrue(observation.has("features"));
		assertTrue(observation.has("players"));
		assertEquals(10, observation.getAsJsonArray("players").size());
		assertEquals(0, StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
				.mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(), eps);
		for (JsonElement player : observation.getAsJsonArray("players"))
			assertTrue(player.getAsJsonObject().has("features"));
	}
	
	private static String toStratString(String name, Spec spec) {
		StringBuilder strat = new StringBuilder(name).append(':');
		stratJoiner.appendTo(strat, spec.entrySet().stream()
				.map(e -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, e.getKey().getSimpleName()) + '_' + e.getValue())
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
