package systemmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import systemmanager.Consts.AgentType;
import systemmanager.Consts.ModelType;

import logger.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import data.AgentPropsPair;
import data.ObjectProperties;
import data.SystemData;

import entity.Agent;
import event.TimeStamp;

/**
 * Stores list of web parameters used in EGTAOnline.
 * 
 * NOTE: All MarketModel types in the spec file must match the corresponding
 * class name.
 * 
 * @author ewah
 */
public class SimulationSpec2 {

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

	protected final JsonObject spec;

	public SimulationSpec2(File specFile) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		spec = new Gson().fromJson(new FileReader(specFile), JsonObject.class);
	}

	public SimulationSpec2(String specFileName) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		this(new File(specFileName));
	}

	public void writeData(SystemData data) {
		params(data);
		marketModels(data);
		backgroundAgents(data);
		players(data);
	}

	protected void params(SystemData data) {
		JsonObject config = spec.getAsJsonObject(CONFIG_KEY);

		data.simLength = new TimeStamp(
				config.getAsJsonPrimitive("sim_length").getAsLong());
		data.tickSize = config.getAsJsonPrimitive("tick_size").getAsInt();
		data.nbboLatency = new TimeStamp(config.getAsJsonPrimitive(
				"nbbo_latency").getAsLong());
		data.arrivalRate = config.getAsJsonPrimitive("arrival_rate").getAsDouble();
		data.reentryRate = config.getAsJsonPrimitive("reentry_rate").getAsDouble();
		data.meanValue = config.getAsJsonPrimitive("mean_value").getAsInt();
		data.kappa = config.getAsJsonPrimitive("kappa").getAsDouble();
		data.shockVar = config.getAsJsonPrimitive("shock_var").getAsDouble();
		data.pvVar = config.getAsJsonPrimitive("private_value_var").getAsDouble();

		// Model-specific parameters
		JsonPrimitive primaryModel = config.getAsJsonPrimitive("primary_model");
		data.primaryModelDesc = primaryModel == null ? null
				: primaryModel.getAsString();
	}

	protected void marketModels(SystemData data) {
		JsonObject config = spec.getAsJsonObject(CONFIG_KEY);

		for (ModelType type : ModelType.values()) {
			// models here is a comma-separated list
			JsonPrimitive modelsTest = config.getAsJsonPrimitive(type.toString());
			if (modelsTest == null)
				continue;
			String models = modelsTest.getAsString();
			String[] configs = models.split(",+");

			if (configs.length > 1) {
				// if > 1, number of that model type is the number of items in
				// the list also must check that not indicating that there are
				// NONE or 0 of this model
				data.numModelType.put(type, configs.length);
			} else if (!models.equals(Consts.MODEL_CONFIG_NONE)
					&& !models.equals("0")) {
				data.numModelType.put(type, configs.length);
			} else {
				data.numModelType.put(type, 0);
			}
		}
	}

	protected void backgroundAgents(SystemData data) {
		JsonObject config = spec.getAsJsonObject(CONFIG_KEY);

		for (AgentType agentType : Consts.SM_AGENT) {
			JsonPrimitive numJson = config.getAsJsonPrimitive(agentType.toString());
			JsonPrimitive setupJson = config.getAsJsonPrimitive(agentType
					+ Consts.setupSuffix);
			if (numJson == null)
				continue;

			ObjectProperties op = getStrategyParameters(agentType,
					setupJson.getAsString());
			AgentPropsPair a = new AgentPropsPair(agentType, op);
			data.addEnvAgentNumber(a, numJson.getAsInt());
		}
	}

	protected void players(SystemData data) {
		JsonObject assignments = spec.getAsJsonObject(ASSIGN_KEY);

		for (String role : Consts.roles) {
			JsonArray strats = assignments.getAsJsonArray(role);
			if (strats == null)
				continue;

			for (JsonElement stratE : strats) {
				String strat = stratE.getAsString();
				if (strat.isEmpty())
					continue;
				String[] as = strat.split(":+"); // split on colon
				if (as.length != 2) {
					Logger.log(Logger.ERROR, this.getClass().getSimpleName()
							+ "::setRolePlayers: "
							+ "incorrect strategy string");
				} else {
					// first elt is agent type, second elt is strategy
					AgentType type = AgentType.valueOf(as[0]);
					ObjectProperties op = getStrategyParameters(type, as[1]);
					data.addPlayerProperties(new AgentPropsPair(type, op));
				}

			}
		}
	}

	/**
	 * Wrapper method because log is not static.
	 * 
	 * @param type
	 * @param strategy
	 * @return
	 */
	protected ObjectProperties getStrategyParameters(AgentType type,
			String strategy) {
		ObjectProperties op = new ObjectProperties(Consts.getProperties(type));
		op.put(Agent.STRATEGY_KEY, strategy);

		// Check that strategy is not blank
		if (!strategy.equals("") && !type.equals(Consts.AgentType.DUMMY)) {
			String[] stratParams = strategy.split("[_]+");
			if (stratParams.length % 2 != 0) {
				return null;
			}
			for (int j = 0; j < stratParams.length; j += 2) {
				op.put(stratParams[j], stratParams[j + 1]);
			}
		}
		return op;
	}

}
