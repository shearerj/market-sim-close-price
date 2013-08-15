package systemmanager;

import static systemmanager.Keys.*;
import static systemmanager.Consts.AgentType.*;
import static systemmanager.Consts.MarketType.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;
import systemmanager.Consts.Presets;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import data.AgentProperties;
import data.EntityProperties;
import data.MarketProperties;

/**
 * Stores list of web parameters used in EGTAOnline.
 * 
 * NOTE: All MarketModel types in the spec file must match the corresponding
 * class name.
 * 
 * @author ewah
 */
public class SimulationSpec {

	protected static final String DELIMITER = ";"; // Delimiter for list of configs
	
	protected static final String[] simulationKeys = { SIMULATION_LENGTH,
			FUNDAMENTAL_MEAN, FUNDAMENTAL_KAPPA, FUNDAMENTAL_SHOCK_VAR,
			RAND_SEED, NBBO_LATENCY, MODEL_NAME, MODEL_NUM };
	protected static final String[] marketKeys = { MARKET_LATENCY, TICK_SIZE };
	protected static final String[] agentKeys = { TICK_SIZE, ARRIVAL_RATE,
			REENTRY_RATE, PRIVATE_VALUE_VAR };

	protected final JsonObject rawSpec;
	protected final EntityProperties simulationProperties;
	protected final EntityProperties defaultMarketProperties;
	protected final EntityProperties defaultAgentProperties;

	protected final Collection<MarketProperties> marketProps;
	protected final Collection<AgentProperties> agentProps;
	protected final JsonObject playerProps; // TODO Change to properties object

	public SimulationSpec(File specFile) throws FileNotFoundException {
		this(new FileReader(specFile));
	}
	
	public SimulationSpec(Reader reader) {
		rawSpec = new Gson().fromJson(reader, JsonObject.class);
		JsonObject config = rawSpec.getAsJsonObject(Keys.CONFIG);
		JsonObject players = rawSpec.getAsJsonObject(Keys.ASSIGN);

		presets(config);
		
		defaultMarketProperties = readProperties(config, marketKeys);
		marketProps = markets(config, defaultMarketProperties);
		
		defaultAgentProperties = readProperties(config, agentKeys);
		agentProps = agents(config, defaultAgentProperties);
		
		getName(config, marketProps, agentProps);
		
		playerProps = players;
		simulationProperties = readProperties(config, simulationKeys);
	}

