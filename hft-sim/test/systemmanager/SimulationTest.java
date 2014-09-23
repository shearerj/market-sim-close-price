package systemmanager;

import static org.junit.Assert.assertEquals;
import static systemmanager.Consts.AgentType.ZIR;

import org.junit.Test;

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
	private static final JsonObject baseSpec = gson.toJsonTree(ImmutableMap.of(
			"assignment", ImmutableMap.of(
					"role", ImmutableList.of()
					),
					"configuration", ImmutableMap.of(
							Keys.ARRIVAL_RATE, 0.075,
							Keys.PRIVATE_VALUE_VAR, 5e6,
							Keys.FUNDAMENTAL_SHOCK_VAR, 1e6,
							Consts.AgentType.ZI.toString(), Keys.NUM + "_" + 0,
							Keys.TICK_SIZE, 1
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
		rawSpec.get("assignment").getAsJsonObject().get("role").getAsJsonArray().add(new JsonPrimitive(ZIR.toString()));
		SimulationSpec spec = new SimulationSpec(rawSpec);
		// Test correct spec, since it'd be too hard to test the actual created
		// agents, since knowledge is sealed away... Could use reflection...
		PlayerSpec player = Iterables.getOnlyElement(spec.getPlayerProps());
		Props props = player.agentProps;
		assertEquals(ZIR, player.type);
		assertEquals(0.075, props.getAsDouble(Keys.ARRIVAL_RATE), 0);
		assertEquals(5e6, props.getAsDouble(Keys.PRIVATE_VALUE_VAR), 0);
		assertEquals(1, props.getAsInt(Keys.TICK_SIZE));
	}
	
	@Test
	public void defaultPropertiesModificationsTest() {
		JsonObject rawSpec = getBaseSpec();
		rawSpec.get("configuration").getAsJsonObject().add(Keys.PRIVATE_VALUE_VAR, new JsonPrimitive(7e8));
		rawSpec.get("assignment").getAsJsonObject().get("role").getAsJsonArray().add(new JsonPrimitive(ZIR + ":" + Keys.ARRIVAL_RATE + '_' + 0.05));
		SimulationSpec spec = new SimulationSpec(rawSpec);

		PlayerSpec player = Iterables.getOnlyElement(spec.getPlayerProps());
		Props props = player.agentProps;
		assertEquals(ZIR, player.type);
		assertEquals(0.05, props.getAsDouble(Keys.ARRIVAL_RATE), 0);
		assertEquals(7e8, props.getAsDouble(Keys.PRIVATE_VALUE_VAR), 0);
		assertEquals(1, props.getAsInt(Keys.TICK_SIZE));
	}
	
}
