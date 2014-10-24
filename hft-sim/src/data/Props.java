package data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import props.ImmutableProps;
import props.Value;
import systemmanager.Defaults;

import com.google.common.base.CaseFormat;
import com.google.common.base.Optional;

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
	
	private final ImmutableProps props;

	protected Props(ImmutableProps props) {
		this.props = props;
	}
	
	public static Props fromPairs() {
		return builder().build();
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

	public <T> T get(Class<? extends Value<T>> key) {
		return Optional.fromNullable(props.get(key))
				.or(Optional.fromNullable(Defaults.get(key)))
				.get(); // Will throw an error if nothing was found
	}
	
	public <T> T get(Class<? extends Value<T>> key, Class<? extends Value<T>> defaultKey) {
		return Optional.fromNullable(props.get(key))
				.or(Optional.fromNullable(props.get(defaultKey)))
				.or(Optional.fromNullable(Defaults.get(defaultKey)))
				.get(); // Will throw an error if nothing was found
	}
	
	public Set<Class<? extends Value<?>>> keySet() {
		return props.keySet();
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
			builder.put("systemmanager.Keys$" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, simpleName), value);
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

	private static final long serialVersionUID = -7220533203495890410L;
	
}
