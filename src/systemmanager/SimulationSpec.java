package systemmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
	private JSONObject roleStrategies;
	private JSONParser parser;

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
			roleStrategies = (JSONObject) array.get("assignment");
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
		data.bidRange = Integer.parseInt(getValue("bid_range"));
		data.privateValueVar = Double.parseDouble(getValue("private_value_var"));
		
		// Market maker variables
		data.marketmaker_sleepTime = Integer.parseInt(getValue("marketmaker_sleep_time"));
		data.marketmaker_numRungs = Integer.parseInt(getValue("marketmaker_num_rungs"));
		data.marketmaker_rungSize = Integer.parseInt(getValue("marketmaker_rung_size"));
		
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
			
		// Check which types of single-market agents to create
		// (from configuration section of spec file)
		for (int i = 0; i < Consts.SMAgentTypes.length; i++) {
			String agentType = Consts.SMAgentTypes[i];
			String num = getValue(agentType);
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numAgents += n;
				data.numAgentType.put(agentType, n);
			}
		}
//		// Check which types of multi-market agents to create
//		// (from configuration section of spec file)
//		for (int i = 0; i < Consts.HFTAgentTypes.length; i++) {
//			String agentType = Consts.HFTAgentTypes[i];
//			String num = getValue(agentType);
//			if (num != null) {
//				int n = Integer.parseInt(num);
//				data.numAgents += n;
//				data.numAgentType.put(agentType, n);
//			}
//		}
		// Check how many agents in a given role 
		// (from role part of spec file)
		for (int i = 0; i < Consts.roles.length; i++) {
			String role = Consts.roles[i];
			data.numAgentType.put(role, getNumPlayers(role));
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
	 * Get number of players for a given role.
	 * 
	 * @param role
	 * @return
	 */
	public int getNumPlayers(String role) {
		Object strats = roleStrategies.get(role);
		if (strats != null) {
			return ((ArrayList<String>) strats).size();
		}
		return 0;
	}
	
	
	/**
	 * @return roleStrategies JSONObject
	 */
	public JSONObject getRoleStrategies() {
		return roleStrategies;
	}
	
}
