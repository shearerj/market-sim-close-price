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
		data.meanPV = Integer.parseInt(getValue("mean_PV"));
		data.kappa = Double.parseDouble(getValue("kappa"));
		data.shockVar = Double.parseDouble(getValue("shock_var"));
		data.expireRate = Double.parseDouble(getValue("expire_rate"));
		data.bidRange = Integer.parseInt(getValue("bid_range"));
		data.privateValueVar = Double.parseDouble(getValue("private_value_var"));
		
		// Model-specific parameters
		data.centralCallClearFreq = new TimeStamp(Integer.parseInt(getValue("CENTRALCALL_clear_freq")));
		data.centralCallClearFreq = data.nbboLatency;
		
		// Check which types of market models to create
		for (int i = 0; i < Consts.modelTypeNames.length; i++) {
			// models here is a comma-separated list
			String models = getValue(Consts.modelTypeNames[i]);
			if (models != null) {
				if (models.endsWith(",")) {
					// remove any extra appended commas
					models = models.substring(0, models.length() - 1);
				}
				String[] settings = models.split("[,]+");
				// number of that model type is the number of items in the list
				data.numModelType.put(Consts.modelTypeNames[i], settings.length);
			}
		}
		
//		// Check which types of markets to create
//		for (int i = 0; i < Consts.marketTypeNames.length; i++) {
//			String num = getValue(Consts.marketTypeNames[i]);
//			if (num != null) {
//				int n = Integer.parseInt(num);
//				data.numMarkets += n;
//				data.numMarketType.put(Consts.marketTypeNames[i], n);
//			}
//		}
//		// Create the central market; check first if valid market type
//		if (data.useCentralMarket()) {
//			data.numMarketType.put(Consts.CENTRAL + "_CDA", 1);
//			data.numMarketType.put(Consts.CENTRAL + "_CALL", 1);
////			data.clearFreq = data.nbboLatency;
//		}
		
		// Check which types of agents to create
		for (int i = 0; i < Consts.agentTypeNames.length; i++) {
			String num = getValue(Consts.agentTypeNames[i]);
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numAgents += n;
				data.numAgentType.put(Consts.agentTypeNames[i], n);
			}
		}
		// Check how many agents in a given role (from role part of spec file)
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
		if (roleStrategies.get(role) != null) {
			return ((ArrayList<String>) roleStrategies.get(role)).size();
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