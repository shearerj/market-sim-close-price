package systemmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

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

	protected Log log;
	protected Gson gson;

	public final static String ASSIGN_KEY = "assignment";
	public final static String CONFIG_KEY = "configuration";

	public SimulationSpec2(Log l) {
		log = l;
		gson = new Gson();
	}
	
	public void loadFile(String specFileName, SystemData data) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		loadFile(new File(specFileName), data);
	}

	public void loadFile(File specFile, SystemData data)
			throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		JsonObject root = gson.fromJson(new FileReader(specFile),
				JsonObject.class);
		JsonObject assignments = root.getAsJsonObject(ASSIGN_KEY);
		JsonObject config = root.getAsJsonObject(CONFIG_KEY);

		params(config, data);
		marketModels(config, data);
		backgroundAgents(config, data);
		players(assignments, data);
	}

	protected void params(JsonObject config, SystemData data) {
		data.simLength = new TimeStamp(config.getAsJsonPrimitive("sim_length")
				.getAsLong());
		data.tickSize = config.getAsJsonPrimitive("tick_size").getAsInt();
		data.nbboLatency = new TimeStamp(config.getAsJsonPrimitive(
				"nbbo_latency").getAsLong());
		data.arrivalRate = config.getAsJsonPrimitive("arrival_rate")
				.getAsDouble();
		data.reentryRate = config.getAsJsonPrimitive("reentry_rate")
				.getAsDouble();
		data.meanValue = config.getAsJsonPrimitive("mean_value").getAsInt();
		data.kappa = config.getAsJsonPrimitive("kappa").getAsDouble();
		data.shockVar = config.getAsJsonPrimitive("shock_var").getAsDouble();
		data.pvVar = config.getAsJsonPrimitive("private_value_var")
				.getAsDouble();

		// Model-specific parameters
		JsonPrimitive primaryModel = config.getAsJsonPrimitive("primary_model");
		data.primaryModelDesc = primaryModel == null ? null : primaryModel
				.getAsString();
	}

	protected void marketModels(JsonObject config, SystemData data) {
		for (String type : Consts.MARKETMODEL_TYPES) {
			// models here is a comma-separated list
			JsonPrimitive modelsTest = config.getAsJsonPrimitive(type);
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

	protected void backgroundAgents(JsonObject config, SystemData data) {
		for (String agentType : Consts.SM_AGENT_TYPES) {
			JsonPrimitive numJson = config.getAsJsonPrimitive(agentType);
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

	protected void players(JsonObject assignments, SystemData data) {
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
					log.log(Log.ERROR, this.getClass().getSimpleName()
							+ "::setRolePlayers: "
							+ "incorrect strategy string");
				} else {
					// first elt is agent type, second elt is strategy
					ObjectProperties op = getStrategyParameters(as[0], as[1]);
					data.addPlayerProperties(new AgentPropsPair(as[0], op));
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
	protected ObjectProperties getStrategyParameters(String type,
			String strategy) {
		ObjectProperties op = new ObjectProperties(Consts.getProperties(type));
		op.put(Agent.STRATEGY_KEY, strategy);

		// Check that strategy is not blank
		if (!strategy.equals("") && !type.equals(Consts.DUMMY)) {
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
