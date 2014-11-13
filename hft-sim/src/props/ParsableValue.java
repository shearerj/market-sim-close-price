package props;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import com.google.common.base.Converter;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

public abstract class ParsableValue<T> extends Value<T> {
	private Converter<String, T> converter;
	
	protected ParsableValue(Converter<String, T> converter) {
		this.converter = converter;
	}
	
	public void parse(String string) {
		set(converter.convert(string));
	}
	
	public String toString() {
		return converter.reverse().convert(get());
	}
	
	public static class DoubleValue extends ParsableValue<Double> {
		public DoubleValue() { super(Doubles.stringConverter()); }
	}
	
	public static class LongValue extends ParsableValue<Long> {
		public LongValue() { super(Longs.stringConverter()); }
	}
	
	public static class IntValue extends ParsableValue<Integer> {
		public IntValue() { super(Ints.stringConverter()); }
	}
	
	public static class BoolValue extends ParsableValue<Boolean> {
		public BoolValue() { super(new CompactBoolConverter()); }
	}
	
	public static class StringValue extends ParsableValue<String> {
		public StringValue() { super(Converter.<String> identity()); }
	}
	
	public static class IntsValue extends ParsableValue<Iterable<Integer>> {
		public IntsValue() { super(new IterableConverter<Integer>(Ints.stringConverter())); }
	}
	
	public static class DoublesValue extends ParsableValue<Iterable<Double>> {
		public DoublesValue() { super(new IterableConverter<Double>(Doubles.stringConverter())); }
	}
	
	public static class StringsValue extends ParsableValue<Iterable<String>> {
		public StringsValue() { super(new IterableConverter<String>(Converter.<String> identity())); }
	}
	
	public static class EnumValue<T extends Enum<T>> extends ParsableValue<T> {
		protected EnumValue(Class<T> clazz) { super(new EnumConverter<T>(clazz)); }
	}
	
	private static final class CompactBoolConverter extends Converter<String, Boolean> {
		private static final Set<String> trueStrings = ImmutableSet.of("t", "true");
		@Override protected String doBackward(Boolean bool) {
			return bool.toString();
		}

		@Override protected Boolean doForward(String string) {
			return trueStrings.contains(checkNotNull(string).toLowerCase());
		}
	}
	
	private static final class IterableConverter<T> extends Converter<String, Iterable<T>> {
		private static final Splitter itemSplitter = Splitter.on('-').omitEmptyStrings();
		private static final Joiner itemJointer = Joiner.on('-');
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
	
	private static final class EnumConverter<T extends Enum<T>> extends Converter<String, T> {
		Class<T> clazz;
		private EnumConverter(Class<T> clazz) {
			this.clazz = clazz;
		}
		
		@Override
		protected String doBackward(T enumerated) {
			return enumerated.toString();
		}

		@Override
		protected T doForward(String string) {
			return Enum.valueOf(clazz, string);
		}
	}
}
