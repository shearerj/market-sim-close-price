package systemmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.*;
import org.json.simple.parser.*;

import entity.Agent;
import event.TimeStamp;

/**
 * Stores list of web parameters used in EGTAOnline.
 * 
 * NOTE: All MarketModel types in the spec file must match the corresponding class name.
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
		} catch (IOException e) {
			log.log(Log.ERROR, this.getClass().getSimpleName() + 
					"::loadFile(String): error opening/processing spec file: " +
					specFile + "/" + e);
		} catch (ParseException e) {
			log.log(Log.ERROR, this.getClass().getSimpleName() + 
					"::loadFile(String): JSON parsing error: " + e);
		}
	}
	
	
	/**
	 * Parses the spec file for config parameters. Overrides settings in environment
	 * properties file & agent properties config.
	 */
	public void readParams() {
		
		data.simLength = new TimeStamp(Integer.parseInt(getValue("sim_length")));
		data.tickSize = Integer.parseInt(getValue("tick_size"));	
		data.nbboLatency = new TimeStamp(Integer.parseInt(getValue("nbbo_latency")));
		data.arrivalRate = Double.parseDouble(getValue("arrival_rate"));
		data.meanValue = Integer.parseInt(getValue("mean_value"));
		data.kappa = Double.parseDouble(getValue("kappa"));
		data.shockVar = Double.parseDouble(getValue("shock_var"));
		data.privateValueVar = Double.parseDouble(getValue("private_value_var"));
				
		// Model-specific parameters
		data.primaryModelDesc = getValue("primary_model");
		
		/*******************
		 * MARKET MODELS
		 *******************/
		for (int i = 0; i < Consts.MARKETMODEL_TYPES.length; i++) {
			// models here is a comma-separated list
			String models = getValue(Consts.MARKETMODEL_TYPES[i]);
			if (models != null) {
				if (models.endsWith(",")) {
					// remove any extra appended commas
					models = models.substring(0, models.length() - 1);
				}
				String[] configs = models.split("[,]+");
				
				if (configs.length > 1) {
					// if > 1, number of that model type is the number of items in the list
					// also must check that not indicating that there are NONE or 0 of this model
					data.numModelType.put(Consts.MARKETMODEL_TYPES[i], configs.length);
				} else if (!models.equals(Consts.MODEL_CONFIG_NONE) && !models.equals("0")) {
					data.numModelType.put(Consts.MARKETMODEL_TYPES[i], configs.length);
				} else {
					data.numModelType.put(Consts.MARKETMODEL_TYPES[i], 0);
				}
			}
		}
		
		/*******************
		 * CONFIGURATION - add environment agents
		 *******************/
		for (int i = 0; i < Consts.SM_AGENT_TYPES.length; i++) {
			String agentType = Consts.SM_AGENT_TYPES[i];
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
		for (int i = 0; i < Consts.roles.length; i++) {
			Object strats = assignments.get(Consts.roles[i]);
			if (strats != null) {			
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
