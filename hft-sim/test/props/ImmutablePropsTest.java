package props;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import props.ParsableValue.BoolValue;
import props.ParsableValue.DoubleValue;
import props.ParsableValue.EnumValue;
import props.ParsableValue.IntValue;
import props.ParsableValue.IntsValue;
import props.ParsableValue.LongValue;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

public class ImmutablePropsTest {
	
	private static enum Enumerated { A, B, C };

	public static final class LongKey extends LongValue {};
	public static final class IntKey extends IntValue {};
	public static final class DoubleKey extends DoubleValue {};
	public static final class OtherDoubleKey extends DoubleValue {};
	public static final class BoolKey extends BoolValue {};
	public static final class IntsKey extends IntsValue {};
	public static final class EnumKey extends EnumValue<Enumerated> { protected EnumKey() { super(Enumerated.class); } };
	
	public static final class PrivateKey extends Value<Object> { private PrivateKey() {}; };
	public static final class BadConstructorKey extends Value<Object> { public BadConstructorKey(@SuppressWarnings("unused") int a) {}; };
	public static final class NotAValue {};
	public static final class Wrapper {
		@SuppressWarnings("unused") private static final class HiddenKey extends Value<Object> {};
	}
		
	@Test
	public void emptyTest() {
		ImmutableProps props = ImmutableProps.of();
		assertNull(props.get(LongKey.class));
	}
	
	@Test
	public void longTest() {
		ImmutableProps props = ImmutableProps.of(LongKey.class, 5l);
		assertEquals(5l, (long) props.get(LongKey.class));
	}
	
	@Test
	public void doubleTest() {
		ImmutableProps props = ImmutableProps.of(DoubleKey.class, 6.6);
		assertEquals(6.6, props.get(DoubleKey.class), 0);
	}
	
	@Test
	public void intTest() {
		ImmutableProps props = ImmutableProps.of(IntKey.class, 7);
		assertEquals(7, (int) props.get(IntKey.class));
	}
	
	@Test
	public void boolTest() {
		ImmutableProps props = ImmutableProps.of(BoolKey.class, true);
		assertEquals(true, (boolean) props.get(BoolKey.class));
	}
	
	@Test
	public void intsTest() {
		ImmutableProps props = ImmutableProps.of(IntsKey.class, Ints.asList(1, 2, 3));
		assertEquals(ImmutableList.of(1, 2, 3), props.get(IntsKey.class));
	}
	
	@Test
	public void enumTest() {
		ImmutableProps props = ImmutableProps.of(EnumKey.class, Enumerated.A);
		assertEquals(Enumerated.A, props.get(EnumKey.class));
	}
	
	@Test
	public void parseTest() {
		ImmutableProps props = ImmutableProps.builder()
				.put("props.ImmutablePropsTest$LongKey", "5")
				.put("props.ImmutablePropsTest$EnumKey", "C")
				.build();
		
		assertEquals(5l, (long) props.get(LongKey.class));
		assertEquals(Enumerated.C, props.get(EnumKey.class));
	}
	
	@Test
	public void overwriteTest() {
		ImmutableProps props = ImmutableProps.of(DoubleKey.class, 6.6, DoubleKey.class, 7.2);
		assertEquals(7.2, props.get(DoubleKey.class), 0);
	}
	
	@Test
	public void multipleKeysTest() {
		ImmutableProps props = ImmutableProps.of(
				DoubleKey.class, 5.6,
				LongKey.class, 7l,
				IntsKey.class, Ints.asList(3, 4, 5),
				OtherDoubleKey.class, 7.2,
				BoolKey.class, false);
		
		assertEquals(7l, (long) props.get(LongKey.class));
		assertEquals(5.6, props.get(DoubleKey.class), 0);
		assertEquals(7.2, props.get(OtherDoubleKey.class), 0);
		assertEquals(false, (boolean) props.get(BoolKey.class));
		assertEquals(ImmutableList.of(3, 4, 5), props.get(IntsKey.class));
	}
		
	@Test(expected=IllegalArgumentException.class)
	public void privateConstructorTest() {
		ImmutableProps.of(PrivateKey.class, 5);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void badConstructorTest() {
		ImmutableProps.of(BadConstructorKey.class, 5);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void invalidClassTest() {
		ImmutableProps.builder().put("props.ImmutablePropsTest$NotAValue", "5").build();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void hiddenClassTest() {
		ImmutableProps.builder().put("props.ImmutablePropsTest$Wrapper$HiddenKey", "5").build();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void nonexistentClassTest() {
		ImmutableProps.builder().put("props.ImmutablePropsTest$Blah", "5").build();
	}

}
