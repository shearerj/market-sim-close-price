package edu.umich.srg.marketsim.market;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

import java.util.ArrayList;
import java.util.Collections;

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
		} else if (benchType.toLowerCase().equals("vwmp")) {
			return vwmp(prices);
		}
		else {
			return 0;
		}
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
	
	private double vwmp (List<Entry<TimeStamp, Price>> prices) {
		double count = 0;
		int price;
		List<Integer> p_only = new ArrayList<Integer>();
		for (Entry<TimeStamp, Price> p : prices) {
			TimeStamp t = p.getKey();
			price = p.getValue().intValue();
			p_only.add(price);
			count += 1;
			System.out.println(price);
		}
		System.out.println(count);
		Collections.sort(p_only);
		if (count > 0) {
			if (count % 2 == 1) {
				return p_only.get((int) (count/2 - 0.5));
			}
			else {
				return ((double) (p_only.get((int) (count/2 - 1))) + (p_only.get((int) (count/2)))) / 2;
			}
		} else {
			return 0;
		}
	}
}