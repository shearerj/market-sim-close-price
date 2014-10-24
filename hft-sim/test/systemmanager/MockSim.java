package systemmanager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import logger.Log;
import logger.Log.Level;
import systemmanager.Consts.MarketType;
import systemmanager.Keys.NumMarkets;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import data.FundamentalValue;
import data.Props;
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
	
	private MockSim(Writer writer, Log.Level logLevel, JsonObject spec) {
		super(SimulationSpec.fromJson(spec), rand, writer, logLevel);
	}
	
	private static MockSim create(Writer writer, Log.Level logLevel, Props globalProps, Map<MarketType,Props> marketProps) {
		JsonObject root = new JsonObject();
		JsonObject config = SimulationSpec.propsToJson(globalProps);
		for (Entry<MarketType, Props> e : marketProps.entrySet())
			config.addProperty(e.getKey().toString(), SimulationSpec.propsToConfig(e.getValue()));
		root.add(SimulationSpec.CONFIG, config);
		return new MockSim(writer, logLevel, root);
	}
	
	public static MockSim createWithErrLogging(Log.Level logLevel, Props globalProps, Map<MarketType,Props> marketProps) {
		return MockSim.create(new PrintWriter(System.err), logLevel, globalProps, marketProps);
	}
	
	private static MockSim create(String name, Level logLevel, Props globalProps, Map<MarketType,Props> marketProps) throws IOException {
		return MockSim.create(new FileWriter(new File(testLogDir, name + ".log"), true), logLevel, globalProps, marketProps);
	}
	
	private static MockSim create(Class<?> testClass, Level logLevel, Props globalProps, Map<MarketType,Props> marketProps) throws IOException {
		return MockSim.create(testClass.getSimpleName(), logLevel, globalProps, marketProps);
	}
	
	public static MockSim create(Class<?> testClass, Level logLevel, MarketType type, int numMarkets, Props globalProps) throws IOException {
		return MockSim.create(testClass, logLevel, globalProps, ImmutableMap.of(type, Props.fromPairs(NumMarkets.class, numMarkets)));
	}
	
	public static MockSim createCDA(Class<?> testClass, Level logLevel, int numMarkets, Props globalProps) throws IOException {
		return MockSim.create(testClass, logLevel, MarketType.CDA, numMarkets, globalProps);
	}
	
	public static MockSim createCDA(Class<?> testClass, Level logLevel, int numMarkets) throws IOException {
		return MockSim.createCDA(testClass, logLevel, numMarkets, Props.fromPairs());
	}
	
	public static MockSim create(Class<?> testClass, Level logLevel, Props globalProps) throws IOException {
		return MockSim.createCDA(testClass, logLevel, 0, globalProps);
	}
	
	public static MockSim create(Class<?> testClass, Level logLevel) throws IOException {
		return MockSim.create(testClass, logLevel, Props.fromPairs());
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
	
	public void executeUntil(TimeStamp time) {
		eventQueue.executeUntil(time);
	}
	
	public void executeImmediate() {
		eventQueue.executeUntil(TimeStamp.IMMEDIATE);
	}

}
