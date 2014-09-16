package data;

import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Random;

import org.junit.Test;

public class FundamentalValueTest {

	@Test(expected=UnsupportedOperationException.class)
	public void immutableTest() {
		Random rand = new Random();
		FundamentalValue fund = FundamentalValue
				.create(rand.nextDouble(), Math.abs(rand.nextInt()) + 1,
						rand.nextDouble() * 100 + 1, rand);
		fund.computeFundamentalTo(1000);
		Iterator<Double> it = fund.iterator();
		it.next();
		it.remove();
		fail();
	}

}
