package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map.Entry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import systemmanager.Defaults;
import systemmanager.Keys;

import com.google.common.base.Splitter;

public class PropsTest {
	
	private static final Splitter configSplit = Splitter.on('_');

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void emptyTest() {
		Props props = Props.fromPairs();
		assertEquals("{}", props.toString());
		
		for (Entry<String, String> e : Defaults.defaults.entrySet())
			assertEquals(e.getValue(), props.getAsString(e.getKey()));
	}
	
	@Test
	public void basicProps() {
		Props props = Props.fromPairs(
				Keys.GAMMA, 0.005,
				Keys.ETA, 10,
				Keys.THETA, Long.MIN_VALUE,
				Keys.WITHDRAW_ORDERS, false,
				Keys.ASSIGN, "assign",
				Keys.BETA_R, Float.MAX_VALUE,
				Keys.SPREADS, "1-2-5");
		
		assertEquals(0.005, props.getAsDouble(Keys.GAMMA), 1E-6);
		assertEquals(10, props.getAsInt(Keys.ETA));
		assertEquals("assign", props.getAsString(Keys.ASSIGN));
		assertEquals(Long.MIN_VALUE, props.getAsLong(Keys.THETA));
		assertEquals(false, props.getAsBoolean(Keys.WITHDRAW_ORDERS));
		assertEquals(Float.MAX_VALUE, props.getAsFloat(Keys.BETA_R), 1E-6);
		int[] spreads = new int[]{1,2,5};
		int[] propSpreads = props.getAsIntArray(Keys.SPREADS);
		for (int i = 0; i < 2; i++)
			assertEquals(spreads[i], propSpreads[i]);
		
		exception.expect(IllegalStateException.class);
		props.getAsDouble("nonexistentKey");
	}
	
	@Test
	public void booleanTest() {
		Props props = Props.fromPairs(
				Keys.GAMMA, true,
				Keys.ETA, false,
				Keys.THETA, "T",
				Keys.ALPHA, "f");
		
		assertTrue(props.getAsBoolean(Keys.GAMMA));
		assertTrue(props.getAsBoolean(Keys.THETA));
		assertFalse(props.getAsBoolean(Keys.ALPHA));
		assertFalse(props.getAsBoolean(Keys.ETA));
		
		assertTrue(props.getAsBoolean(Keys.GAMMA, Keys.ETA));
		assertTrue(props.getAsBoolean("nonexistentKey", Keys.GAMMA));
		assertTrue(props.getAsBoolean("nonexistentKey", Keys.THETA));
		assertFalse(props.getAsBoolean("nonexistentKey", Keys.ALPHA));
		assertFalse(props.getAsBoolean("nonexistentKey", Keys.ETA));
		
		String config = Keys.GAMMA + "_T_" + Keys.ETA + "_f";
		Props props2 = Props.fromPairs(configSplit.split(config));
		assertTrue(props2.getAsBoolean(Keys.GAMMA));
		assertFalse(props2.getAsBoolean(Keys.ETA));
		
	}
	
	@Test
	public void copyPropsFromPairs() {
		Props props = Props.fromPairs(
				Keys.GAMMA, 0.005,
				Keys.ETA, 10);
		
		Props copy = Props.withDefaults(props, Keys.THETA, -1, Keys.ETA, 20);
		assertEquals(props.getAsDouble(Keys.GAMMA), copy.getAsDouble(Keys.GAMMA), 1E-6);
		assertNotEquals(props.getAsInt(Keys.ETA), copy.getAsInt(Keys.ETA));
		assertEquals(20, copy.getAsInt(Keys.ETA));
		assertEquals(-1, copy.getAsInt(Keys.THETA));
	}
	
	@Test
	public void propsFromConfigString() {
		String config = "gamma_0.005_eta_10";
		
		Props props = Props.fromPairs(configSplit.split(config));
		assertEquals(0.005, props.getAsDouble(Keys.GAMMA), 1E-6);
		assertEquals(10, props.getAsInt(Keys.ETA));
	}
	
	@Test
	public void defaultIntProps() {
		// test with using a default key
		Props props = Props.fromPairs(
				Keys.THETA, 1,
				Keys.THETA_MAX, 10);
		
		assertEquals(1, props.getAsInt(Keys.THETA, Keys.THETA_MAX));
		assertEquals(1, props.getAsInt(Keys.THETA, "nonExistentKey"));
		assertNotEquals(1, props.getAsInt("nonExistentKey", Keys.THETA_MAX));
		assertEquals(10, props.getAsInt("nonExistentKey", Keys.THETA_MAX));
		
		exception.expect(IllegalStateException.class);
		props.getAsInt("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultDoubleProps() {
		// test with using a default key
		Props props = Props.fromPairs(
				Keys.THETA, 1.0,
				Keys.THETA_MAX, 10.0);
		
		assertEquals(1.0, props.getAsDouble(Keys.THETA, Keys.THETA_MAX), 1E-6);
		assertEquals(1.0, props.getAsDouble(Keys.THETA, "nonExistentKey"), 1E-6);
		assertNotEquals(1.0, props.getAsDouble("nonExistentKey", Keys.THETA_MAX), 1E-6);
		assertEquals(10.0, props.getAsDouble("nonExistentKey", Keys.THETA_MAX), 1E-6);
		
		exception.expect(IllegalStateException.class);
		props.getAsDouble("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultLongProps() {
		// test with using a default key
		Props props = Props.fromPairs(
				Keys.THETA, 1,
				Keys.THETA_MAX, 10);
		
		assertEquals(1, props.getAsLong(Keys.THETA, Keys.THETA_MAX));
		assertEquals(1, props.getAsLong(Keys.THETA, "nonExistentKey"));
		assertNotEquals(1, props.getAsLong("nonExistentKey", Keys.THETA_MAX));
		assertEquals(10, props.getAsLong("nonExistentKey", Keys.THETA_MAX));
		
		exception.expect(IllegalStateException.class);
		props.getAsLong("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultFloatProps() {
		// test with using a default key
		Props props = Props.fromPairs(
				Keys.THETA, 1.0,
				Keys.THETA_MAX, 10.0);
		
		assertEquals(1.0, props.getAsFloat(Keys.THETA, Keys.THETA_MAX), 1E-6);
		assertEquals(1.0, props.getAsFloat(Keys.THETA, "nonExistentKey"), 1E-6);
		assertNotEquals(1.0, props.getAsFloat("nonExistentKey", Keys.THETA_MAX), 1E-6);
		assertEquals(10.0, props.getAsFloat("nonExistentKey", Keys.THETA_MAX), 1E-6);
		
		exception.expect(IllegalStateException.class);
		props.getAsFloat("nonexistentKey", "nonExistentKey");
	}
	
	@Test
	public void defaultBooleanProps() {
		// test with using a default key
		Props props = Props.fromPairs(
				Keys.THETA, true,
				Keys.THETA_MAX, false);
		
		assertTrue(props.getAsBoolean(Keys.THETA, Keys.THETA_MAX));
		assertTrue(props.getAsBoolean(Keys.THETA, "nonExistentKey"));
		assertFalse(props.getAsBoolean("nonExistentKey", Keys.THETA_MAX));
		assertFalse(props.getAsBoolean("nonExistentKey", Keys.THETA_MAX));
		
		exception.expect(IllegalStateException.class);
		props.getAsBoolean("nonexistentKey", "nonExistentKey");
	}
}
