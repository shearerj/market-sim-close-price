package systemmanager;

import static data.Props.keyToString;
import static org.junit.Assert.assertEquals;
import static systemmanager.SimulationSpec.ASSIGNMENT;
import static systemmanager.SimulationSpec.CONFIG;

import java.io.IOException;
import java.io.Writer;

import logger.Log.Level;

import org.junit.Test;

import systemmanager.Keys.FundamentalShockVar;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.NumMarkets;
import systemmanager.Keys.PrivateValueVar;
import systemmanager.Keys.ReentryRate;
import utils.Rand;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import data.Props;
import data.Stats;

public class SimulationTest {
	
	// FIXME Test that NOOP liquidates
	
	private static final Gson gson = new Gson();
	private static final JsonObject json = gson.toJsonTree(ImmutableMap.of(
			ASSIGNMENT, ImmutableMap.of("role", ImmutableList.of()),
			CONFIG, ImmutableMap.of(
					keyToString(ReentryRate.class), 0.075,
					keyToString(PrivateValueVar.class), 5e6,
					keyToString(FundamentalShockVar.class), 1e6,
					Consts.AgentType.ZIR.toString(), Props.fromPairs(NumAgents.class, 1).toConfigString(),
					Consts.MarketType.CDA.toString(), Props.fromPairs(NumMarkets.class, 1).toConfigString()
					)
			)).getAsJsonObject();
	
	@Test
	public void postFinalFundamental() {
		SimulationSpec spec = SimulationSpec.fromJson(json);
		Simulation sim = Simulation.create(spec, Rand.create(), new Writer() {
			@Override public void write(char[] arg0, int arg1, int arg2) throws IOException { }
			@Override public void flush() throws IOException { }
			@Override public void close() throws IOException { }
		}, Level.NO_LOGGING);
		sim.executeEvents();
		
		// Fundamental isn't exposed, but it seems hard to screw up, so at least this checks existence
		assertEquals(1, sim.getStatistics().getSummaryStats().get(Stats.FUNDAMENTAL_END_PRICE).n());
	}
	
	@Test
	public void randomTest() {
		for (int i = 0; i < 20; ++i) {
			postFinalFundamental();
		}
	}
	
}
