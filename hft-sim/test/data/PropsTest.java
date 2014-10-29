package data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import props.ParsableValue.DoubleValue;
import systemmanager.Keys.Eta;
import systemmanager.Keys.Gamma;
import systemmanager.Keys.Theta;
import systemmanager.Keys.ThetaMax;

import com.google.common.base.Splitter;

public class PropsTest {
	
	public static class FakeDouble extends DoubleValue {};
	
	private static final double eps = 1e6;
	private static final Splitter configSplit = Splitter.on('_');

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void emptyTest() {
		Props props = Props.fromPairs();
		assertEquals("{}", props.toString());
	}
	
	@Test
	public void parseTest() {
		Props props = Props.fromPairs("gamma", "0.005");	
		assertEquals(0.005, props.get(Gamma.class), 1E-6);
	}
	
	@Test
	public void copyPropsFromPairs() {
		Props props = Props.fromPairs(
				Gamma.class, 0.005,
				Eta.class, 10);
		
		Props copy = Props.withDefaults(props, Theta.class, -1d, Eta.class, 20);
		assertEquals(props.get(Gamma.class), copy.get(Gamma.class), 1E-6);
		assertNotEquals(props.get(Eta.class), copy.get(Eta.class));
		assertEquals(20, (int) copy.get(Eta.class));
	}
	
	@Test
	public void propsFromConfigString() {
		String config = "gamma_0.005_eta_10";
		
		Props props = Props.fromPairs(configSplit.split(config));
		assertEquals(0.005, props.get(Gamma.class), 1E-6);
		assertEquals(10, (int) props.get(Eta.class));
	}

	/** Test that default key (second) works properly */
	@Test
	public void defaultProps() {
		Props props = Props.fromPairs(
				Theta.class, 1d,
				ThetaMax.class, 10d);
		
		assertEquals(1, props.get(Theta.class, ThetaMax.class), eps);
		assertEquals(1, props.get(Theta.class, FakeDouble.class), eps);
		assertEquals(1, props.get(FakeDouble.class, Theta.class), eps);
		assertEquals(10, props.get(FakeDouble.class, ThetaMax.class), eps);
	}
	
}
