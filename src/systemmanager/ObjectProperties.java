package systemmanager;

import java.util.HashMap;

import event.TimeStamp;

/**
 * This class stores the parameters for a specific agent, market, or market model.
 * Each sets defaults for its properties in its constructor or in the Consts class, 
 * but the default settings can be overridden by simulation specifications.
 * 
 * ObjectProperties class is a wrapper for HashMap<String,String>.
 * 
 * @author ewah
 */
public class ObjectProperties {

	private HashMap<String,String> properties;
	
	public ObjectProperties() {
		properties = new HashMap<String,String>();
	}
	
	public ObjectProperties(ObjectProperties p) {
		properties = (HashMap<String,String>) p.properties.clone();
	}
	
	public ObjectProperties(HashMap<String,String> hm) {
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
	
	@Override
	public int hashCode() {
		return properties.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
	        return true;
	    if (obj == null)
	        return false;
	    if (getClass() != obj.getClass())
	        return false;
	    final ObjectProperties other = (ObjectProperties) obj;
	    return this.properties.equals(other.properties);
	}
}
