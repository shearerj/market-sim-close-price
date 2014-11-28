package data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

import props.ImmutableProps;
import props.Value;
import systemmanager.Defaults;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

/**
 * Class that represents the properties of an entity. These are generally loaded
 * from a simulation spec file.
 * 
 * This class contains two main accessor methods. get(key) and get(key,
 * backup-key). See the methods for full documentation.
 * 
 * @author erik
 * 
 */
public class Props implements Serializable {
	
	private static final Joiner paramJoiner = Joiner.on('_');
	private static final String classPath = "systemmanager.Keys$"; // Allows reading of strings into classes
	private static final Props empty = builder().build();
	
	private final ImmutableProps props; // Underlying type safe properties data structure

	protected Props(ImmutableProps props) {
		this.props = props;
	}
	
	public static Props fromPairs() {
		return empty;
	}
	
	public static <T> Props fromPairs(Class<? extends Value<T>> key, T value) {
		return builder().put(key, value).build();
	}
	
	public static <T1, T2> Props fromPairs(Class<? extends Value<T1>> key1, T1 value1, Class<? extends Value<T2>> key2, T2 value2) {
		return builder().put(key1, value1).put(key2, value2).build();
	}
	
	public static <T1, T2, T3> Props fromPairs(
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3) {
		return builder().put(key1, value1).put(key2, value2).put(key3, value3).build();
	}
	
	public static <T1, T2, T3, T4> Props fromPairs(
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3,
			Class<? extends Value<T4>> key4, T4 value4) {
		return builder().put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4).build();
	}
	
	public static <T1, T2, T3, T4, T5> Props fromPairs(
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3,
			Class<? extends Value<T4>> key4, T4 value4,
			Class<? extends Value<T5>> key5, T5 value5) {
		return builder().put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4).put(key5, value5).build();
	}
	
	public static Props fromPairs(String... keyValuePairs) {
		return fromPairs(Arrays.asList(keyValuePairs));
	}
	
	public static Props fromPairs(Iterable<String> keyValuePairs) {
		return fromPairs(keyValuePairs.iterator());
	}
	
	public static Props fromPairs(Iterator<String> keyValuePairs) {
		Builder builder = builder();
		while (keyValuePairs.hasNext())
			builder.put(keyValuePairs.next(), keyValuePairs.next());
		return builder.build();
	}
	
	public static <T> Props withDefaults(Props defaults, Class<? extends Value<T>> key, T value) {
		return builder().putAll(defaults).put(key, value).build();
	}
	
	public static <T1, T2> Props withDefaults(
			Props defaults,
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2) {
		return builder().putAll(defaults).put(key1, value1).put(key2, value2).build();
	}
	
	public static <T1, T2, T3> Props withDefaults(
			Props defaults,
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3) {
		return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3).build();
	}
	
	public static <T1, T2, T3, T4> Props withDefaults(
			Props defaults,
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3,
			Class<? extends Value<T4>> key4, T4 value4) {
		return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4).build();
	}
	
	public static <T1, T2, T3, T4, T5> Props withDefaults(
			Props defaults,
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3,
			Class<? extends Value<T4>> key4, T4 value4,
			Class<? extends Value<T5>> key5, T5 value5) {
		return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4).put(key5, value5).build();
	}
	
	public static Props withDefaults(Props defaults, String... keyValuePairs) {
		return withDefaults(defaults, Arrays.asList(keyValuePairs));
	}
	
	public static Props withDefaults(Props defaults, Iterable<String> keyValuePairs) {
		return withDefaults(defaults, keyValuePairs.iterator());
	}
	
	// Note, this will fail slow if iterator has an odd number of elements
	public static Props withDefaults(Props defaults, Iterator<String> keyValuePairs) {
		Builder builder = builder().putAll(defaults);
		while (keyValuePairs.hasNext())
			builder.put(keyValuePairs.next(), keyValuePairs.next());
		return builder.build();
	}
	
	public static Props merge(Props... severalProperties) {
		Builder builder = builder();
		for (Props props : severalProperties)
			builder.putAll(props);
		return builder.build();
	}
	
	public static Builder builder() {
		return new Builder();
	}

	/** Gets the value associated with the passed in key */
	public <T> T get(Class<? extends Value<T>> key) {
		return checkNotNull(Optional.fromNullable(props.get(key))
				.or(Optional.fromNullable(Defaults.get(key)))
				.orNull(), "Default value of %s is not defined", key);
	}
	
	/**
	 * Gets the value associated with the passed in key. If a value isn't
	 * present, then it gets the value associated with the defaultKey
	 */
	public <T> T get(Class<? extends Value<T>> key, Class<? extends Value<T>> defaultKey) {
		return checkNotNull(Optional.fromNullable(props.get(key))
				.or(Optional.fromNullable(props.get(defaultKey)))
				.or(Optional.fromNullable(Defaults.get(defaultKey)))
				.orNull(), "Default value of %s is not defined", key);
	}
	
	public static class Builder {
		private final ImmutableProps.Builder builder;
		
		private Builder() {
			this.builder = ImmutableProps.builder();
		}
		
		public <T> Builder put(Class<? extends Value<T>> key, T value) {
			builder.put(key, value);
			return this;
		}
		
		public Builder put(String simpleName, String value) {
			builder.put(classPath + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, simpleName), value);
			return this;
		}
		
		public Builder putAll(Props other) {
			builder.putAll(other.props);
			return this;
		}
		
		public Props build() {
			return new Props(builder.build());
		}
	}

	@Override
	public String toString() {
		return props.toString();
	}
	
	public String toConfigString() {
		ImmutableList.Builder<String> strings = ImmutableList.builder();
		for (Entry<Class<? extends Value<?>>, Value<?>> e : props.entrySet())
			strings.add(keyToString(e.getKey()))
			.add(e.getValue().toString());
		return paramJoiner.join(strings.build());
	}
	
	public JsonObject toJson() {
		JsonObject root = new JsonObject();
		for (Entry<Class<? extends Value<?>>, Value<?>> e : props.entrySet())
			root.addProperty(keyToString(e.getKey()), e.getValue().toString());
		return root;
	}
	
	public static String keyToString(Class<?> key) {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key.getSimpleName());
	}

	private static final long serialVersionUID = -7220533203495890410L;
	
}
