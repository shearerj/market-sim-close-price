package utils;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;
import java.util.SortedMap;

import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ForwardingSortedMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public abstract class Maps2 {

	/**
	 * Creates an immutable map from strings to strings where the input is
	 * successive pairs of arbitrary objects.
	 */
	public static ImmutableMap<String, String> fromPairs(Object... keyValuePairs) {
		checkArgument(keyValuePairs.length % 2 == 0, "Must have an even number of inputs");
		Builder<String, String> builder = ImmutableMap.builder();
		for (int i = 0; i < keyValuePairs.length; i += 2)
			builder.put(keyValuePairs[i].toString(), keyValuePairs[i+1].toString());
		return builder.build();
	}

	public static <K, V> Map<K, V> addDefault(final Map<K, V> map, final Supplier<V> supplier) {
		return new ForwardingMap<K, V>() {
			
			@Override
			protected Map<K, V> delegate() {
				return map;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public V get(Object key) {
				V value = delegate().get(key);
				if (value == null) {
					value = supplier.get();
					delegate().put((K) key, value);
				}
				return value;
			}
			
		};
	}
	
	public static <K, V> SortedMap<K, V> addDefault(final SortedMap<K, V> map, final Supplier<V> supplier) {
		return new ForwardingSortedMap<K, V>() {
			
			@Override
			protected SortedMap<K, V> delegate() {
				return map;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public V get(Object key) {
				V value = delegate().get(key);
				if (value == null) {
					value = supplier.get();
					delegate().put((K) key, value);
				}
				return value;
			}
			
		};
	}

	public static <V> SortedMap<String, V> prefix(String prefix, SortedMap<String, V> map) {
		int index = prefix.length() - 1;
		for (; index >= 0; index--)
			if (prefix.charAt(index) < Character.MAX_VALUE)
				break;
		if (index < 0) {
			return map.tailMap(prefix);
		} else {
			return map.subMap(prefix, prefix.substring(0, index) + (char) (1 + prefix.charAt(index)));
		}
	}
	
}
