package systemmanager;

import static data.Preset.Presets.CENTRALCALL;
import static data.Preset.Presets.CENTRALCDA;
import static data.Preset.Presets.MAXEFF;
import static data.Preset.Presets.TWOMARKET;
import static data.Preset.Presets.TWOMARKETLA;
import static org.junit.Assert.assertEquals;

import java.util.Map.Entry;

import org.junit.Test;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Keys.ClearInterval;
import systemmanager.Keys.MaxPosition;
import systemmanager.Keys.NbboLatency;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.NumMarkets;
import systemmanager.Keys.SimLength;

import com.google.gson.JsonObject;

import data.Preset;
import data.Props;
import event.TimeStamp;

public class PresetTest {

	@Test
	public void centralCDAPresetTest() {
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

	@Test
	public void centralCallPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(SimulationSpec.CONFIG, config);
		config.addProperty(Preset.KEY, CENTRALCALL.toString());
		config.addProperty(Props.keyToString(NbboLatency.class), 1337);
		
		SimulationSpec spec = SimulationSpec.fromJson(json);
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(0, (int) mp.getValue().get(NumMarkets.class));
				break;
			case CALL:
				assertEquals(1, (int) mp.getValue().get(NumMarkets.class));
				assertEquals(TimeStamp.of(1337), mp.getValue().get(ClearInterval.class));
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
	
	@Test
	public void twoMarketPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(SimulationSpec.CONFIG, config);
		config.addProperty(Preset.KEY, TWOMARKET.toString());
		
		SimulationSpec spec = SimulationSpec.fromJson(json);
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(2, (int) mp.getValue().get(NumMarkets.class));
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
	
	@Test
	public void twoMarketLAPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(SimulationSpec.CONFIG, config);
		config.addProperty(Preset.KEY, TWOMARKETLA.toString());
		
		SimulationSpec spec = SimulationSpec.fromJson(json);
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(2, (int) mp.getValue().get(NumMarkets.class));
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
				assertEquals(1, (int) mp.getValue().get(NumAgents.class));
				break;
			default:
			}
		}
	}
	
	@Test
	public void maxEffPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(SimulationSpec.CONFIG, config);
		config.addProperty(Preset.KEY, MAXEFF.toString());
		
		SimulationSpec spec = SimulationSpec.fromJson(json);
		assertEquals(12, (int) spec.getSimulationProps().get(SimLength.class));
		
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(0, (int) mp.getValue().get(NumMarkets.class));
				break;
			case CALL:
				assertEquals(1, (int) mp.getValue().get(NumMarkets.class));
				assertEquals(TimeStamp.of(10), mp.getValue().get(ClearInterval.class));
				break;
			default:
			}
		}
		for (Entry<AgentType, Props> mp : spec.getAgentProps().entries()) {
			switch (mp.getKey()) {
			case MAXEFFICIENCY:
				assertEquals(66, (int) mp.getValue().get(NumAgents.class));
				assertEquals(Defaults.get(MaxPosition.class), mp.getValue().get(MaxPosition.class));
				break;
			default:
			}
		}
	}

}
