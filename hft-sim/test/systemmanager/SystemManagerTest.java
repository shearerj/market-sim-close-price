package systemmanager;

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
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;

public class SystemManagerTest {

	/*
	 * TODO Change these tests to verify output
	 */
	
	private static final Gson gson = new Gson();
	private static final String minSpec = createSimulationSpec(
			"numSims", "1",
			"simLength", "60000",
			"tickSize", "1",
			"nbboLatency", "100",
			"marketLatency", "0",
			"arrivalRate", "0.075",
			"reentryRate", "0.0005",
			"fundamentalMean", "100000",
			"fundamentalKappa", "0.05",
			"fundamentalShockVar", "1E8",
			"privateValueVar", "1E8");

	@Test
	public void minimalTest() throws IOException {
		StringReader simSpec = new StringReader(minSpec);
		StringReader properties = new StringReader(
				"logLevel = 0\n" +
				"outputConfig = false\n");
		StringWriter observations = new StringWriter();
		StringWriter logs = new StringWriter();
		SystemManager.execute(simSpec, properties, observations, logs, 0);
		
		System.out.println(observations.toString());
		// FIXME Controlled variates is not NaN or 0, but I don't know why...
//		assertEquals("{\"players\":[],\"features\":{\"control_var_fund\":2.083368056134269E-6,\"spreads_mean_markets\":\"NaN\",\"trans_rmsd\":\"NaN\",\"vol_freq_250_mean_log_return\":\"NaN\",\"spreads_median_nbbo\":\"NaN\",\"vol_mean_log_return\":\"NaN\",\"trans_num\":0.0,\"control_mean_fund\":105657.7877653704,\"vol_freq_250_mean_log_price\":\"NaN\",\"profit_sum_total\":0.0,\"trans_freq_250_rmsd\":\"NaN\",\"surplus_sum_no_disc\":0.0,\"profit_sum_hft\":0.0,\"vol_mean_stddev_price\":\"NaN\",\"exectime_mean\":\"NaN\",\"trans_stddev_price\":\"NaN\",\"control_mean_private\":\"NaN\",\"profit_sum_marketmaker\":0.0,\"trans_mean_price\":\"NaN\",\"surplus_sum_disc_6.0E-4\":0.0,\"profit_sum_background\":0.0,\"vol_mean_log_price\":\"NaN\",\"vol_freq_250_mean_stddev_price\":\"NaN\",\"numSims\":1}}",
//				observations.toString().isEmpty());
		assertTrue(logs.toString().isEmpty());
	}
	
	@Test
	public void stagedTest() throws IOException {
		StringReader simSpec = new StringReader(createSimulationSpec(
				"randomSeed", "271828",
				"numSims", "1",
				"presets", "NONE",
				"simLength", "60000",
				"tickSize", "1",
				"nbboLatency", "100",
				"marketLatency", "0",
				"arrivalRate", "0.075",
				"reentryRate", "0.0005",
				"fundamentalMean", "100000",
				"fundamentalKappa", "0.05",
				"fundamentalShockVar", "1E8",
				"privateValueVar", "1E8",
				"CDA", "num_2",
				"LA", "num_1",
				"ZIR", "num_61_maxQty_10"));
		StringReader properties = new StringReader(
				"logLevel = 2\n" +
				"outputConfig = true\n");
		StringWriter observations = new StringWriter();
		StringWriter logs = new StringWriter();
		SystemManager.execute(simSpec, properties, observations, logs, 0);
		
		assertFalse(observations.toString().isEmpty());
		assertFalse(logs.toString().isEmpty());
	}
	
	@Test
	public void fullTest() throws IOException {
		// Copy Simspec File
		File testDir = new File(Consts.TEST_OUTPUT_DIR, "full_test");
		testDir.mkdirs();
		File simSpecFile = new File(testDir, "simulation_spec.json");
		Writer simSpecWriter = new FileWriter(simSpecFile);
		Reader exampleReader = new FileReader(new File("docs", "simulation_spec.json"));
		char[] buf = new char[8192];
		while (true) {
			int length = exampleReader.read(buf);
			if (length < 0)
				break;
			simSpecWriter.write(buf, 0, length);
		}
		simSpecWriter.close();
		exampleReader.close();
		
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

	protected static String createSimulationSpec(String... keysAndValues) {
		Builder<String, String> builder = ImmutableMap.<String, String> builder();
		for (int i = 0; i < keysAndValues.length; i += 2)
			builder.put(keysAndValues[i], keysAndValues[i+1]);
		return gson.toJson(ImmutableMap.<String, Map<String, String>> of("configuration", builder.build()));
	}

}