	public SimulationSpec(String specFileName) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		this(new File(specFileName));
	}

	protected static EntityProperties readProperties(JsonObject config,
			String... keys) {
		EntityProperties props = new EntityProperties();
		for (String key : keys) {
			JsonPrimitive value = config.getAsJsonPrimitive(key);
			if (value == null) continue;
			props.put(key, value.getAsString());
		}
		return props;
	}

	protected Collection<MarketProperties> markets(
			JsonObject config, EntityProperties def) {
		Collection<MarketProperties> markets = new ArrayList<MarketProperties>();

		for (MarketType marketType : MarketType.values()) {
			JsonPrimitive configJson = config.getAsJsonPrimitive(marketType.toString());
			if (configJson == null) continue;
			for (String marketConfig : configJson.getAsString().split(DELIMITER))
				markets.add(new MarketProperties(marketType, def, marketConfig));
		}
		return markets;
	}

	protected Collection<AgentProperties> agents(JsonObject config,
			EntityProperties def) {
		Collection<AgentProperties> backgroundAgents = new ArrayList<AgentProperties>();

		for (AgentType agentType : Consts.AgentType.values()) {
			JsonPrimitive setupJson = config.getAsJsonPrimitive(agentType.toString());
			if (setupJson == null) continue;
			for (String agentConfig : setupJson.getAsString().split(DELIMITER))
				backgroundAgents.add(new AgentProperties(agentType, def,
						agentConfig));
		}
		return backgroundAgents;
	}
	
	/**
	 * Set preset for standard simulations
	 */
	// XXX Just add a new case to add your own!
	protected void presets(JsonObject config) {
		JsonPrimitive preset = config.getAsJsonPrimitive(Keys.PRESETS);
		if (preset == null) return;
		switch(Presets.valueOf(preset.getAsString())) {
		case TWOMARKET:
			config.addProperty(CDA.toString(), NUM + "_2");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		case TWOMARKETLA:
			config.addProperty(CDA.toString(), NUM + "_2");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_1");
			break;
		case CENTRALCDA:
			config.addProperty(CDA.toString(), NUM + "_1");
			config.addProperty(CALL.toString(), NUM + "_0");
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		case CENTRALCALL:
			int nbboLatency = config.getAsJsonPrimitive(NBBO_LATENCY).getAsInt();
			config.addProperty(CDA.toString(), NUM + "_0");
			config.addProperty(CALL.toString(), NUM + "_1_" + CLEAR_FREQ + "_" + nbboLatency);
			config.addProperty(LA.toString(), NUM + "_0");
			break;
		default:
			// Should be impossible to reach here
			throw new IllegalArgumentException("Unknown Preset");
		}
		
	}
	
	/**
	 * Used to generate a unique name for the simulation. If a name is given in the spec, it will
	 * use that name. Otherwise if there's a preset, it will use that appended with the model
	 * number. If there's no preset it will form a string with the number of markets, LA agents, and
	 * the model number. The central call market would simply be called 1CALL_1. TWOMARKET with an
	 * LA would be 2CDA_1LA_1. A hypothetical model with 2 CDA markets, 1 call market, and 2 la
	 * agents would be 2CDA_1CALL_2LA_1.
	 */
	// XXX Move to System Manager? I'm not sure this should go here.
	protected void getName(JsonObject config,
			Collection<MarketProperties> marketProps,
			Collection<AgentProperties> agentProps) {
		if (!config.has(Keys.MODEL_NUM)) config.addProperty(Keys.MODEL_NUM, 1);
		if (config.has(Keys.MODEL_NAME)) return;
		JsonPrimitive preset = config.getAsJsonPrimitive(Keys.PRESETS);
		if (preset != null) {
			// Use preset
			config.addProperty(Keys.MODEL_NAME, preset.getAsString() + "_"
					+ config.getAsJsonPrimitive(Keys.MODEL_NUM).getAsString());
		} else {
			// Use config
			Map<MarketType, Integer> marketCounts = new HashMap<MarketType, Integer>(MarketType.values().length);
			for (MarketType mktType : MarketType.values())
				marketCounts.put(mktType, 0);
			for (MarketProperties props : marketProps)
				marketCounts.put(
						props.getMarketType(),
						props.getAsInt(Keys.NUM, 0) + marketCounts.get(props.getMarketType()));

			Map<AgentType, Integer> agentCounts = new HashMap<AgentType, Integer>(AgentType.values().length);
			for (AgentType agType : AgentType.values())
				agentCounts.put(agType, 0);
			for (AgentProperties props : agentProps)
				agentCounts.put(
						props.getAgentType(),
						props.getAsInt(Keys.NUM, 0) + agentCounts.get(props.getAgentType()));

			int num = config.getAsJsonPrimitive(Keys.MODEL_NUM).getAsInt();
			
			StringBuilder sb = new StringBuilder();
			for (Entry<MarketType, Integer> e : marketCounts.entrySet())
				if (e.getValue() > 0)
					sb.append(e.getValue()).append(e.getKey()).append('_');
			for (Entry<AgentType, Integer> e : agentCounts.entrySet())
				if (e.getValue() > 0 && e.getKey() == LA)
					sb.append(e.getValue()).append(e.getKey()).append('_');
			sb.append(num);
			config.addProperty(Keys.MODEL_NAME, sb.toString());
		}
	}

	public EntityProperties getSimulationProps() {
		return simulationProperties;
	}
	
	public EntityProperties getDefaultMarketProps() {
		return defaultMarketProperties;
	}
	
	public EntityProperties getDefaultAgentProps() {
		return defaultAgentProperties;
	}

	public Collection<MarketProperties> getMarketProps() {
		return Collections.unmodifiableCollection(marketProps);
	}

	public Collection<AgentProperties> getAgentProps() {
		return Collections.unmodifiableCollection(agentProps);
	}

	public JsonObject getPlayerProps() {
		return playerProps;
	}
	
	@Override
	public String toString() {
		return rawSpec.toString();
	}

}
