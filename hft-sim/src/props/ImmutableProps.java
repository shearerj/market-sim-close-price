package props;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Maps;
import com.google.common.collect.MutableClassToInstanceMap;

public class ImmutableProps {

	private static final MapJoiner toString = Joiner.on(", ").withKeyValueSeparator("=");
	private final ImmutableClassToInstanceMap<Value<?>> map;
	
	protected ImmutableProps(ImmutableClassToInstanceMap<Value<?>> map) {
		this.map = map;
	}
	
	public static ImmutableProps of() {
		return builder().build();
	}
	
	public static <T> ImmutableProps of(Class<? extends Value<T>> key, T value) {
		return builder().put(key, value).build();
	}
	
	public static <T1, T2> ImmutableProps of(Class<? extends Value<T1>> key1, T1 value1, Class<? extends Value<T2>> key2, T2 value2) {
		return builder().put(key1, value1).put(key2, value2).build();
	}

	public static <T1, T2, T3> ImmutableProps of(
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3) {
		return builder().put(key1, value1).put(key2, value2).put(key3, value3).build();
	}
	
	public static <T1, T2, T3, T4> ImmutableProps of(
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3,
			Class<? extends Value<T4>> key4, T4 value4) {
		return builder().put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4).build();
	}
	
	public static <T1, T2, T3, T4, T5> ImmutableProps of(
			Class<? extends Value<T1>> key1, T1 value1,
			Class<? extends Value<T2>> key2, T2 value2,
			Class<? extends Value<T3>> key3, T3 value3,
			Class<? extends Value<T4>> key4, T4 value4,
			Class<? extends Value<T5>> key5, T5 value5) {
		return builder().put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4).put(key5, value5).build();
	}
	
	public static ImmutableProps of(String... keyValuePairs) {
		return of(Arrays.asList(keyValuePairs));
	}
	
	public static ImmutableProps of(Iterable<String> keyValuePairs) {
		return of(keyValuePairs.iterator());
	}
	
	public static ImmutableProps of(Iterator<String> keyValuePairs) {
		Builder builder = builder();
		while (keyValuePairs.hasNext())
			builder.put(keyValuePairs.next(), keyValuePairs.next());
		return builder.build();
	}

	public <T> T get(Class<? extends Value<T>> key) {
		Value<T> val = map.getInstance(key);
		return val == null ? null : val.get();
	}
	
	public Set<Class<? extends Value<?>>> keySet() {
		return map.keySet();
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		// This is used instead of a builder, so we can overwrite keys
		private final MutableClassToInstanceMap<Value<?>> builder;
		
		private Builder() {
			this.builder = MutableClassToInstanceMap.create();
		}
		
		@SuppressWarnings("unchecked")
		public <T> Builder put(Class<? extends Value<T>> key, T value) {
			Value<T> instance = getInstance(key);
			instance.set(value);
			builder.putInstance((Class<Value<T>>) key, instance);
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public <T> Builder put(String className, String value) {
			Class<ParsableValue<?>> key;
			try {
				key = (Class<ParsableValue<?>>) Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(className + " doesn't exist");
			}
			ParsableValue<?> instance = getInstance(key);
			instance.parse(value);
			builder.putInstance(key, instance);
			return this;
		}
		
		@SuppressWarnings("unchecked")
		public Builder putAll(ImmutableProps other) {
			for (Entry<Class<? extends Value<?>>, Value<?>> e : other.map.entrySet())
				builder.putInstance((Class<Value<?>>) e.getKey(), e.getValue());
			return this;
		}
		
		public ImmutableProps build() {
			return new ImmutableProps(ImmutableClassToInstanceMap.copyOf(builder));
		}
		
	}
	
	private static <T extends Value<?>> T getInstance(final Class<T> clazz) {
		T instance;
		try {
			instance = clazz.newInstance();
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("Can't initiate empty constructor of key class " + clazz);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Can't access key class " + clazz + " or its empty constructor");
		}
		return instance;
	}

	@Override
	public String toString() {
		final Iterator<Entry<Class<? extends Value<?>>, Value<?>>> it = map.entrySet().iterator();
		StringBuilder builder = new StringBuilder().append('{');
		toString.appendTo(builder, new Iterator<Entry<String, Value<?>>>() {
			@Override public boolean hasNext() { return it.hasNext(); }
			@Override public Entry<String, Value<?>> next() {
				Entry<Class<? extends Value<?>>, Value<?>> e = it.next();
				return Maps.<String, Value<?>> immutableEntry(e.getKey().getSimpleName(), e.getValue());
			}
			@Override public void remove() { throw new UnsupportedOperationException(); }
		});
		return builder.append('}').toString();
	}
	
}
