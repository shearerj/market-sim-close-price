package systemmanager;

import static org.junit.Assert.assertEquals;
import static systemmanager.Consts.Presets.CENTRALCALL;
import static systemmanager.Consts.Presets.CENTRALCDA;
import static systemmanager.Consts.Presets.TWOMARKET;
import static systemmanager.Consts.Presets.TWOMARKETLA;

import java.util.Map.Entry;

import org.junit.Test;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;

import com.google.gson.JsonObject;

import data.Props;

public class SimulationSpecTest {

	@Test
	public void centralCDAPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, CENTRALCDA.toString());

		SimulationSpec spec = new SimulationSpec(json);
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(1, mp.getValue().getAsInt(Keys.NUM_MARKETS, Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getValue().getAsInt(Keys.NUM_MARKETS, Keys.NUM));
				break;
			default:
			}
		}
		for (Entry<AgentType, Props> mp : spec.getAgentProps().entries()) {
			switch (mp.getKey()) {
			case LA:
				assertEquals(0, mp.getValue().getAsInt(Keys.NUM_AGENTS, Keys.NUM));
				break;
			default:
			}
		}
	}

	@Test
	public void centralCallPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, CENTRALCALL.toString());
		config.addProperty(Keys.NBBO_LATENCY, 1337);
		
		SimulationSpec spec = new SimulationSpec(json);
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(0, mp.getValue().getAsInt(Keys.NUM_MARKETS, Keys.NUM));
				break;
			case CALL:
				assertEquals(1, mp.getValue().getAsInt(Keys.NUM_MARKETS, Keys.NUM));
				assertEquals(1337, mp.getValue().getAsInt(Keys.CLEAR_FREQ));
				break;
			default:
			}
		}
		for (Entry<AgentType, Props> mp : spec.getAgentProps().entries()) {
			switch (mp.getKey()) {
			case LA:
				assertEquals(0, mp.getValue().getAsInt(Keys.NUM_AGENTS, Keys.NUM));
				break;
			default:
			}
		}
	}
	
	@Test
	public void twoMarketPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, TWOMARKET.toString());
		
		SimulationSpec spec = new SimulationSpec(json);
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(2, mp.getValue().getAsInt(Keys.NUM_MARKETS, Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getValue().getAsInt(Keys.NUM_MARKETS, Keys.NUM));
				break;
			default:
			}
		}
		for (Entry<AgentType, Props> mp : spec.getAgentProps().entries()) {
			switch (mp.getKey()) {
			case LA:
				assertEquals(0, mp.getValue().getAsInt(Keys.NUM_AGENTS, Keys.NUM));
				break;
			default:
			}
		}
	}
	
	@Test
	public void twoMarketLAPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, TWOMARKETLA.toString());
		
		SimulationSpec spec = new SimulationSpec(json);
		for (Entry<MarketType, Props> mp : spec.getMarketProps().entries()) {
			switch (mp.getKey()) {
			case CDA:
				assertEquals(2, mp.getValue().getAsInt(Keys.NUM_MARKETS, Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getValue().getAsInt(Keys.NUM_MARKETS, Keys.NUM));
				break;
			default:
			}
		}
		for (Entry<AgentType, Props> mp : spec.getAgentProps().entries()) {
			switch (mp.getKey()) {
			case LA:
				assertEquals(1, mp.getValue().getAsInt(Keys.NUM_AGENTS, Keys.NUM));
				break;
			default:
			}
		}
	}

}
