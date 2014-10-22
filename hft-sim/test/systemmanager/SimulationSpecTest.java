package systemmanager;

import static org.junit.Assert.assertEquals;
import static systemmanager.Consts.Presets.CENTRALCALL;
import static systemmanager.Consts.Presets.CENTRALCDA;
import static systemmanager.Consts.Presets.TWOMARKET;
import static systemmanager.Consts.Presets.TWOMARKETLA;
import static systemmanager.Consts.Presets.MAXEFF;

import java.io.StringReader;

import org.junit.Test;

import com.google.gson.JsonObject;

import data.AgentProperties;
import data.MarketProperties;

public class SimulationSpecTest {

	@Test
	public void centralCDAPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, CENTRALCDA.toString());
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(1, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case LA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
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
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(1, mp.getAsInt(Keys.NUM));
				assertEquals(1337, mp.getAsInt(Keys.CLEAR_FREQ));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case LA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
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
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(2, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case LA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
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
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(2, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case LA:
				assertEquals(1, mp.getAsInt(Keys.NUM));
				break;
			default:
			}
		}
	}

	@Test
	public void maxEffPresetTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, MAXEFF.toString());
		config.addProperty(Keys.MAX_POSITION, Defaults.get(Keys.MAX_POSITION));
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals(12, spec.simulationProperties.getAsInt(Keys.SIMULATION_LENGTH));
		
		for (MarketProperties mp : spec.getMarketProps()) {
			switch (mp.getMarketType()) {
			case CDA:
				assertEquals(0, mp.getAsInt(Keys.NUM));
				break;
			case CALL:
				assertEquals(1, mp.getAsInt(Keys.NUM));
				assertEquals(10, mp.getAsInt(Keys.CLEAR_FREQ));
				break;
			default:
			}
		}
		for (AgentProperties mp : spec.getAgentProps()) {
			switch (mp.getAgentType()) {
			case MAXEFFICIENCY:
				assertEquals(66, mp.getAsInt(Keys.NUM));
				assertEquals(Integer.parseInt(Defaults.get(Keys.MAX_POSITION)), mp.getAsInt(Keys.MAX_POSITION));
				break;
			default:
			}
		}
	}
	
}
