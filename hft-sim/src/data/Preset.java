package data;

import static systemmanager.Consts.AgentType.LA;
import static systemmanager.Consts.MarketType.CALL;
import static systemmanager.Consts.MarketType.CDA;
import static data.Props.keyToString;
import systemmanager.Keys.ClearFrequency;
import systemmanager.Keys.NbboLatency;
import systemmanager.Keys.NumAgents;
import systemmanager.Keys.NumMarkets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import event.TimeStamp;

public class Preset {
	
	public static enum Presets { NONE, TWOMARKET, TWOMARKETLA, CENTRALCDA, CENTRALCALL };
	public static final String KEY = "presets";

	public static JsonObject parsePresets(JsonObject config) {
		JsonElement preset = config.remove(KEY);
		if (preset == null || preset.getAsString().isEmpty())
			return config;
		switch(Presets.valueOf(preset.getAsString())) {
		case NONE:
			return config;
			
		case TWOMARKET:
			config.addProperty(CDA.toString(), Props.fromPairs(NumMarkets.class, 2).toConfigString());
			config.addProperty(CALL.toString(), Props.fromPairs(NumMarkets.class, 0).toConfigString());
			config.addProperty(LA.toString(), Props.fromPairs(NumAgents.class, 0).toConfigString());
			return config;

		case TWOMARKETLA:
			config.addProperty(CDA.toString(), Props.fromPairs(NumMarkets.class, 2).toConfigString());
			config.addProperty(CALL.toString(), Props.fromPairs(NumMarkets.class, 0).toConfigString());
			config.addProperty(LA.toString(), Props.fromPairs(NumAgents.class, 1).toConfigString());
			return config;

		case CENTRALCDA:
			config.addProperty(CDA.toString(), Props.fromPairs(NumMarkets.class, 1).toConfigString());
			config.addProperty(CALL.toString(), Props.fromPairs(NumMarkets.class, 0).toConfigString());
			config.addProperty(LA.toString(), Props.fromPairs(NumAgents.class, 0).toConfigString());
			return config;

		case CENTRALCALL:
			TimeStamp nbboLatency = TimeStamp.of(config.getAsJsonPrimitive(keyToString(NbboLatency.class)).getAsInt());
			config.addProperty(CDA.toString(), Props.fromPairs(NumMarkets.class, 0).toConfigString());
			config.addProperty(CALL.toString(), Props.fromPairs(NumMarkets.class, 1, ClearFrequency.class, nbboLatency).toConfigString());
			config.addProperty(LA.toString(), Props.fromPairs(NumAgents.class, 0).toConfigString());
			return config;

		default:
			throw new IllegalArgumentException("A preset was added, but a parser was never made for it");
		}
	}
	
}
