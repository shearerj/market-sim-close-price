package systemmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.ModelType;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import data.AgentProperties;
import data.EntityProperties;
import data.Keys;
import data.ModelProperties;

/**
 * Stores list of web parameters used in EGTAOnline.
 * 
 * NOTE: All MarketModel types in the spec file must match the corresponding
 * class name.
 * 
 * @author ewah
 */
public class SimulationSpec {

	protected static final String[] simulationKeys = { Keys.SIMULATION_LENGTH,
			Keys.FUNDAMENTAL_MEAN, Keys.FUNDAMENTAL_KAPPA, Keys.FUNDAMENTAL_SHOCK_VAR,
			Keys.RAND_SEED };
	protected static final String[] modelKeys = { Keys.NBBO_LATENCY, Keys.ARRIVAL_RATE, Keys.MARKET_LATENCY, Keys.TICK_SIZE };
	protected static final String[] agentKeys = { Keys.TICK_SIZE, Keys.ARRIVAL_RATE,
			Keys.REENTRY_RATE, Keys.PRIVATE_VALUE_VAR };

	protected final EntityProperties simulationProperties;
	protected final EntityProperties defaultModelProperties;
	protected final EntityProperties defaultAgentProperties;

	protected final Collection<ModelProperties> models;
	protected final Map<AgentProperties, Integer> backgroundAgents;
	protected final JsonObject playerConfig;

	public SimulationSpec(File specFile) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		JsonObject spec = new Gson().fromJson(new FileReader(specFile),
				JsonObject.class);
		JsonObject config = spec.getAsJsonObject(Keys.CONFIG);

		simulationProperties = readProperties(config, simulationKeys);
		defaultModelProperties = readProperties(config, modelKeys);
		defaultAgentProperties = readProperties(config, agentKeys);

		models = marketModels(config, defaultModelProperties);
		backgroundAgents = backgroundAgents(config, defaultAgentProperties);
		playerConfig = spec.getAsJsonObject(Keys.ASSIGN);
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

	protected Collection<ModelProperties> marketModels(
			JsonObject config, EntityProperties def) {
		Collection<ModelProperties> models = new ArrayList<ModelProperties>();

		for (ModelType type : ModelType.values()) {
			// models here is a comma-separated list
			JsonPrimitive modelsTest = config.getAsJsonPrimitive(type.toString());
			if (modelsTest == null) continue;
			String modelSpec = modelsTest.getAsString();
			String[] configs = modelSpec.split(",+");

			for (String configString : configs)
				models.add(new ModelProperties(type, def, configString));
		}
		return models;
	}

	protected Map<AgentProperties, Integer> backgroundAgents(JsonObject config,
			EntityProperties def) {
		Map<AgentProperties, Integer> backgroundAgents = new HashMap<AgentProperties, Integer>();

		for (AgentType agentType : Consts.AgentType.values()) {
			JsonPrimitive numJson = config.getAsJsonPrimitive(agentType.toString());
			JsonPrimitive setupJson = config.getAsJsonPrimitive(agentType
					+ Consts.SETUP_SUFFIX);
			if (numJson == null) continue;

			AgentProperties props = new AgentProperties(agentType, def,
					setupJson.getAsString());
			backgroundAgents.put(props, numJson.getAsInt());
		}
		return backgroundAgents;
	}

	public EntityProperties getSimulationProperties() {
		return simulationProperties;
	}
	
	public EntityProperties getDefaultModelProperties() {
		return defaultModelProperties;
	}
	
	public EntityProperties getDefaultAgentProperties() {
		return defaultAgentProperties;
	}

	public Collection<ModelProperties> getModels() {
		return Collections.unmodifiableCollection(models);
	}

	public Map<AgentProperties, Integer> getBackgroundAgents() {
		return Collections.unmodifiableMap(backgroundAgents);
	}

	public JsonObject getPlayerConfig() {
		return playerConfig;
	}
	
	public JsonObject toJson() {
		JsonObject config = new JsonObject();
		
		for (String key : simulationProperties.keys())
			config.addProperty(key, simulationProperties.getAsString(key));
		for (String key : defaultModelProperties.keys())
			config.addProperty(key, defaultModelProperties.getAsString(key));
		for (String key : defaultAgentProperties.keys())
			config.addProperty(key, defaultAgentProperties.getAsString(key));

		for (Entry<AgentProperties, Integer> agentProps : getBackgroundAgents().entrySet()) {
			AgentProperties props = agentProps.getKey();
			int number = agentProps.getValue();

			config.addProperty(props.getAgentType().toString(), number);
			config.addProperty(props.getAgentType() + Consts.SETUP_SUFFIX,
					props.toConfigString());
		}
		return config;
	}
	
	@Override
	public String toString() {
		return toJson().toString();
	}

}
