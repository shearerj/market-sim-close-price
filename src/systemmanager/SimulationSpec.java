package systemmanager;

import static systemmanager.Keys.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.MarketType;

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
			RAND_SEED, NBBO_LATENCY };
	protected static final String[] marketKeys = { MARKET_LATENCY, TICK_SIZE };
	protected static final String[] agentKeys = { TICK_SIZE, ARRIVAL_RATE,
			REENTRY_RATE, PRIVATE_VALUE_VAR };

	protected final EntityProperties simulationProperties;
	protected final EntityProperties defaultMarketProperties;
	protected final EntityProperties defaultAgentProperties;

	protected final Collection<MarketProperties> marketConfigs;
	protected final Collection<AgentProperties> agentConfigs;
	protected final JsonObject playerConfig; // TODO Change to properties object

	public SimulationSpec(File specFile) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		JsonObject spec = new Gson().fromJson(new FileReader(specFile),
				JsonObject.class);
		JsonObject config = spec.getAsJsonObject(Keys.CONFIG);
		JsonObject players = spec.getAsJsonObject(Keys.ASSIGN);

		simulationProperties = readProperties(config, simulationKeys);
		defaultMarketProperties = readProperties(config, marketKeys);
		defaultAgentProperties = readProperties(config, agentKeys);

		marketConfigs = markets(config, defaultMarketProperties);
		agentConfigs = agents(config, defaultAgentProperties);
		playerConfig = players;
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

	public EntityProperties getSimulationConfig() {
		return simulationProperties;
	}
	
	public EntityProperties getDefaultMarketConfig() {
		return defaultMarketProperties;
	}
	
	public EntityProperties getDefaultAgentConfig() {
		return defaultAgentProperties;
	}

	public Collection<MarketProperties> getMarketConfigs() {
		return Collections.unmodifiableCollection(marketConfigs);
	}

	public Collection<AgentProperties> getAgentConfigs() {
		return Collections.unmodifiableCollection(agentConfigs);
	}

	public JsonObject getPlayerConfig() {
		return playerConfig;
	}
	
	public JsonObject toJson() {
		JsonObject config = new JsonObject();
		
		for (String key : simulationProperties.keys())
			config.addProperty(key, simulationProperties.getAsString(key));
		for (String key : defaultMarketProperties.keys())
			config.addProperty(key, defaultMarketProperties.getAsString(key));
		for (String key : defaultAgentProperties.keys())
			config.addProperty(key, defaultAgentProperties.getAsString(key));
		for (AgentProperties props : getAgentConfigs())
			config.addProperty(props.getAgentType().toString(),
					props.toConfigString());
		return config;
	}
	
	@Override
	public String toString() {
		return toJson().toString();
	}

}
