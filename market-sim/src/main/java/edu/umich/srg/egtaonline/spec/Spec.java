package edu.umich.srg.egtaonline.spec;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.common.reflect.TypeToken;

import edu.umich.srg.marketsim.Keys.BenchmarkType;
import edu.umich.srg.util.IndentWriter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Spec {

  private static final MapJoiner toString = Joiner.on(", ").withKeyValueSeparator("=");
  private static final ConcurrentHashMap<Package, //
      Collection<Entry<String, Class<ParsableValue<?>>>>> classCache = new ConcurrentHashMap<>();

  private final ImmutableClassToInstanceMap<Value<?>> map;

  @SuppressWarnings("unchecked")
  private static Collection<Entry<String, Class<ParsableValue<?>>>> getKeyClasses(
      Package keyPackage) {
    return classCache.computeIfAbsent(keyPackage, pkg -> {
      try {
        return ClassPath.from(Thread.currentThread().getContextClassLoader()).getAllClasses()
            .stream() //
            .filter(cls -> cls.getPackageName().startsWith(keyPackage.getName())) //
            .map(ClassInfo::load) //
            .filter(ParsableValue.class::isAssignableFrom) //
            .filter(cls -> cls.getAnnotation(IgnoreValue.class) == null)
            .map(cls -> new AbstractMap.SimpleImmutableEntry<>( //
                cls.getSimpleName().isEmpty() ? cls.getName() : cls.getSimpleName(),
                (Class<ParsableValue<?>>) cls))
            .collect(Collectors.toList());
      } catch (IOException e) {
        return Collections.emptyList();
      }
    });
  }

  protected Spec(ImmutableClassToInstanceMap<Value<?>> map) {
    this.map = map;
  }

  public static Spec empty() {
    return builder().build();
  }

  public static <T> Spec fromPairs(Class<? extends Value<T>> key, T value) {
    return builder().put(key, value).build();
  }

  public static <T1, T2> Spec fromPairs(Class<? extends Value<T1>> key1, T1 value1,
      Class<? extends Value<T2>> key2, T2 value2) {
    return builder().put(key1, value1).put(key2, value2).build();
  }

  public static <T1, T2, T3> Spec fromPairs(Class<? extends Value<T1>> key1, T1 value1,
      Class<? extends Value<T2>> key2, T2 value2, Class<? extends Value<T3>> key3, T3 value3) {
    return builder().put(key1, value1).put(key2, value2).put(key3, value3).build();
  }

  public static <T1, T2, T3, T4> Spec fromPairs(Class<? extends Value<T1>> key1, T1 value1,
      Class<? extends Value<T2>> key2, T2 value2, Class<? extends Value<T3>> key3, T3 value3,
      Class<? extends Value<T4>> key4, T4 value4) {
    return builder().put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4)
        .build();
  }

  public static <T1, T2, T3, T4, T5> Spec fromPairs(Class<? extends Value<T1>> key1, T1 value1,
      Class<? extends Value<T2>> key2, T2 value2, Class<? extends Value<T3>> key3, T3 value3,
      Class<? extends Value<T4>> key4, T4 value4, Class<? extends Value<T5>> key5, T5 value5) {
    return builder().put(key1, value1).put(key2, value2).put(key3, value3).put(key4, value4)
        .put(key5, value5).build();
  }

  public static Spec fromPairs(Package keyPackage, String key1, String value1,
      String... keyValuePairs) {
    return fromPairs(keyPackage, Iterables.concat(Collections.singleton(key1),
        Collections.singleton(value1), Arrays.asList(keyValuePairs)));
  }

  /** Build a spec from string pairs. */
  public static Spec fromPairs(Package keyPackage, Iterable<String> keyValuePairs) {
    return fromPairs(keyPackage, keyValuePairs.iterator());
  }

  /** Build a spec from string pairs with specified prefix and case format. */
  public static Spec fromPairs(Package keyPackage, Iterator<String> keyValuePairs) {
    ParsingBuilder builder = builder(keyPackage);
    while (keyValuePairs.hasNext()) {
      builder.put(keyValuePairs.next(), keyValuePairs.next());
    }
    return builder.build();
  }

  public static <T> Spec fromDefaultPairs(Spec defaults, Class<? extends Value<T>> key, T value) {
    return builder().putAll(defaults).put(key, value).build();
  }

  public static <T1, T2> Spec fromDefaultPairs(Spec defaults, Class<? extends Value<T1>> key1,
      T1 value1, Class<? extends Value<T2>> key2, T2 value2) {
    return builder().putAll(defaults).put(key1, value1).put(key2, value2).build();
  }

  public static <T1, T2, T3> Spec fromDefaultPairs(Spec defaults, Class<? extends Value<T1>> key1,
      T1 value1, Class<? extends Value<T2>> key2, T2 value2, Class<? extends Value<T3>> key3,
      T3 value3) {
    return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3).build();
  }

  public static <T1, T2, T3, T4> Spec fromDefaultPairs(Spec defaults,
      Class<? extends Value<T1>> key1, T1 value1, Class<? extends Value<T2>> key2, T2 value2,
      Class<? extends Value<T3>> key3, T3 value3, Class<? extends Value<T4>> key4, T4 value4) {
    return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3)
        .put(key4, value4).build();
  }

  public static <T1, T2, T3, T4, T5> Spec fromDefaultPairs(Spec defaults,
      Class<? extends Value<T1>> key1, T1 value1, Class<? extends Value<T2>> key2, T2 value2,
      Class<? extends Value<T3>> key3, T3 value3, Class<? extends Value<T4>> key4, T4 value4,
      Class<? extends Value<T5>> key5, T5 value5) {
    return builder().putAll(defaults).put(key1, value1).put(key2, value2).put(key3, value3)
        .put(key4, value4).put(key5, value5).build();
  }

  /*
   * We may at some point want to also have a method similar to the map (java8) getWithDefault,
   * however this may encourage putting defaults in specific class implementations which can make it
   * confusing as to what is actually being run, and won't throw errors for misconfigured
   * specifications
   */

  /** Get a value from the spec. */
  public <T> T get(Class<? extends Value<T>> key) {
    Value<T> val = checkNotNull(map.getInstance(key), "Key \"%s\" does not exist in spec",
        key.getSimpleName());
    return val.get();
  }

  public Set<Entry<Class<? extends Value<?>>, Value<?>>> entrySet() {
    return map.entrySet();
  }

  public Spec withDefault(Spec defaults) {
    return builder().putAll(defaults).putAll(this).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static ParsingBuilder builder(Package keyPackage) {
    return new ParsingBuilder(keyPackage);
  }

  public static class Builder {
    // This is used instead of a builder, so we can overwrite keys
    final MutableClassToInstanceMap<Value<?>> builder;

    private Builder() {
      this.builder = MutableClassToInstanceMap.create();
    }

    /** Put a value in the builder. */
    @SuppressWarnings("unchecked")
    public <T> Builder put(Class<? extends Value<T>> key, T value) {
      Value<T> instance = getInstance(key);
      instance.set(value);
      builder.putInstance((Class<Value<T>>) key, instance);
      return this;
    }

    /**
     * Put all values from a previous spec in the builder. This will override any previous settings
     * of a key.
     */
    @SuppressWarnings("unchecked")
    public Builder putAll(Spec other) {
      for (Entry<Class<? extends Value<?>>, Value<?>> e : other.map.entrySet()) {
        builder.putInstance((Class<Value<?>>) e.getKey(), e.getValue());
      }
      return this;
    }

    public Spec build() {
      return new Spec(ImmutableClassToInstanceMap.<Value<?>, Value<?>>copyOf(builder));
    }

  }

  public static class ParsingBuilder extends Builder {
    // This is used instead of a builder, so we can overwrite keys
    private final Map<String, Class<ParsableValue<?>>> keys;

    private ParsingBuilder(Package keyPackage) {
      this.keys = getKeyClasses(keyPackage).stream()
          .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Entry::getValue));
    }

    @Override
    public <T> ParsingBuilder put(Class<? extends Value<T>> key, T value) {
      return (ParsingBuilder) super.put(key, value);
    }

    /** Put a value interpreted from a string in the builder. */
    public <T> ParsingBuilder put(String className, String value) {
      Class<ParsableValue<?>> key = keys.get(className.toLowerCase());
      if (key == null) {
        throw new IllegalArgumentException(className + " is not a valid key");
      }
      ParsableValue<?> instance = getInstance(key);
      instance.parse(value);
      builder.putInstance(key, instance);
      return this;
    }

    @Override
    public ParsingBuilder putAll(Spec other) {
      return (ParsingBuilder) super.putAll(other);
    }

  }

  private static <T extends Value<?>> T getInstance(final Class<T> clazz) {
    T instance;
    try {
      instance = clazz.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("Can't initiate empty constructor of key class " + clazz);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(
          "Either " + clazz + " or its empty constructor is inaccessable");
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(
          clazz + " does not extend Value, and so is not a valid key");
    }
    return instance;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Spec)) {
      return false;
    }
    Spec that = (Spec) other;
    return Objects.equals(this.map, that.map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder().append('{');
    toString.appendTo(builder, map.entrySet().stream()
        .map(e -> new SimpleImmutableEntry<>(e.getKey().getSimpleName(), e.getValue())).iterator());
    return builder.append('}').toString();
  }

  /** Print documentation about all valid keys to writer. */
  public static void printKeys(Package keyPackage, Writer writer) {
    try (PrintWriter print = new PrintWriter(writer)) {
      getKeyClasses(keyPackage).stream().sorted(Comparator.comparing(Entry::getKey))
          .forEachOrdered(entry -> {
            String typeName = ((ParameterizedType) TypeToken.of(entry.getValue())
                .getSupertype(Value.class).getType()).getActualTypeArguments()[0].getTypeName()
                    .replaceAll("[A-Za-z.]+[.$]", "");
            print.format("%s : %s", entry.getKey(), typeName);
            ValueHelp help = entry.getValue().getAnnotation(ValueHelp.class);
            if (help != null) {
              try {
                IndentWriter.withIndent(print, 4).write(help.value());
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
            print.println();
          });
    }
  }

}
