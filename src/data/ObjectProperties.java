package data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;

public class ObjectProperties {
	
	protected static final transient Gson gson = new Gson();
	protected Map<String, String> properties;

	public ObjectProperties() {
		properties = new HashMap<String, String>();
	}
	
	public ObjectProperties(ObjectProperties copy) {
		properties = new HashMap<String, String>(copy.properties);
	}
	
	public ObjectProperties(String config) {
		this();
		addConfig(config);
	}
	
	public ObjectProperties(ObjectProperties def, String config) {
		this(def);
		addConfig(config);
	}
	
	public void addConfig(String config) {
		String[] args = config.split("_");
		for (int i = 0; i < (args.length/2)*2; i = i + 2) {
			properties.put(args[i], args[i+1]);
		}
	}
	
	public Set<String> keys() {
		return properties.keySet();
	}
	
	public boolean hasKey(String key) {
		return properties.containsKey(key);
	}
	
	public boolean remove(String key) {
		return properties.remove(key) != null;
	}
	
	public String getAsString(String key) {
		return properties.get(key);
	}
	
	public String getAsString(String key, String def) {
		String val = properties.get(key);
		return val != null ? val : def;
	}
	
	public int getAsInt(String key) {
		return Integer.parseInt(properties.get(key));
	}
	
	public int getAsInt(String key, int def) {
		String val = properties.get(key);
		return val != null ? Integer.parseInt(val) : def;
	}
	
	public double getAsDouble(String key) {
		return Double.parseDouble(properties.get(key));
	}
	
	public double getAsDouble(String key, double def) {
		String val = properties.get(key);
		return val != null ? Double.parseDouble(val) : def;
	}
	
	public float getAsFloat(String key) {
		return Float.parseFloat(properties.get(key));
	}
	
	public float getAsFloat(String key, float def) {
		String val = properties.get(key);
		return val != null ? Float.parseFloat(val) : def;
	}
	
	public long getAsLong(String key) {
		return Long.parseLong(properties.get(key));
	}
	
	public long getAsLong(String key, long def) {
		String val = properties.get(key);
		return val != null ? Long.parseLong(val) : def;
	}
	
	public boolean getAsBoolean(String key) {
		return Boolean.parseBoolean(properties.get(key));
	}
	
	public boolean getAsBoolean(String key, boolean def) {
		String val = properties.get(key);
		return val != null ? Boolean.parseBoolean(val) : def;
	}
	
	public void put(String key, String value) {
		properties.put(key, value);
	}
	
	public void put(String key, int value) {
		properties.put(key, Integer.toString(value));
	}
	
	public void put(String key, double value) {
		properties.put(key, Double.toString(value));
	}

	public void put(String key, float value) {
		properties.put(key, Float.toString(value));
	}
	
	public void put(String key, long value) {
		properties.put(key, Long.toString(value));
	}
	
	public void put(String key, boolean value) {
		properties.put(key, Boolean.toString(value));
	}
	
	@Override
	public boolean equals(Object o) {
	    if (o == null || !(o instanceof ObjectProperties))
	        return false;
	    final ObjectProperties e = (ObjectProperties) o;
	    return properties.equals(e.properties);
	}
	
	@Override
	public int hashCode() {
		return properties.hashCode();
	}
	
	public String toConfigString() {
		if (properties.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> e : properties.entrySet())
			sb.append('_').append(e.getKey()).append('_').append(e.getValue());
		return sb.substring(1);
	}
	
	@Override
	public String toString() {
		return properties.toString();
	}
	
	public static void main(String... args) {
		ObjectProperties e = new ObjectProperties("blah_1_hello_hft_key_6.7");
		System.out.println(e);
		System.out.println(e.toConfigString());
	}

}
