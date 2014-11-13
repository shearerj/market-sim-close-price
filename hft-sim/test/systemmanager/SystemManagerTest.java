package systemmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class SystemManagerTest {
	
	private static final Gson gson = new Gson();

	// FIXME Change these tests to verify output
	// FIXME Multiple simulation test. Use fixed random seed. Generate first n independently and then generate one with n simulations and comapre

	@Test
	public void minimalTest() throws IOException {
		StringReader simSpec = new StringReader("{configuration: {" +
				"numSims: 1," +
				"simLength: 60000," +
				"tickSize: 1," +
				"nbboLatency: 100," +
				"marketLatency: 0," +
				"arrivalRate: 0.075," +
				"reentryRate: 0.0005," +
				"fundamentalMean: 100000," +
				"fundamentalKappa: 0.05," +
				"fundamentalShockVar: 1E8," +
				"privateValueVar: 1E8," +
				"CDA: \"num_1\"" +
				"}}");
		StringReader properties = new StringReader("logLevel = 0\noutputConfig = false\n");
		StringWriter observations = new StringWriter();
		StringWriter logs = new StringWriter();
		SystemManager.execute(simSpec, properties, observations, logs, 0);
		
//		assertEquals("{\"players\":[],\"features\":{\"control_var_fund\":2.083368056134269E-6,\"spreads_mean_markets\":\"NaN\",\"trans_rmsd\":\"NaN\",\"vol_freq_250_mean_log_return\":\"NaN\",\"spreads_median_nbbo\":\"NaN\",\"vol_mean_log_return\":\"NaN\",\"trans_num\":0.0,\"control_mean_fund\":105657.7877653704,\"vol_freq_250_mean_log_price\":\"NaN\",\"profit_sum_total\":0.0,\"trans_freq_250_rmsd\":\"NaN\",\"surplus_sum_no_disc\":0.0,\"profit_sum_hft\":0.0,\"vol_mean_stddev_price\":\"NaN\",\"exectime_mean\":\"NaN\",\"trans_stddev_price\":\"NaN\",\"control_mean_private\":\"NaN\",\"profit_sum_marketmaker\":0.0,\"trans_mean_price\":\"NaN\",\"surplus_sum_disc_6.0E-4\":0.0,\"profit_sum_background\":0.0,\"vol_mean_log_price\":\"NaN\",\"vol_freq_250_mean_stddev_price\":\"NaN\",\"numSims\":1}}",
//				observations.toString().isEmpty());
		assertTrue(logs.toString().isEmpty());
	}
	
	@Test
	public void playersTest() throws IOException {
		StringReader simSpec = new StringReader("{assignment: {" +
				"role: [\"NOOP\", \"ZIR\"]" +
				"}," +
				"configuration: {" +
				"numSims: 1," +
				"simLength: 60000," +
				"tickSize: 1," +
				"nbboLatency: 100," +
				"marketLatency: 0," +
				"arrivalRate: 0.075," +
				"reentryRate: 0.0005," +
				"fundamentalMean: 100000," +
				"fundamentalKappa: 0.05," +
				"fundamentalShockVar: 1E8," +
				"privateValueVar: 1E8," +
				"CDA: \"num_1\"" +
				"}}");
		StringReader properties = new StringReader("logLevel = 0\noutputConfig = false\n");
		StringWriter observations = new StringWriter();
		StringWriter logs = new StringWriter();
		SystemManager.execute(simSpec, properties, observations, logs, 0);
	}
	
	@Test
	public void stagedTest() throws IOException {
		StringReader simSpec = new StringReader("{configuration: {" +
				"randomSeed: 271828," +
				"simLength: 60000," +
				"nbboLatency: 1000," +
				"marketLatency: 0," +
				"arrivalRate: 0.075," +
				"reentryRate: 0.0005," +
				"fundamentalMean: 100000," +
				"fundamentalKappa: 0.05," +
				"fundamentalShockVar: 1E8," +
				"privateValueVar: 1E8," +
				"CDA: \"num_2\"," +
				"LA: \"num_1\"," +
				"ZIR: \"num_61_maxPosition_10\"" +
				"}}");
		StringReader properties = new StringReader("logLevel = 2\noutputConfig = true\n");
		StringWriter observations = new StringWriter();
		StringWriter logs = new StringWriter();
		SystemManager.execute(simSpec, properties, observations, logs, 0);
		
		assertFalse(observations.toString().isEmpty());
		assertFalse(logs.toString().isEmpty());
	}
	
	// TODO This should have players
	@Test
	public void fullTest() throws IOException {
		// Copy Simspec File
		File testDir = new File(Consts.TEST_OUTPUT_DIR, "full_test");
		testDir.mkdirs();
		copyFile(new File("docs", "simulation_spec.json"), new File(testDir, "simulation_spec.json"));
		
		// Bookkeeping
		File logDir = new File(testDir, "logs");
		int numLogs = logDir.exists() ? logDir.listFiles().length : 0;
		
		// Run test
		SystemManager.execute(testDir, 0);
		
		// Verify Results
		File observations = new File(testDir, "observation0.json");
		assertTrue(observations.exists());
		assertTrue(observations.canWrite());
		assertTrue(observations.length() > 0);
		
		assertTrue(logDir.listFiles().length > numLogs);
	}
	
	/** Test that spec is written out correctly when outputConfig is set to true */
	@Test
	public void outputConfigTest() throws IOException {
		String spec = "{configuration: {" +
				"numSims: 1," +
				"simLength: 60000," +
				"tickSize: 1," +
				"nbboLatency: 100," +
				"marketLatency: 0," +
				"arrivalRate: 0.075," +
				"reentryRate: 0.0005," +
				"fundamentalMean: 100000," +
				"fundamentalKappa: 0.05," +
				"fundamentalShockVar: 1E8," +
				"privateValueVar: 1E8," +
				"CDA: \"num_1\"" +
				"}}";
		
		StringReader simSpec = new StringReader(spec);
		StringReader properties = new StringReader("logLevel = 0\noutputConfig = true\n");
		StringWriter observations = new StringWriter();
		StringWriter logs = new StringWriter();
		SystemManager.execute(simSpec, properties, observations, logs, 0);

		assertEquals(
				gson.fromJson(spec, JsonObject.class), // Spec
				gson.fromJson(observations.toString(), JsonObject.class).get("features").getAsJsonObject().get("config") // Spec in obseration
				);
	}
	
	private static void copyFile(File from, File to) throws IOException {
		Writer simSpecWriter = new FileWriter(to);
		Reader exampleReader = new FileReader(from);
		char[] buf = new char[8192];
		while (true) {
			int length = exampleReader.read(buf);
			if (length < 0)
				break;
			simSpecWriter.write(buf, 0, length);
		}
		simSpecWriter.close();
		exampleReader.close();
	}

}
