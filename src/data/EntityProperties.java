package data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class EntityProperties {
	
	protected EntityProperties def;
	protected Map<String, String> properties;

	public EntityProperties() {
		this.properties = new HashMap<String, String>();
		this.def = null;
	}
	
	public EntityProperties(EntityProperties def) {
		this.properties = new HashMap<String, String>();
		this.def = def;
	}
	
	public EntityProperties(String config) {
		this();
		addConfig(config);
	}
	
	public EntityProperties(EntityProperties def, String config) {
		this(def);
		addConfig(config);
	}
	
	protected EntityProperties(Map<String, String> properties, EntityProperties def) {
		this.properties = properties;
		this.def = def;
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
	
	public void put(String key, Number value) {
		properties.put(key, value.toString());
	}
	
	public void put(String key, boolean value) {
		properties.put(key, Boolean.toString(value));
	}
	
	public EntityProperties getDefault() {
		return def;
	}
	
	public void setDefault(EntityProperties def) {
		this.def = def;
	}
	
	public EntityProperties flatten() {
		Map<String, String> newProps = new HashMap<String, String>();
		flattenHelper(newProps);
		return new EntityProperties(newProps, null);
	}
	
	private void flattenHelper(Map<String, String> newProps) {
		if (def != null) def.flattenHelper(newProps);
		newProps.putAll(properties);
	}
	
	@Override
	public boolean equals(Object o) {
	    if (o == null || !(o instanceof EntityProperties))
	        return false;
	    final EntityProperties e = (EntityProperties) o;
	    return properties.equals(e.properties);
	}
	
	@Override
	public int hashCode() {
		return properties.hashCode() ^ (def == null ? 0 : def.hashCode());
	}
	
	public String toConfigString() {
		if (properties.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> e : properties.entrySet())
			sb.append('_').append(e.getKey()).append('_').append(e.getValue());
		return sb.substring(1);
	}
	
	public String toCascadeString() {
		if (def == null) return toString();
		return toString() + " <- " + def.toCascadeString();
	}
	
	@Override
	public String toString() {
		return properties.toString();
	}
	
	public static void main(String... args) {
		EntityProperties a = new EntityProperties("foo_5");
		EntityProperties b = new EntityProperties(a);
		EntityProperties c = new EntityProperties(b, "bar_baz");
		System.out.println(c);
		System.out.println(c.flatten());
		System.out.println(c.toCascadeString());
	}

}
