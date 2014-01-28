package data;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;

public class MultiSimulationObservationsTest {

	@Test
	public void singlePlayerTest() {
		MultiSimulationObservations obs = new MultiSimulationObservations(true, 1);
		
		obs.addObservation(new MockObservations(
				ImmutableList.of(new PlayerObservation("back", "a", 5, ImmutableMap.<String, Double> of())),
				ImmutableMap.<String, Double> of()));
		obs.addObservation(new MockObservations(
				ImmutableList.of(new PlayerObservation("back", "a", 10, ImmutableMap.<String, Double> of())),
				ImmutableMap.<String, Double> of()));
		obs.addObservation(new MockObservations(
				ImmutableList.of(new PlayerObservation("back", "a", 21, ImmutableMap.<String, Double> of())),
				ImmutableMap.<String, Double> of()));
		
		assertEquals(12, obs.playerObservations.get(0).payoff.getMean(), 0.001);
		// Sample Standard Deviation...
		assertEquals(8.185, obs.playerObservations.get(0).payoff.getStandardDeviation(), 0.001);
		
		JsonArray players = obs.toJson().getAsJsonObject().get("players").getAsJsonArray();
		assertEquals(12, players.get(0).getAsJsonObject().get("payoff").getAsDouble(), 0.001);
		assertEquals(8.185, players.get(0).getAsJsonObject().get("features").getAsJsonObject().get("payoff_stddev").getAsDouble(), 0.001);
	}
	
	@Test
	public void twoPlayerTest() {
		MultiSimulationObservations obs = new MultiSimulationObservations(true, 1);
		
		obs.addObservation(new MockObservations(
				ImmutableList.of(
						new PlayerObservation("back", "a", 5, ImmutableMap.<String, Double> of()),
						new PlayerObservation("back", "b", 10, ImmutableMap.<String, Double> of())),
				ImmutableMap.<String, Double> of()));
		obs.addObservation(new MockObservations(
				ImmutableList.of(
						new PlayerObservation("back", "a", 10, ImmutableMap.<String, Double> of()),
						new PlayerObservation("back", "b", 20, ImmutableMap.<String, Double> of())),
				ImmutableMap.<String, Double> of()));
		
		assertEquals(7.5, obs.playerObservations.get(0).payoff.getMean(), 0.001);
		assertEquals(15, obs.playerObservations.get(1).payoff.getMean(), 0.001);
		
		JsonArray players = obs.toJson().getAsJsonObject().get("players").getAsJsonArray();
		assertEquals(7.5, players.get(0).getAsJsonObject().get("payoff").getAsDouble(), 0.001);
		assertEquals("a", players.get(0).getAsJsonObject().get("strategy").getAsString());
		assertEquals(15, players.get(1).getAsJsonObject().get("payoff").getAsDouble(), 0.001);
	}

	// TODO tests for averaging player-level features
}
