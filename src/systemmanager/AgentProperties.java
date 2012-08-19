package systemmanager;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores the parameters for a specific agent. Each agent sets defaults
 * for each of these parameters in their constructor, but they can be overridden
 * by simulation specifications.
 * 
 * AgentProperties class is a wrapper for HashMap<String,String>.
 * 
 * @author ewah
 */
public class AgentProperties {

	private HashMap<String,String> properties;
	
	public AgentProperties() {
		properties = new HashMap<String,String>();
	}
	
	public AgentProperties(AgentProperties ap) {
		properties = (HashMap<String,String>) ap.properties.clone();
	}
	
	public AgentProperties(HashMap<String,String> hm) {
		properties = new HashMap<String,String>(hm);
	}
	
	/**
	 * Checks if properties contains a key.
	 * 
	 * @param key
	 * @return
	 */
	public boolean containsKey(String key) {
		return properties.containsKey(key);
	}
	
	/**
	 * Set a property.
	 * @param key
	 * @param val
	 */
	public void put(String key, String val) {
		properties.put(key, val);
	}
	
	/**
	 * Get property's value.
	 * @param key
	 * @return String
	 */
	public String get(String key) {
		return properties.get(key);
	}

	
	public String toString() {
		return properties.toString();
	}
}
