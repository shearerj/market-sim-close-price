package utils;

import org.junit.Test;

/**
 * 
 * A test case to make sure the project and testing are configured appropriately
 * 
 * @author erik
 */
public class ConfigurationTest {

	/** This test will fail if assertions aren't enabled */
	@Test(expected=AssertionError.class)
	public void assertionTest() {
		assert false;
	}

}
