package data;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class FundamentalValueTest {

	@Test
	public void asTimeSeriesTest() {
		for (int i = 0; i < 1000; ++i) {
			Random rand = new Random();
			FundamentalValue fund = FundamentalValue.create(rand.nextDouble(), Math.abs(rand.nextInt()) + 1, rand.nextDouble()*100 + 1, rand);
			fund.computeFundamentalTo(1000);
			assertEquals(fund.meanRevertProcess, fund.asTimeSeries().sample(1, 1001));
		}
	}

}
