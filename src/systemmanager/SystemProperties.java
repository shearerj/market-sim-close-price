package systemmanager;

import java.util.HashMap;

/**
 * This class stores the parameters that are not set in a
 * configuration file, but which may be changed at each runtime.
 * 
 * [For testing purposes; can tune params from one location]
 * 
 * @author ewah
 */
public class SystemProperties {

	private HashMap<String,HashMap<String,String>> props;
	public HashMap<String,Integer> sleepTimes;
	
	public SystemProperties() {
		props = new HashMap<String,HashMap<String,String>>();
		
		// ZI agent properties
		HashMap<String,String> ZIprops = new HashMap<String,String>();
		ZIprops.put("sleepTime","25");
		
		// NBBO agent properties
		HashMap<String,String> NBBOprops = new HashMap<String,String>();
		NBBOprops.put("meanPV","50000");
		NBBOprops.put("arrivalRate","0.1");
		NBBOprops.put("kappa","0.2");
		NBBOprops.put("sleepTime","35");
		NBBOprops.put("markets","1,2"); // TODO eliminate this eventually
		
		// TEST agent properties
		HashMap<String,String> TESTprops = new HashMap<String,String>();
		TESTprops.put("sleepTime","15");
		
		// Update props for each agent
		props.put("ZI",ZIprops);
		props.put("NBBO",NBBOprops);
		props.put("TEST",TESTprops);
	}
	
	public HashMap<String,String> get(String key) {
		return props.get(key);
	}
}
