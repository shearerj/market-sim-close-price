package systemmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.*;
import org.json.simple.parser.*;

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
	
	private HashMap<String, ArrayList<AgentObjectPair>> roleStrategies;
	private ArrayList<AgentObjectPair> agents;

	/**
	 * Constructor
	 * @param file
	 * @param l
	 */
	public SimulationSpec(String file, Log l, SystemData d) {
		log = l;
		data = d;
		parser = new JSONParser();
		roleStrategies = new HashMap<String, ArrayList<AgentObjectPair>>();
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
			assignments = (JSONObject) array.get("assignment");
			params = (JSONObject) array.get("configuration");
		} catch (IOException e) {
			log.log(Log.ERROR, "loadFile(String): error opening/processing spec file: " +
					specFile + "/" + e);
		} catch (ParseException e) {
			log.log(Log.ERROR, "loadFile(String): JSON parsing error: " + e);
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
		// data.bidRange = Integer.parseInt(getValue("bid_range")); // for ZI set outside roles
		data.privateValueVar = Double.parseDouble(getValue("private_value_var"));
		
		// Market maker variables TODO - later remove?
		//data.marketmaker_sleepTime = Integer.parseInt(getValue("marketmaker_sleep_time"));
		//data.marketmaker_numRungs = Integer.parseInt(getValue("marketmaker_num_rungs"));
		//data.marketmaker_rungSize = Integer.parseInt(getValue("marketmaker_rung_size"));
		
		// Model-specific parameters
		data.primaryModelDesc = getValue("primary_model");
		
		// Check which types of market models to create
		for (int i = 0; i < Consts.modelTypeNames.length; i++) {
			// models here is a comma-separated list
			String models = getValue(Consts.modelTypeNames[i]);
			if (models != null) {
				if (models.endsWith(",")) {
					// remove any extra appended commas
					models = models.substring(0, models.length() - 1);
				}
				String[] configs = models.split("[,]+");
				
				if (configs.length > 1) {
					// if > 1, number of that model type is the number of items in the list
					// also must check that not indicating that there are NONE or 0 of this model
					data.numModelType.put(Consts.modelTypeNames[i], configs.length);
				} else if (!models.equals(Consts.MODEL_CONFIG_NONE) && !models.equals("0")) {
					data.numModelType.put(Consts.modelTypeNames[i], configs.length);
				} else {
					data.numModelType.put(Consts.modelTypeNames[i], 0);
				}
			}
		}
			
		// Check which types of single-market agents to create (configuration part of spec file)
		// These agents are NOT considered players in the game.
		for (int i = 0; i < Consts.SMAgentTypes.length; i++) {
			String agentType = Consts.SMAgentTypes[i];
			String num = getValue(agentType);
			String setup = getValue(agentType + "_setup"); // setup string for this agent type
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numEnvironmentAgents += n;
				data.numAgentType.put(agentType, n);
				ObjectProperties op = getEntityProperties(agentType, setup);
				agents.add(new AgentObjectPair(agentType, op));
			}
		}

		// Check how many players in a given role (from role part of spec file)
		// Strategies for each player will be set in SystemSetup.
		for (int i = 0; i < Consts.roles.length; i++) {
			Object strats = assignments.get(Consts.roles[i]);
			if (strats != null) {			
				ArrayList<String> strategies = (ArrayList<String>) strats;
				for (Iterator<String> it = strategies.iterator(); it.hasNext(); ) {
					String strat = it.next();
					if (!strat.equals("")) {
						// split on semicolon
						String[] as = strat.split("[:]+");
						if (as.length != 2) {
							log.log(Log.ERROR, "SimulationSpec::setRolePlayers: incorrect strategy string");
						} else if (as[0] != null) {
							// first elt is agent type, second elt is strategy
							ObjectProperties op = getEntityProperties(as[0], as[1]);
							agents.add(new AgentObjectPair(as[0], op));
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
	 * @return agents
	 */
	public ArrayList<AgentObjectPair> getAgentList() {
		return agents;
	}
	
	
	/**
	 * Add to count of number of each agent type in SystemData object.
	 * 
	 * @param type
	 */
	private void addAgentType(String type) {
		// add to existing if exists (in data.numAgentType)
		// otherwise insert new value
		if (data.numAgentType.containsKey(type)) {
			int cnt = data.numAgentType.get(type);
			data.numAgentType.put(type, ++cnt);
		} else {
			data.numAgentType.put(type, 1);
		}
	}

	
	/**
	 * Gets properties for an entity. Will overwrite default EntityProperties set in
	 * Consts. If the entity type indicates that the entity is a player in a role, 
	 * this method parses the strategy, if any, in the simulation spec file.
	 * The index is used to select the player from the list in the spec file.
	 *
	 * @param type
	 * @param strategy
	 * @return ObjectProperties
	 */
	public ObjectProperties getEntityProperties(String type, String strategy) {
		ObjectProperties p = new ObjectProperties(Consts.getProperties(type));
			
		p.put("strategy", strategy);
		
		// Check that strategy is not blank
		if (!strategy.equals("") && !type.equals("DUMMY")) {
			String[] stratParams = strategy.split("[_]+");
			if (stratParams.length % 2 != 0) {
				log.log(Log.ERROR, "SimulationSpec::getEntityProperties: error parsing strategy " + stratParams);
				return null;
			}
			for (int j = 0; j < stratParams.length; j += 2) {
				p.put(stratParams[j], stratParams[j+1]);
			}
		}
		log.log(Log.INFO, type + ": " + p);
		return p;
	}
}
