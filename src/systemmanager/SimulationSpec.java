package systemmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.json.simple.*;
import org.json.simple.parser.*;

import event.TimeStamp;

/**
 * Stores list of web parameters used in EGTAOnline.
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
			log.log(Log.ERROR, "loadFile(String): error opening/processing simulation spec file: " +
					specFile + "/" + e);
		} catch (ParseException e) {
			log.log(Log.ERROR, "loadFile(String): JSON parsing error: " + e);
		}
	}
	
	
	/**
	 * Parses the spec file for config parameters. Overrides settings in environment
	 * properties file & agent properties config.
	 */
	public void configParams() {
		
		data.simLength = new TimeStamp(Integer.parseInt(getValue("sim_length")));
		data.nbboLatency = new TimeStamp(Integer.parseInt(getValue("nbbo_latency")));
		
		// Check which types of markets to create
		for (int i = 0; i < SystemConsts.marketTypes.length; i++) {
			String num = getValue(SystemConsts.marketTypes[i]);
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numMarkets += n;
				data.numMarketType.put(SystemConsts.marketTypes[i], n);
			}
		}
		// Check which types of agents to create
		for (int i = 0; i < SystemConsts.agentTypes.length; i++) {
			String num = getValue(SystemConsts.agentTypes[i]);
			if (num != null) {
				int n = Integer.parseInt(num);
				data.numAgents += n;
				data.numAgentType.put(SystemConsts.agentTypes[i], n);
			}
		}
		// Check how many agents in a given role (from spec file)
		for (int i = 0; i < SystemConsts.roles.length; i++) {
			String role = SystemConsts.roles[i];
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
		return ((ArrayList<String>) roleStrategies.get(role)).size();
	}
	
	
	/**
	 * Set strategy for one player in a given role. Overwrites the default
	 * AgentProperties set in SystemConsts class.
	 *
	 * @param role	agent who is a player
	 * @param idx	index of the strategy (of the specific role) to set
	 * @return ap	AgentProperties customized based on the player
	 */
	public AgentProperties setStrategy(String role, int idx) {
		if (roleStrategies.containsKey(role)) {
			AgentProperties ap = new AgentProperties(SystemConsts.getProperties(role));
			
			ArrayList<String> players = (ArrayList<String>) roleStrategies.get(role);
			String strategy = players.get(idx);
			ap.put("strategy", strategy);
			
			String[] stratParams = strategy.split("[_]+");
			if (stratParams.length % 2 != 0) {
				log.log(Log.ERROR, "setStrategy: error with describing the strategy");
				return null;
			}
			for (int j = 0; j < stratParams.length; j += 2) {
				ap.put(stratParams[j], stratParams[j+1]);
			}
			log.log(Log.INFO, role + ": " + ap);
			return ap;
		} else {
			return new AgentProperties(SystemConsts.getProperties(role));
		}
	}
	
	
	/**
	 * @return roleStrategies JSONObject
	 */
	public JSONObject getRoleStrategies() {
		return roleStrategies;
	}
	
}