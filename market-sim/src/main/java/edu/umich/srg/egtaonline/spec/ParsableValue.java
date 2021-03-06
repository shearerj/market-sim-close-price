package edu.umich.srg.egtaonline.spec;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Converter;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import edu.umich.srg.collect.Iters;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public abstract class ParsableValue<T> extends Value<T> {
  private final Converter<String, T> converter;

  protected ParsableValue(Converter<String, T> converter) {
    this.converter = converter;
  }

  public void parse(String string) {
    set(converter.convert(string));
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof ParsableValue)) {
      return false;
    }
    ParsableValue<?> that = (ParsableValue<?>) other;
    return Objects.equals(this.get(), that.get());
  }

  @Override
  public int hashCode() {
    return get().hashCode();
  }

  @Override
  public String toString() {
    return converter.reverse().convert(get());
  }

  public static class DoubleValue extends ParsableValue<Double> {
    public DoubleValue() {
      super(Doubles.stringConverter());
    }
  }

  public static class LongValue extends ParsableValue<Long> {
    public LongValue() {
      super(Longs.stringConverter());
    }
  }

  public static class IntValue extends ParsableValue<Integer> {
    public IntValue() {
      super(Ints.stringConverter());
    }
  }

  public static class BoolValue extends ParsableValue<Boolean> {
    public BoolValue() {
      super(new CompactBoolConverter());
    }
  }

  public static class StringValue extends ParsableValue<String> {
    public StringValue() {
      super(Converter.<String>identity());
    }
  }

  public abstract static class IterableValue<T> extends ParsableValue<Iterable<T>> {
    public IterableValue(Converter<String, T> elementConverter) {
      super(new IterableConverter<T>(elementConverter));
    }

    @Override
    public int hashCode() {
      return Iters.hashCode(get());
    }

    @Override
    public boolean equals(Object other) {
      if (other == null || !(other instanceof IterableValue)) {
        return false;
      }
      IterableValue<?> that = (IterableValue<?>) other;
      return Iterables.elementsEqual(this.get(), that.get());
    }
  }

  public static class IntsValue extends IterableValue<Integer> {
    public IntsValue() {
      super(Ints.stringConverter());
    }
  }

  public static class DoublesValue extends IterableValue<Double> {
    public DoublesValue() {
      super(Doubles.stringConverter());
    }
  }

  public static class StringsValue extends IterableValue<String> {
    public StringsValue() {
      super(Converter.<String>identity());
    }
  }

  public abstract static class EnumValue<T extends Enum<T>> extends ParsableValue<T> {
    public EnumValue(Class<T> clazz) {
      super(new EnumConverter<T>(clazz));
    }
  }

  public static class SpecValue extends ParsableValue<Spec> {
    protected SpecValue(Package keyPackage) {
      super(new SpecConverter(keyPackage));
    }
  }

  private static final class CompactBoolConverter extends Converter<String, Boolean> {
    private static final Set<String> trueStrings = ImmutableSet.of("t", "true", "1");
    private static final Set<String> falseStrings = ImmutableSet.of("f", "false", "0");

    @Override
    protected String doBackward(Boolean bool) {
      return bool.toString();
    }

    @Override
    protected Boolean doForward(String string) {
      String lower = checkNotNull(string).toLowerCase();
      boolean value = trueStrings.contains(lower);
      checkArgument(value || falseStrings.contains(lower),
          "String must be a true (%s) or false (%s) string", trueStrings, falseStrings);
      return value;
    }
  }

  private static final class IterableConverter<T> extends Converter<String, Iterable<T>> {
    private static final Splitter itemSplitter = Splitter.on('/').omitEmptyStrings();
    private static final Joiner itemJointer = Joiner.on('/');
    private final Converter<String, T> itemConverter;

    protected IterableConverter(Converter<String, T> itemConverter) {
      this.itemConverter = itemConverter;
    }

    @Override
    protected String doBackward(Iterable<T> iterable) {
      return itemJointer.join(itemConverter.reverse().convertAll(iterable));
    }

    @Override
    protected Iterable<T> doForward(String string) {
      return itemConverter.convertAll(itemSplitter.split(string));
    }
  }

  // Not that for conversion to enums, underscores in enum names are replaced with '.'s
  private static final class EnumConverter<T extends Enum<T>> extends Converter<String, T> {
    private Class<T> clazz;
    private static final char underscore_replacement = '.';

    private EnumConverter(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Override
    protected String doBackward(T enumerated) {
      return enumerated.toString().replace('_', underscore_replacement);
    }

    @Override
    protected T doForward(String string) {
      return Enum.valueOf(clazz, string.replace(underscore_replacement, '_'));
    }
  }

  private static final class SpecConverter extends Converter<String, Spec> {
    private static final Splitter splitter = Splitter.on('_');
    private static final MapJoiner joiner = Joiner.on('_').withKeyValueSeparator("_");

    private final Package keyPackage;

    private SpecConverter(Package keyPackage) {
      this.keyPackage = keyPackage;
    }

    @Override
    protected String doBackward(Spec spec) {
      Iterator<Entry<Class<? extends Value<?>>, Value<?>>> specIter = spec.entrySet().iterator();
      return joiner.join(new Iterator<Entry<String, String>>() {

        @Override
        public boolean hasNext() {
          return specIter.hasNext();
        }

        @Override
        public Entry<String, String> next() {
          Entry<Class<? extends Value<?>>, Value<?>> entry = specIter.next();
          return Maps.immutableEntry(entry.getKey().getSimpleName(), entry.getValue().toString());
        }
      });
    }

    @Override
    protected Spec doForward(String string) {
      return Spec.fromPairs(keyPackage, splitter.split(string));
    }
  }

}
