package edu.umich.srg.marketsim.privatevalue;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.util.RepeatRule;
import edu.umich.srg.util.TestInts;

@RunWith(Theories.class)
public class AbstractListPrivateValueTest {
	
	private static final Random rand = new Random();
	private static final DoubleDistribution[] distributions = {Gaussian.withMeanVariance(0, 100), Uniform.continuous(0, 1000)};
	
	@Rule
	public RepeatRule repeatRule = new RepeatRule();
	
	@Theory
	public void decreasingMarginalValueTest(@TestInts({1, 10, 100}) int maxPos, @TestInts({0, 1}) int distIndex) {
		DoubleDistribution dist = distributions[distIndex];
		PrivateValue pv = new AbstractListPrivateValue(dist, maxPos, rand) { };
		for (int pos = -maxPos; pos < maxPos - 1; ++pos)
			assertTrue(pv.valueForExchange(pos, BUY) > pv.valueForExchange(pos + 1, BUY));
		for (int pos = maxPos; pos > -maxPos + 1; --pos)
			assertTrue(pv.valueForExchange(pos, SELL) > pv.valueForExchange(pos - 1, SELL));
	}

}
