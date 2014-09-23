package systemmanager;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Random;

import logger.Log;
import logger.Log.Level;

import com.google.gson.JsonObject;

import data.FundamentalValue;
import data.Stats;

import entity.market.Market;
import event.TimeStamp;

/**
 * Class that provides the ability to "simulate" an even manager. This
 * guarantees that residual effects of actions get propagated (such as
 * information spreading), as well as the ability to set the time
 * 
 * @author erik
 * 
 */
public class MockSim extends Simulation {

	protected static final Random rand = new Random();
	protected static final File testLogDir = new File(new File("simulations"), "unit_testing");
	
	static {
		if (testLogDir.exists()) // Delete all log files
			for (File f : testLogDir.listFiles())
				f.delete();
		testLogDir.mkdirs();
	}
	
	protected MockSim(Writer writer, Log.Level logLevel, JsonObject spec) {
		super(SimulationSpec.fromJson(spec), rand, writer, logLevel);
	}
	
	protected static MockSim create(Writer writer, Log.Level logLevel, Object... globalProps) {
		checkArgument(globalProps.length %2 == 0, "Must pass an even number of pairs");
		JsonObject root = new JsonObject();
		JsonObject config = new JsonObject();
		for (int i = 0; i < globalProps.length; i += 2)
			config.addProperty(globalProps[i].toString(), globalProps[i+1].toString());
		root.add(Keys.CONFIG, config);
		return new MockSim(writer, logLevel, root);
	}
	
	public static MockSim createWithErrLogging(Log.Level logLevel, Object... globalProps) {
		return MockSim.create(new PrintWriter(System.err), logLevel, globalProps);
	}
	
	public static MockSim createWithNoLogging(Object... globalProps) {
		return MockSim.create(new Writer() {
			@Override public void write(char[] cbuf, int off, int len) throws IOException { }
			@Override public void flush() throws IOException { }
			@Override public void close() throws IOException { }
		}, Log.Level.NO_LOGGING, globalProps);
	}
	
	public static MockSim create(String name, Level logLevel, Object... globalProps) throws IOException {
		return MockSim.create(new FileWriter(new File(testLogDir, name + ".log"), true), logLevel, globalProps);
	}
	
	public static MockSim create(Class<?> testClass, Level logLevel, Object... globalProps) throws IOException {
		return MockSim.create(testClass.getSimpleName(), logLevel, globalProps);
	}

	public Collection<Market> getMarkets() {
		return markets;
	}
	
	public Stats getStats() {
		return statistics;
	}
	
	public FundamentalValue getFundamental() {
		return fundamental;
	}
	
	// Sets currentTime to time
	public void executeUntil(TimeStamp time) {
		eventQueue.executeUntil(time);
	}
	
	public void executeImmediate() {
		eventQueue.executeUntil(TimeStamp.IMMEDIATE);
	}

}
