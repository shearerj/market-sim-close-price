package systemmanager;

import data.*;
import entity.Agent;
import event.TimeStamp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.*;
import org.json.simple.parser.*;


/**
 * Stores list of parameters used in the simulation_spec.json file.
 * 
 * NOTE: All MarketModel types in the spec file must match the 
 * corresponding class name.
 * 
 * @author ewah
 */
public class SimulationSpec {

	private Log log;
	private SystemData data;
	private JSONObject params;
	private JSONObject assignments;
	private JSONParser parser;
	
	public final static String ASSIGN_KEY = "assignment";
	public final static String CONFIG_KEY = "configuration";
	
	// Parameters in spec file
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
	
	/**
	 * Constructor
	 * @param file
	 * @param l
	 */
	public SimulationSpec(String file, Log l, SystemData d) {
		log = l;
		data = d;
		parser = new JSONParser();
		loadFile(file);
		readParams();
	}
	
	/**
	 * Load the simulation specification file.
	 * @param specFile
	 */
	public void loadFile(String specFile) {
		try {
			FileInputStream is = new FileInputStream(new File(specFile));
			InputStreamReader isr = new InputStreamReader(is);
			Object obj = parser.parse(isr);
			JSONObject array = (JSONObject) obj;
			assignments = (JSONObject) array.get(ASSIGN_KEY);
			params = (JSONObject) array.get(CONFIG_KEY);
			isr.close();
			is.close();
		} catch (IOException e) {
			System.err.println(this.getClass().getSimpleName() + 
					"::loadFile(String): error opening/processing spec file: " +
					specFile);
			e.printStackTrace();
		} catch (ParseException e) {
			System.err.println(this.getClass().getSimpleName() + 
					"::loadFile(String): JSON parsing error");
		}
	}
	
	
	/**
	 * Parses the spec file for config parameters. Overrides settings 
	 * in environment properties file & agent properties config.
	 */
	public void readParams() {
		
		data.simLength = new TimeStamp(Integer.parseInt(getValue(SIMULATION_LENGTH)));
		data.tickSize = Integer.parseInt(getValue(TICK_SIZE));	
		data.nbboLatency = new TimeStamp(Integer.parseInt(getValue(LATENCY)));
		data.arrivalRate = Double.parseDouble(getValue(ARRIVAL_RATE));
		data.reentryRate = Double.parseDouble(getValue(REENTRY_RATE));
		data.meanValue = Integer.parseInt(getValue(FUNDAMENTAL_MEAN));
		data.kappa = Double.parseDouble(getValue(FUNDAMENTAL_KAPPA));
		data.shockVar = Double.parseDouble(getValue(FUNDAMENTAL_SHOCK_VAR));
		data.pvVar = Double.parseDouble(getValue(PRIVATE_VALUE_VAR));
		data.primaryModelDesc = getValue(PRIMARY_MODEL);
		
		/*******************
		 * MARKET MODELS
		 *******************/
		for (String modelType : Consts.MARKETMODEL_TYPES) {
			// models here is a comma-separated list
			String models = getValue(modelType);
			if (models != null) {
				if (!models.isEmpty()) {
					if (models.endsWith(",")) {
						// remove any extra appended commas
						models = models.substring(0, models.length() - 1);
					}
					String[] configs = models.split("[,]+");
					
					if (configs.length > 1) {
						// if > 1, # model type = # of items in the list
						// check if there are NONE or 0 of this model
						data.numModelType.put(modelType, configs.length);
					} else if (!models.equals(Consts.MODEL_CONFIG_NONE) && 
							!models.equals("0")) {
						data.numModelType.put(modelType, configs.length);
					} else {
						data.numModelType.put(modelType, 0);
					}
				}
			}
		}
		
		/*******************
		 * CONFIGURATION - add environment agents
		 *******************/
		for (String agentType : Consts.SM_AGENT_TYPES) {
			String num = getValue(agentType);
			String setup = getValue(agentType + Consts.setupSuffix);
			if (num != null) {
				int n = Integer.parseInt(num);
				ObjectProperties op = getStrategyParameters(agentType, setup);
				AgentPropsPair a = new AgentPropsPair(agentType, op);
				data.addEnvAgentNumber(a, n);
			}
		}

		/*******************
		 * ASSIGNMENT - add players
		 *******************/
		for (String role : Consts.roles) {
			Object strats = assignments.get(role);
			if (strats != null) {			
				@SuppressWarnings("unchecked")
				ArrayList<String> strategies = (ArrayList<String>) strats;
				for (Iterator<String> it = strategies.iterator(); it.hasNext(); ) {
					String strat = it.next();
					if (!strat.equals("")) {
						String[] as = strat.split("[:]+");	// split on colon
						if (as.length != 2) {
							log.log(Log.ERROR, this.getClass().getSimpleName() + 
									"::setRolePlayers: " + "incorrect strategy string");
						} else {
							// first elt is agent type, second elt is strategy
							ObjectProperties op = getStrategyParameters(as[0], as[1]);
							data.addPlayerProperties(new AgentPropsPair(as[0], op));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Gets the value associated with a given key in the JSONObject.
	 * 
	 * @param key
	 * @return
	 */
	public String getValue(String key) {
		if (params.containsKey(key)) {
			return (String) params.get(key);
		} else {
			return null;
		}
	}

	/**
	 * Wrapper method because log is not static.
	 * 
	 * @param type
	 * @param strategy
	 * @return
	 */
	private ObjectProperties getStrategyParameters(String type, String strategy) {
		ObjectProperties op = SimulationSpec.getAgentProperties(type, strategy);
		
		if (op == null) {
			log.log(Log.ERROR, this.getClass().getSimpleName() + 
					"::getStrategyParameters: error parsing " + strategy.split("[_]+"));
		}
		return op;
	}
	
	
	/**
	 * Gets properties for an entity. Will overwrite default ObjectProperties set in
	 * Consts. If the entity type indicates that the entity is a player in a role, 
	 * this method parses the strategy, if any, in the simulation spec file.
	 *
	 * @param type
	 * @param strategy
	 * @return ObjectProperties
	 */
	public static ObjectProperties getAgentProperties(String type, String strategy) {
		ObjectProperties p = new ObjectProperties(Consts.getProperties(type));
		p.put(Agent.STRATEGY_KEY, strategy);
		
		if (strategy == null) return p;
		
		// Check that strategy is not blank
		if (!strategy.equals("") && !type.equals(Consts.DUMMY)) {
			String[] stratParams = strategy.split("[_]+");
			if (stratParams.length % 2 != 0) {
				return null;
			}
			for (int j = 0; j < stratParams.length; j += 2) {
				p.put(stratParams[j], stratParams[j+1]);
			}
		}
		return p;
	}
}
