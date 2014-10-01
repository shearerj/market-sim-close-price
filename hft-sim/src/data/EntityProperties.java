package data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import systemmanager.Consts;
import systemmanager.Defaults;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
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

	/**
	 * Store everything as strings, and convert out when called
	 */
	protected final Map<String, String> properties;
	protected final String configString;

	protected EntityProperties(Map<String, String> backedProperties, String configString) {
		this.properties = backedProperties;
		this.configString = configString;
	}
	
	/**
	 * Create an empty EntityProperties
	 * 
	 * @return
	 */
	public static EntityProperties empty() {
		return new EntityProperties(Maps.<String, String> newHashMap(), "");
	}
	
	/**
	 * Make a deep copy of another entity properties
	 * 
	 * @param from
	 * @return
	 */
	public static EntityProperties copy(EntityProperties from) {
		return new EntityProperties(Maps.newHashMap(from.properties), from.configString);
	}
	
	/**
	 * Make an entity properties from pairs of strings and objects. There must
	 * be an even number of parameters, and every other parameter, including the
	 * first, must be a string.
	 * 
	 * @param keysAndValues
	 * @return
	 */
	public static EntityProperties fromPairs(Object... keysAndValues) {
		EntityProperties created = EntityProperties.empty();
		created.putPairs(keysAndValues);
		return created;
	}
	
	/**
	 * Same as fromPairs, but with a default entity properties that the initial
	 * configuration is copied from. If a key appears in both from and
	 * keysAndValues, the value in keysAndValues will be used.
	 * 
	 * @param from
	 * @param keysAndValues
	 * @return
	 */
	public static EntityProperties copyFromPairs(EntityProperties from, Object... keysAndValues) {
		EntityProperties created = EntityProperties.copy(from);
		created.putPairs(keysAndValues);
		return created;
	}
	
	/**
	 * Parse an entity properties from a config string where keys and values are
	 * underscore delimited.
	 * 
	 * <code>fromPairs(pairs...)</code> and
	 * <code>fromConfigString(Joiner.on('_').join(pairs...))</code> will have
	 * the same result.
	 * 
	 * @param configString
	 * @return
	 */
	public static EntityProperties fromConfigString(String configString) {
		Map<String, String> properties = parseConfigString(configString);
		return new EntityProperties(properties, configString);
	}

	/**
	 * Parses a config string into a map from keys to values
	 * @param configString
	 * @return
	 */
	protected static Map<String, String> parseConfigString(String configString) {
		Iterable<String> args = configSplitter.split(checkNotNull(configString, "Config String"));
		checkArgument(Iterables.size(args) % 2 == 0, "Not key value pair");
		Map<String, String> parsed = Maps.newHashMap();
		for (Iterator<String> it = args.iterator(); it.hasNext();)
			parsed.put(it.next(), it.next());
		return parsed;
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
		String val = properties.get(key);
		return val != null ? val : Defaults.getAsString(key);
	}

	public int getAsInt(String key) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Integer.parseInt(val) : Defaults.getAsInt(key);
		return Defaults.getAsInt(key);
	}

	public int getAsInt(String key, String defaultKey) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Integer.parseInt(val) : getAsInt(defaultKey);
		return getAsInt(defaultKey);
	}

	public double getAsDouble(String key) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Double.parseDouble(val) : Defaults.getAsDouble(key);
		return Defaults.getAsDouble(key);
	}

	public double getAsDouble(String key, String defaultKey) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Double.parseDouble(val) : getAsDouble(defaultKey);
		return getAsDouble(defaultKey);
	}

	public float getAsFloat(String key) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Float.parseFloat(val) : Defaults.getAsFloat(key);
		return Defaults.getAsFloat(key);
	}
	
	public float getAsFloat(String key, String defaultKey) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Float.parseFloat(val) : getAsFloat(defaultKey);
		return getAsFloat(defaultKey);
	}

	public long getAsLong(String key) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Long.parseLong(val) : Defaults.getAsLong(key);
		return Defaults.getAsLong(key);
	}

	public long getAsLong(String key, String defaultKey) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Long.parseLong(val) : getAsLong(defaultKey);
		return getAsLong(defaultKey);
	}
	
	private static boolean parseBoolean(String val) {
		if (val.toLowerCase().equals("t")) return true;
		else if (val.toLowerCase().equals("f")) return false;
		return false;
	}
	
	public boolean getAsBoolean(String key) {
		String val = properties.get(key);
		if (val != null) if (!val.isEmpty()) return Boolean.parseBoolean(val) || EntityProperties.parseBoolean(val);
		return Defaults.getAsBoolean(key);
	}

	public boolean getAsBoolean(String key, String defaultKey) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? Boolean.parseBoolean(val) || EntityProperties.parseBoolean(val) 
											   : getAsBoolean(defaultKey);
		return getAsBoolean(defaultKey);
	}
	
	public int[] getAsIntArray(String key) {
		String val = properties.get(key);
		if (val != null) return !val.isEmpty() ? getAsIntArray(key, Consts.DELIMITER) 
											   : Defaults.getAsIntArray(key, Consts.DELIMITER);
		return Defaults.getAsIntArray(key, Consts.DELIMITER);
	}
	
	public int[] getAsIntArray(String key, String delim){
		String[] vals = properties.get(key).split(delim);
		int[] result = new int[vals.length];
		for(int i = 0; i < vals.length; i++)
			{ result[i] = Integer.parseInt(vals[i]); }
		return result;
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
			put((String) keysAndValues[i], keysAndValues[i+1].toString());
	}
	
	public String getConfigString() {
		return configString;
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
