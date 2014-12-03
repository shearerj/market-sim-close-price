package systemmanager;

import static data.Props.keyToString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static systemmanager.Consts.AgentType.FUNDAMENTALMM;
import static systemmanager.Consts.AgentType.ZIR;
import static systemmanager.Consts.AgentType.ZIRP;
import static systemmanager.SimulationSpec.ASSIGNMENT;
import static systemmanager.SimulationSpec.CONFIG;

import org.junit.Test;

import systemmanager.Keys.BackgroundReentryRate;
import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.MarketMakerReentryRate;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.ReentryRate;
import systemmanager.Keys.Rmax;
import systemmanager.Keys.Rmin;
import systemmanager.Keys.Spread;
import systemmanager.Keys.TickSize;
import systemmanager.SimulationSpec.PlayerSpec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import data.Props;
import entity.market.Price;

public class SimulationSpecTest {
	
	// FIXME test empty strings in config
	
	private static final Gson gson = new Gson();
	private static final double eps = 1e-6;
	private static final JsonObject baseSpec = gson.toJsonTree(ImmutableMap.of(
			ASSIGNMENT, ImmutableMap.of("role", ImmutableList.of()),
			CONFIG, ImmutableMap.of(
					keyToString(ReentryRate.class), 0.075,
					keyToString(PrivateValueVar.class), 5e6,
					keyToString(FundamentalShockVar.class), 1e6,
					Consts.AgentType.ZIR.toString(), Props.fromPairs(NumAgents.class, 1).toConfigString(),
					keyToString(TickSize.class), 1
					)
			)).getAsJsonObject();
	
	/** This is the result of a major bug that caused default spec properties to not propogate to players. */
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
		assertEquals(0.075, props.get(ReentryRate.class), 0);
		assertEquals(5e6, props.get(PrivateValueVar.class), 0);
		assertEquals(1, (int) props.get(TickSize.class));
	}
	
	@Test
	public void defaultPropertiesModificationsTest() {
		JsonObject rawSpec = getBaseSpec();
		rawSpec.get("configuration").getAsJsonObject().add(keyToString(PrivateValueVar.class), new JsonPrimitive(7e8));
		rawSpec.get("assignment").getAsJsonObject().get("role").getAsJsonArray().add(
				new JsonPrimitive(ZIR + ":" + Props.fromPairs(ReentryRate.class, 0.05).toConfigString()));
		SimulationSpec spec = SimulationSpec.fromJson(rawSpec);

		PlayerSpec player = Iterables.getOnlyElement(spec.getPlayerProps());
		Props props = player.agentProps;
		assertEquals(ZIR, player.type);
		assertEquals(0.05, props.get(ReentryRate.class), 0);
		assertEquals(7e8, props.get(PrivateValueVar.class), 0);
		assertEquals(1, (int) props.get(TickSize.class));
	}
	
	// Ugly deep copy. Can be made more efficient, but probably not necessary
	static JsonObject getBaseSpec() {
		return gson.fromJson(gson.toJson(baseSpec), JsonObject.class);
	}
	
	/**
	 * Tests different reentry rates for background & MM, also tests creation of
	 * players via simulation spec. Can only test via if the read in spec file
	 * is correct...
	 */
	@Test
	public void reentryTest() {
		JsonObject json = gson.toJsonTree(ImmutableMap.of(
				ASSIGNMENT, ImmutableMap.of("BACKGROUND", ImmutableList.of(
						ZIR + ":" + Props.fromPairs(Rmax.class, 100).toConfigString(),
						ZIRP + ":" + Props.fromPairs(Rmin.class, 10).toConfigString()),
						"MARKETMAKER", ImmutableList.of(
								FUNDAMENTALMM + ":" + Props.fromPairs(Spread.class, Price.of(256)).toConfigString())),
				CONFIG, ImmutableMap.of(
						keyToString(BackgroundReentryRate.class), 0.0005,
						keyToString(MarketMakerReentryRate.class), 0.05)
				)).getAsJsonObject();
		
		SimulationSpec spec = SimulationSpec.fromJson(json);
		
		for (PlayerSpec pspec : spec.getPlayerProps()) {
			switch(pspec.type) {
			case ZIR:
				assertEquals(100, (int) pspec.agentProps.get(Rmax.class));
				assertEquals(0.0005, pspec.agentProps.get(BackgroundReentryRate.class), eps);
				break;
			case ZIRP:
				assertEquals(10, (int) pspec.agentProps.get(Rmin.class));
				assertEquals(0.0005, pspec.agentProps.get(BackgroundReentryRate.class), eps);
				break;
			case FUNDAMENTALMM:
				assertEquals(Price.of(256), pspec.agentProps.get(Spread.class));
				assertEquals(0.05, pspec.agentProps.get(MarketMakerReentryRate.class), eps);
				break;
			default:
				fail("Shouldn't get here");
			}
		}
	}
	
	// FIXME Test that you can pass an empty calue string for agent setup in the spec config, and that agentnumber / num will be inhereted from the spec
	
}
