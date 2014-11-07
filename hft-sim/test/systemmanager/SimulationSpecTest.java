package systemmanager;

import static data.Preset.Presets.CENTRALCDA;
import static data.Props.keyToString;
import static org.junit.Assert.assertEquals;
import static systemmanager.Consts.AgentType.ZIR;
import static systemmanager.SimulationSpec.ASSIGNMENT;
import static systemmanager.SimulationSpec.CONFIG;

import java.util.Map.Entry;

import org.junit.Test;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Keys.ArrivalRate;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.NumMarkets;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.TickSize;
import systemmanager.SimulationSpec.PlayerSpec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import data.Preset;
import data.Props;

public class SimulationSpecTest {
	
	private static final Gson gson = new Gson();
	private static final JsonObject baseSpec = gson.toJsonTree(ImmutableMap.of(
			ASSIGNMENT, ImmutableMap.of("role", ImmutableList.of()),
			CONFIG, ImmutableMap.of(
					keyToString(ArrivalRate.class), 0.075,
					keyToString(PrivateValueVar.class), 5e6,
					keyToString(FundamentalShockVar.class), 1e6,
					Consts.AgentType.ZIR.toString(), Props.fromPairs(NumAgents.class, 1).toConfigString(),
					keyToString(TickSize.class), 1
					)
			)).getAsJsonObject();

	/** Tests to see if presets overwrite object accordingly */
	@Test
	public void copyPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(SimulationSpec.CONFIG, config);
		config.addProperty(Preset.KEY, CENTRALCDA.toString());

		SimulationSpec spec = SimulationSpec.fromJson(json);
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(1, (int) mp.getValue().get(NumMarkets.class));
				break;
			case CALL:
				assertEquals(0, (int) mp.getValue().get(NumMarkets.class));
				break;
			default:
			}
		}
		for (Entry<AgentType, Props> mp : spec.getAgentProps().entries()) {
			switch (mp.getKey()) {
			case LA:
				assertEquals(0, (int) mp.getValue().get(NumAgents.class));
				break;
			default:
			}
		}
	}
	
	/*
	 * This is the result of a major bug that caused default spec properties to
	 * not propogate to players.
	 */
	@Test
	public void defaultPropertiesTest() {
		JsonObject rawSpec = getBaseSpec();
		rawSpec.get(ASSIGNMENT).getAsJsonObject().get("role").getAsJsonArray().add(new JsonPrimitive(ZIR.toString()));
		SimulationSpec spec = SimulationSpec.fromJson(rawSpec);
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
		SimulationSpec spec = SimulationSpec.fromJson(rawSpec);

		PlayerSpec player = Iterables.getOnlyElement(spec.getPlayerProps());
		Props props = player.agentProps;
		assertEquals(ZIR, player.type);
		assertEquals(0.05, props.get(ArrivalRate.class), 0);
		assertEquals(7e8, props.get(PrivateValueVar.class), 0);
		assertEquals(1, (int) props.get(TickSize.class));
	}
	
	// Ugly deep copy. Can be made more efficient, but probably not necessary
	static JsonObject getBaseSpec() {
		return gson.fromJson(gson.toJson(baseSpec), JsonObject.class);
	}
	
}
