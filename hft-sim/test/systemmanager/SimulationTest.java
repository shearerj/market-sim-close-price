package systemmanager;

import static systemmanager.SimulationSpec.*;
import static data.Props.*;
import static org.junit.Assert.assertEquals;
import static systemmanager.Consts.AgentType.ZIR;

import org.junit.Test;

import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.TickSize;
import systemmanager.SimulationSpec.PlayerSpec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import data.Props;

public class SimulationTest {
	
	/*
	 * Testing proper setup of simulation is hard, so we'll just test the the
	 * parsed configuration that's passed in is what we expect
	 */

	// FIXME Add test for final fundamental price
	
	private static final Gson gson = new Gson();
	// This is a little hacky...
	private static final JsonObject baseSpec = gson.toJsonTree(ImmutableMap.of(
			ASSIGNMENT, ImmutableMap.of("role", ImmutableList.of()),
			CONFIG, ImmutableMap.of(
					keyToString(ArrivalRate.class), 0.075,
					keyToString(PrivateValueVar.class), 5e6,
					keyToString(FundamentalShockVar.class), 1e6,
					Consts.AgentType.ZI.toString(), Props.fromPairs(NumAgents.class, 1).toConfigString(),
					keyToString(TickSize.class), 1
					)
			)).getAsJsonObject();

	static JsonObject getBaseSpec() {
		// XXX Ugly deep copy. Can be made more efficient, but probably not necessary
		return gson.fromJson(gson.toJson(baseSpec), JsonObject.class);
	}
	
	/*
	 * This is the result of a major bug that caused default spec properties to
	 * not propogate to players.
	 */
	@Test
	public void defaultPropertiesTest() {
		JsonObject rawSpec = getBaseSpec();
		rawSpec.get(ASSIGNMENT).getAsJsonObject().get("role").getAsJsonArray().add(new JsonPrimitive(ZIR.toString()));
		SimulationSpec spec = new SimulationSpec(rawSpec);
		// Test correct spec, since it'd be too hard to test the actual created
		// agents, since knowledge is sealed away... Could use reflection...
		PlayerSpec player = Iterables.getOnlyElement(spec.getPlayerProps());
		Props props = player.agentProps;
		assertEquals(ZIR, player.type);
		assertEquals(0.075, props.get(ArrivalRate.class), 0);
		assertEquals(5e6, props.get(PrivateValueVar.class), 0);
		assertEquals(1, (int) props.get(TickSize.class));
	}
	
	@Test
	public void defaultPropertiesModificationsTest() {
		JsonObject rawSpec = getBaseSpec();
		rawSpec.get("configuration").getAsJsonObject().add(keyToString(PrivateValueVar.class), new JsonPrimitive(7e8));
		rawSpec.get("assignment").getAsJsonObject().get("role").getAsJsonArray().add(
				new JsonPrimitive(ZIR + ":" + Props.fromPairs(ArrivalRate.class, 0.05).toConfigString()));
		SimulationSpec spec = new SimulationSpec(rawSpec);

		PlayerSpec player = Iterables.getOnlyElement(spec.getPlayerProps());
		Props props = player.agentProps;
		assertEquals(ZIR, player.type);
		assertEquals(0.05, props.get(ArrivalRate.class), 0);
		assertEquals(7e8, props.get(PrivateValueVar.class), 0);
		assertEquals(1, (int) props.get(TickSize.class));
	}
	
}
