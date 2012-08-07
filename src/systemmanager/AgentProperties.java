package systemmanager;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores the parameters that are not set in a
 * configuration file, but which may be changed at each runtime.
 * 
 * Allows setting all agent parameters from a single location.
 * 
 * Parameters:
 * 	- sleepTime		how long agent sleeps between each strategy call
 * 	- sleepVar		the variance of the sleep time (Normal RV)
 * 
 * @author ewah
 */
public class AgentProperties {

	private HashMap<String,HashMap<String,String>> properties;
	public HashMap<String,Integer> sleepTimes;
	
	public AgentProperties() {
		properties = new HashMap<String,HashMap<String,String>>();
		HashMap<String,String> props;
		
		// ZI agent properties ------------------------------------
		props = new HashMap<String,String>();
		props.put("sleepTime","150");
		props.put("sleepVar","100");
		properties.put("ZI",props);
		
		// MM agent properties ------------------------------------
		props = new HashMap<String,String>();
		props.put("sleepTime","150");
		props.put("sleepVar","100");
		properties.put("M",props);

		// HFT agent properties ------------------------------------
		props = new HashMap<String,String>();
		props.put("sleepTime","0");
		props.put("sleepVar","100");
		props.put("alpha","0.01");
		props.put("delta","0.05");
		props.put("orderSize","200");
		props.put("timeLimit","30");
//		props.put("lossLimit","0.05");
		properties.put("HFT",props);
		
		// NBBO agent properties ----------------------------------
		props = new HashMap<String,String>();
		props.put("sleepTime","350");
		props.put("sleepVar","100");
		props.put("meanPV","50000");
		props.put("arrivalRate","0.1");
		props.put("kappa","0.2");
		props.put("shockVar","2");
		props.put("expireRate","0.5");
		properties.put("NBBO",props);

	}
	
	/**
	 * Get property's value.
	 * @param key
	 * @return
	 */
	public HashMap<String,String> get(String key) {
		return properties.get(key);
	}

	
	public String toString() {
		String s = "";
		for (Map.Entry<String,HashMap<String,String>> entry : properties.entrySet()) {
			s += entry.getKey() + ": " + entry.getValue().toString() + ", ";
		}
		return s;
	}
}
