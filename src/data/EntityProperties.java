package data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EntityProperties {
	
	protected Map<String, String> map;

	public EntityProperties() {
		map = new HashMap<String, String>();
	}
	
	public EntityProperties(String config) {
		this();
		String[] args = config.split("_");
		for (int i = 0; i < (args.length/2)*2; i = i + 2) {
			map.put(args[i], args[i+1]);
		}
	}
	
	public Set<String> keys() {
		return map.keySet();
	}
	
	public boolean hasKey(String key) {
		return map.containsKey(key);
	}
	
	public boolean remove(String key) {
		return map.remove(key) != null;
	}
	
	public String getAsString(String key) {
		return map.get(key);
	}
	
	public String getAsString(String key, String def) {
		String val = map.get(key);
		return val != null ? val : def;
	}
	
	public int getAsInt(String key) {
		return Integer.parseInt(map.get(key));
	}
	
	public int getAsInt(String key, int def) {
		String val = map.get(key);
		return val != null ? Integer.parseInt(val) : def;
	}
	
	public double getAsDouble(String key) {
		return Double.parseDouble(map.get(key));
	}
	
	public double getAsDouble(String key, double def) {
		String val = map.get(key);
		return val != null ? Double.parseDouble(val) : def;
	}
	
	public long getAsLong(String key) {
		return Long.parseLong(map.get(key));
	}
	
	public long getAsLong(String key, long def) {
		String val = map.get(key);
		return val != null ? Long.parseLong(val) : def;
	}
	
	public void put(String key, String value) {
		map.put(key, value);
	}
	
	public void put(String key, int value) {
		map.put(key, Integer.toString(value));
	}
	
	public void put(String key, double value) {
		map.put(key, Double.toString(value));
	}
	
	public void put(String key, long value) {
		map.put(key, Long.toString(value));
	}

}
