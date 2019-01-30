package edu.umich.srg.marketsim.market;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

public class Benchmark {
	private static final Map<String, Benchmark> memoized = new HashMap<>();
	
	private final String benchType; //Add final once it is a key
	
	private Benchmark (String benchType) {
		this.benchType = benchType;
	}
	
	public static Benchmark create(String benchType) {
	    return memoized.computeIfAbsent(benchType, Benchmark::new);
	  }
	
	public double calcBenchmark (List<Entry<TimeStamp, Price>> prices) {
		if (benchType.toLowerCase().equals("vwap")) {
			return vwap(prices);
		}
		return 0; // Throw error, that benchmark not available
		
	}
	
	private double vwap (List<Entry<TimeStamp, Price>> prices) {
		int bench = 0;
		double count = 0;
		int price;
		for (Entry<TimeStamp, Price> p : prices) {
			TimeStamp t = p.getKey();
			price = p.getValue().intValue();
			bench += price;
			count += 1;
		}
		
		if (count > 0) {
			return bench / count;
		} else {
			return 0;
		}
	}
}