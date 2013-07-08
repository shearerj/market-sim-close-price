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

	// XXX Move into model properties?
	protected final EntityProperties simulationProperties;
	protected final Collection<ModelProperties> models;
	protected final Map<AgentProperties, Integer> backgroundAgents;
	protected final JsonObject playerConfig;

	public SimulationSpec(File specFile) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		JsonObject spec = new Gson().fromJson(new FileReader(specFile), JsonObject.class);
		JsonObject config = spec.getAsJsonObject(CONFIG_KEY);
		
		simulationProperties = simulationProperties(config);
		models = marketModels(config, systemModelProperties(config));
		backgroundAgents = backgroundAgents(config, systemAgentProperties(config));
		playerConfig = spec.getAsJsonObject(ASSIGN_KEY);
	}

	public SimulationSpec(String specFileName) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		this(new File(specFileName));
	}
	
	protected static EntityProperties simulationProperties(JsonObject config) {
		EntityProperties simProps = new EntityProperties();
		simProps.put(SIMULATION_LENGTH, config.getAsJsonPrimitive(SIMULATION_LENGTH).getAsLong());
		simProps.put(FUNDAMENTAL_MEAN, config.getAsJsonPrimitive(FUNDAMENTAL_MEAN).getAsInt());
		simProps.put(FUNDAMENTAL_KAPPA, config.getAsJsonPrimitive(FUNDAMENTAL_KAPPA).getAsDouble());
		simProps.put(FUNDAMENTAL_SHOCK_VAR, config.getAsJsonPrimitive(FUNDAMENTAL_SHOCK_VAR).getAsDouble());
		simProps.put(RAND_SEED, config.getAsJsonPrimitive(RAND_SEED).getAsLong());
		
		// TODO Find location
//		data.pvVar = config.getAsJsonPrimitive("private_value_var").getAsDouble();
		
		return simProps;
	}
	
	protected static EntityProperties systemModelProperties(JsonObject config) {
		EntityProperties modelProps = new EntityProperties();
		modelProps.put(LATENCY, config.getAsJsonPrimitive(
				LATENCY).getAsLong());
		return modelProps;
	}
	
	protected static EntityProperties systemAgentProperties(JsonObject config) {
		EntityProperties agentProps = new EntityProperties();
		agentProps.put(TICK_SIZE, config.getAsJsonPrimitive(TICK_SIZE).getAsInt());
		agentProps.put(ARRIVAL_RATE, config.getAsJsonPrimitive(ARRIVAL_RATE).getAsDouble());
		agentProps.put(REENTRY_RATE, config.getAsJsonPrimitive(REENTRY_RATE).getAsDouble());
		return agentProps;
	}

	protected static Collection<ModelProperties> marketModels(JsonObject config, EntityProperties def) {
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

	protected Map<AgentProperties, Integer> backgroundAgents(JsonObject config, EntityProperties def) {
		Map<AgentProperties, Integer> backgroundAgents = new HashMap<AgentProperties, Integer>();

		for (AgentType agentType : Consts.SM_AGENT) {
			JsonPrimitive numJson = config.getAsJsonPrimitive(agentType.toString());
			JsonPrimitive setupJson = config.getAsJsonPrimitive(agentType
					+ Consts.setupSuffix);
			if (numJson == null) continue;

			AgentProperties props = new AgentProperties(agentType, def, setupJson.getAsString());
			backgroundAgents.put(props, numJson.getAsInt());
		}
		return backgroundAgents;
	}

	public EntityProperties getSimulationProperties() {
		return simulationProperties;
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

	// TODO Implement Players
//	protected void players(SystemData data) {
//		JsonObject assignments = spec.getAsJsonObject(ASSIGN_KEY);
//
//		String primaryModel;
//		for (String role : Consts.roles) {
//			JsonArray strats = assignments.getAsJsonArray(role);
//			if (strats == null)
//				continue;
//
//			for (JsonElement stratE : strats) {
//				String strat = stratE.getAsString();
//				if (strat.isEmpty())
//					continue;
//				String[] as = strat.split(":+"); // split on colon
//				if (as.length != 2) {
//					Logger.log(Logger.ERROR, this.getClass().getSimpleName()
//							+ "::setRolePlayers: "
//							+ "incorrect strategy string");
//				} else {
//					// first elt is agent type, second elt is strategy
//					AgentType type = AgentType.valueOf(as[0]);
//					ObjectProperties op = getStrategyParameters(type, as[1]);
//					data.addPlayerProperties(new AgentPropsPair(type, op));
//				}
//
//			}
//		}
//	}

}
