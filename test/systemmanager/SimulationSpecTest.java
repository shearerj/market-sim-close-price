package systemmanager;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.junit.Test;

import static systemmanager.Consts.Presets.*;
import static systemmanager.Consts.MarketType.*;
import static systemmanager.Consts.AgentType.*;

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
	public void givenNameAndNumberTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.MODEL_NAME, "TestName_WHY?NOT...");
		
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals(1, spec.getSimulationProps().getAsInt(Keys.MODEL_NUM));
		assertEquals("TestName_WHY?NOT...", spec.getSimulationProps().getAsString(Keys.MODEL_NAME));
		
		config.addProperty(Keys.MODEL_NUM, 6);
		
		spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals(6, spec.getSimulationProps().getAsInt(Keys.MODEL_NUM));
		assertEquals("TestName_WHY?NOT...", spec.getSimulationProps().getAsString(Keys.MODEL_NAME));
	}
	
	@Test
	public void presetNameAndNumberTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(Keys.PRESETS, TWOMARKETLA.toString());
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals(1, spec.getSimulationProps().getAsInt(Keys.MODEL_NUM));
		assertEquals("TWOMARKETLA_1", spec.getSimulationProps().getAsString(Keys.MODEL_NAME));
		
		config.addProperty(Keys.MODEL_NUM, 7);
		
		spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals(7, spec.getSimulationProps().getAsInt(Keys.MODEL_NUM));
		assertEquals("TWOMARKETLA_7", spec.getSimulationProps().getAsString(Keys.MODEL_NAME));
	}
	
	@Test
	public void arbitraryNameAndNumberTest() {
		JsonObject json = new JsonObject();
		JsonObject config = new JsonObject();
		json.add(Keys.CONFIG, config);
		config.addProperty(CDA.toString(), Keys.NUM + "_1");
		
		SimulationSpec spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals("1CDA_1", spec.getSimulationProps().getAsString(Keys.MODEL_NAME));
		
		config.addProperty(CALL.toString(), Keys.NUM + "_4");
		
		spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals("1CDA_4CALL_1", spec.getSimulationProps().getAsString(Keys.MODEL_NAME));
		
		config.addProperty(LA.toString(), Keys.NUM + "_51");
		
		spec = new SimulationSpec(new StringReader(json.toString()));
		assertEquals("1CDA_4CALL_51LA_1", spec.getSimulationProps().getAsString(Keys.MODEL_NAME));
	}

}
