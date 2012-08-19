package systemmanager;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.Writer;
import org.json.simple.*;

/**
 * Contains payoff data/features for all players in the simulation.
 *  
 * @author ewah
 */
public class Observations {

	private HashMap<String,Object> observations;
	private SystemData data;
	
	/**
	 * Constructor
	 */
	public Observations(SystemData d) {
		observations = new HashMap<String,Object>();
		data = d;
	}
	
	
	/**
	 * Gets the agent's observation and adds to the hashmap container.
	 * @param agentID
	 */
	public void addObservation(int agentID) {
		HashMap<String,Object> obs = data.getAgent(agentID).getObservation();
		
		// Don't add observation if agent is not a player in the game
		if (obs == null) return;
		
		if (!observations.containsKey("players")) {
			ArrayList<Object> array = new ArrayList<Object>();
			array.add(obs);
			observations.put("players", array);
		} else {
			((ArrayList<Object>) observations.get("players")).add(obs);
		}
	}

	/**
	 * Adds a feature // TODO - add to Agent class?
	 * @param agentID
	 * @param ft
	 */
	public void addFeature(int agentID, HashMap<String,Object> ft) {
		if (!observations.containsKey("features")) {
			ArrayList<Object> array = new ArrayList<Object>();
			array.add(ft);
			observations.put("features", array);
		} else {
			((ArrayList<Object>) observations.get("features")).add(ft);
		}
	}
	
	/**
	 * Writes observations to the JSON file.
	 */
	public String generateObservationFile() {
		try {
			return JSONObject.toJSONString(observations);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
