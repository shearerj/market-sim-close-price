package data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import systemmanager.Defaults;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

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
public class Props implements Serializable {
	
	private static final long serialVersionUID = -7220533203495890410L;
	
	private static final Set<String> trueStrings = ImmutableSet.of("t", "true");
	private static final Splitter arraySplitter = Splitter.on('-');

	/**
	 * Store everything as strings, and convert out when called
	 */
	private final ImmutableMap<String, String> properties;

	protected Props() {
		this.properties = ImmutableMap.of();
	}
	
	protected Props(Map<?, ?> properties) {
		Builder<String, String> builder = ImmutableMap.builder();
		for (Entry<?, ?> e : properties.entrySet())
			builder.put(e.getKey().toString(), e.getValue().toString());
		this.properties = builder.build();
	}
	
	public static Props fromMap(Map<?, ?> keyValuePairs) {
		return new Props(keyValuePairs);
	}
	
	/**
	 * Make an entity properties from pairs of objects. There must be an even
	 * number of parameters.
	 */
	public static Props fromPairs(Iterable<?> keyValuePairs) {
		return withDefaults(new Props(), keyValuePairs);
	}
	
	/**
	 * Make an entity properties from pairs of objects. There must be an even
	 * number of parameters.
	 */
	public static Props fromPairs(Object... keyValuePairs) {
		return fromPairs(Arrays.asList(keyValuePairs));
	}
	
	/**
	 * Creates a new entity properties with the given defaults and an iterable
	 * of key value pairs.
	 */
	public static Props withDefaults(Props defaults, Iterable<?> keyValuePairs) {
		Map<Object, Object> newProperties = Maps.<Object, Object> newHashMap(defaults.properties);
		for (Iterator<?> it = keyValuePairs.iterator(); it.hasNext();)
			newProperties.put(it.next(), it.next());
		return new Props(newProperties);
	}
	
	/**
	 * Creates a new entity properties with the given defaults and extra key
	 * value pairs.
	 */
	public static Props withDefaults(Props defaults, Object... keysAndValues) {
		return withDefaults(defaults, Arrays.asList(keysAndValues));
	}

	public String getAsString(String key) {
		return Optional.fromNullable(properties.get(key))
				.or(Optional.fromNullable(Defaults.get(key)))
				.get(); // Will throw an error if nothing was found
	}
	
	public String getAsString(String key, String defaultKey) {
		return Optional.fromNullable(properties.get(key))
				.or(Optional.fromNullable(properties.get(defaultKey)))
				.or(Optional.fromNullable(Defaults.get(defaultKey)))
				.get(); // Will throw an error if nothing was found
	}

	public int getAsInt(String key) {
		return Integer.parseInt(getAsString(key));
	}

	public int getAsInt(String key, String defaultKey) {
		return Integer.parseInt(getAsString(key, defaultKey));
	}

	public double getAsDouble(String key) {
		return Double.parseDouble(getAsString(key));
	}

	public double getAsDouble(String key, String defaultKey) {
		return Double.parseDouble(getAsString(key, defaultKey));
	}

	public float getAsFloat(String key) {
		return Float.parseFloat(getAsString(key));
	}
	
	public float getAsFloat(String key, String defaultKey) {
		return Float.parseFloat(getAsString(key, defaultKey));
	}

	public long getAsLong(String key) {
		return Long.parseLong(getAsString(key));
	}

	public long getAsLong(String key, String defaultKey) {
		return Long.parseLong(getAsString(key, defaultKey));
	}

	public boolean getAsBoolean(String key) {
		return parseBoolean(getAsString(key));
	}
	
	public boolean getAsBoolean(String key, String defaultKey) {
		return parseBoolean(getAsString(key, defaultKey));
	}
	
	public int[] getAsIntArray(String key) {
		return parseIntArr(getAsString(key));
	}
	
	public int[] getAsIntArray(String key, String defaultKey){
		return parseIntArr(getAsString(key, defaultKey));
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Props))
			return false;
		final Props e = (Props) o;
		return properties.equals(e.properties);
	}

	@Override
	public int hashCode() {
		return properties.hashCode();
	}

	@Override
	public String toString() {
		return properties.toString();
	}

	protected static boolean parseBoolean(String string) {
		return string != null && trueStrings.contains(string.toLowerCase());
	}
	
	// TODO Make this apply to general arrays instead of just int
	protected static int[] parseIntArr(String string) {
		return Ints.toArray(Collections2.transform(arraySplitter.splitToList(string), Ints.stringConverter()));
	}
}
