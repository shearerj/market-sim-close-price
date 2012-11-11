package systemmanager;

import java.util.HashMap;

/**
 * This class stores the parameters for a specific agent. Each agent sets defaults
 * for each of these parameters in their constructor, but they can be overridden
 * by simulation specifications.
 * 
 * EntityProperties class is a wrapper for HashMap<String,String>.
 * 
 * @author ewah
 */
public class EntityProperties {

	private HashMap<String,String> properties;
	
	public EntityProperties() {
		properties = new HashMap<String,String>();
	}
	
	public EntityProperties(EntityProperties ep) {
		properties = (HashMap<String,String>) ep.properties.clone();
	}
	
	public EntityProperties(HashMap<String,String> hm) {
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
