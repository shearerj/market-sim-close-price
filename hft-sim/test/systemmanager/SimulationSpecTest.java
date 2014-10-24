package systemmanager;

import static org.junit.Assert.assertEquals;
import static data.Preset.Presets.*;

import java.util.Map.Entry;

import org.junit.Test;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.NumMarkets;

import com.google.gson.JsonObject;

import data.Preset;
import data.Props;

public class SimulationSpecTest {

	/** Tests to see if presets overwrite object accordingly */
	@Test
	public void copyPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(SimulationSpec.CONFIG, config);
		config.addProperty(Preset.KEY, CENTRALCDA.toString());

		SimulationSpec spec = new SimulationSpec(json);
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

}
