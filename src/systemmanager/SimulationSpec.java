package systemmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.ModelType;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import data.AgentProperties;
import data.ModelProperties;
import data.EntityProperties;

/**
 * Stores list of web parameters used in EGTAOnline.
 * 
 * NOTE: All MarketModel types in the spec file must match the corresponding
 * class name.
 * 
 * @author ewah
 */
public class SimulationSpec {

	public final static String ASSIGN_KEY = "assignment";
	public final static String CONFIG_KEY = "configuration";

	public final static String SIMULATION_LENGTH = "sim_length";
	public final static String TICK_SIZE = "tick_size";
	public final static String LATENCY = "nbbo_latency";
	public final static String ARRIVAL_RATE = "arrival_rate";
	public final static String REENTRY_RATE = "reentry_rate";
	public final static String FUNDAMENTAL_MEAN = "mean_value";
	public final static String FUNDAMENTAL_KAPPA = "kappa";
	public final static String FUNDAMENTAL_SHOCK_VAR = "shock_var";
	public final static String PRIVATE_VALUE_VAR = "private_value_var";
	public final static String PRIMARY_MODEL = "primary_model";
	public final static String RAND_SEED = "random_seed";

	protected static final String[] simulationKeys = { SIMULATION_LENGTH,
			FUNDAMENTAL_MEAN, FUNDAMENTAL_KAPPA, FUNDAMENTAL_SHOCK_VAR,
			RAND_SEED };
	protected static final String[] modelKeys = { LATENCY };
	protected static final String[] agentKeys = { TICK_SIZE, ARRIVAL_RATE,
			REENTRY_RATE, "private_value_var" };

	// XXX Move into model properties?
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
		JsonObject config = spec.getAsJsonObject(CONFIG_KEY);

		simulationProperties = readProperties(config, simulationKeys);
		defaultModelProperties = readProperties(config, modelKeys);
		defaultAgentProperties = readProperties(config, agentKeys);

		models = marketModels(config, defaultModelProperties);
		backgroundAgents = backgroundAgents(config, defaultAgentProperties);
		playerConfig = spec.getAsJsonObject(ASSIGN_KEY);
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

	protected static Collection<ModelProperties> marketModels(
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

}
