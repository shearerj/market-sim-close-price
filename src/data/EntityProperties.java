package data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * Class that represents the properties of an entity. These are generally loaded
 * from a simulation spec file.
 * 
 * Contains methods to get values as any type, potentially with a default value.
 * If a default value isn't used, and the key doesn't exist, you'll get a null
 * object, or a null pointer.
 * 
 * @author erik
 * 
 */
public class EntityProperties implements Serializable {
	
	private static final long serialVersionUID = -7220533203495890410L;
	private final static Splitter configSplitter = Splitter.on('_');
	private final static MapJoiner configJoiner = Joiner.on('_').withKeyValueSeparator("_");

	protected Map<String, String> properties;

	// FIXME Change protected constructor to only takea map of values and create static methods for everything
	protected EntityProperties() {
		this.properties = Maps.newHashMap();
	}

	protected EntityProperties(EntityProperties def) {
		this.properties = Maps.newHashMap(checkNotNull(def.properties));
	}

	public EntityProperties(String config) {
		this();
		addConfig(config);
	}

	public EntityProperties(EntityProperties def, String config) {
		this(def);
		addConfig(config);
	}
	
	public static EntityProperties empty() {
		return new EntityProperties();
	}
	
	public static EntityProperties copy(EntityProperties from) {
		return new EntityProperties(from);
	}
	
	public static EntityProperties fromPairs(Object... keysAndValues) {
		EntityProperties created = EntityProperties.empty();
		created.putPairs(keysAndValues);
		return created;
	}
	
	public static EntityProperties copyFromPairs(EntityProperties from, Object... keysAndValues) {
		EntityProperties created = EntityProperties.copy(from);
		created.putPairs(keysAndValues);
		return created;
	}

	protected void addConfig(String config) {
		checkNotNull(config, "Config String");
		ImmutableList<String> args = ImmutableList.copyOf(configSplitter.split(config));
		checkArgument(args.size() % 2 == 0, "Not key value pair");
		for (Iterator<String> it = args.iterator(); it.hasNext();) {
			properties.put(it.next(), it.next());
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
		if (val != null) return !val.isEmpty() ? Integer.parseInt(val) : def;
		return def;
	}

	public double getAsDouble(String key) {
		return Double.parseDouble(properties.get(key));
	}

	public double getAsDouble(String key, double def) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Double.parseDouble(val) : def;
		return def;
	}

	public float getAsFloat(String key) {
		return Float.parseFloat(properties.get(key));
	}

	public float getAsFloat(String key, float def) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Float.parseFloat(val) : def;
		return def;
	}

	public long getAsLong(String key) {
		return Long.parseLong(properties.get(key));
	}

	public long getAsLong(String key, long def) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Long.parseLong(val) : def;
		return def;
	}

	public boolean getAsBoolean(String key) {
		return Boolean.parseBoolean(properties.get(key));
	}

	public boolean getAsBoolean(String key, boolean def) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Boolean.parseBoolean(val) : def;
		return def;
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
	
	public void putPairs(Object... keysAndValues) {
		checkArgument(keysAndValues.length % 2 == 0);
		for (int i = 0; i < keysAndValues.length; i+=2)
			put(keysAndValues[i].toString(), keysAndValues[i+1].toString());
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
		return properties.hashCode();
	}

	public String toConfigString() {
		return configJoiner.join(properties);
	}

	@Override
	public String toString() {
		return properties.toString();
	}

}
