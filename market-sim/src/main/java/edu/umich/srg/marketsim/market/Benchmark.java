package edu.umich.srg.marketsim.market;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import java.util.Random;
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
		} 
		else if (benchType.toLowerCase().equals("vwmp")) {
			return vwmp(prices);
		}
		else if (benchType.toLowerCase().equals("twap")) {
			return twap(prices);
		}
		else if (benchType.toLowerCase().equals("rvwap")) {
			return rvwap(prices);
		}
		else if (benchType.toLowerCase().equals("rtwap")) {
			return rtwap(prices);
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
	
	
	private double twap (List<Entry<TimeStamp, Price>> prices) {
		int bench = 0;
		double count = 0;
		// TODO: add sim length
		int partitions = 100;
		int price;
		int weight;
		for (Entry<TimeStamp, Price> p : prices) {
			TimeStamp t = p.getKey();
			price = p.getValue().intValue();
			weight = (int)t.get() / partitions;
			bench += price * weight;
			count += weight;
		}
		if (count > 0) {
			return bench / count;
		} else {
			return 0;
		}
	}
	
	private double rvwap (List<Entry<TimeStamp, Price>> prices) {
		int bench = 0;
		int price;
		double percent = 0.1;
		// subsection of trades analyzed for benchmark should be 1/10th size of total trades
		int subSize = (int)(prices.size() * percent);
		Random rand = new Random();
		// start of random range should be in first 9/10ths of trades
		int subStart = rand.nextInt((int)(prices.size() - subSize));
		for (int i = subStart; i < subStart + subSize; ++i) {
			price = prices.get(i).getValue().intValue();
			bench += price;
		}
		if (subSize > 0) {
			return (double)bench / (double)subSize;
		} else {
			return 0;
		}
	}
	
	private double rtwap (List<Entry<TimeStamp, Price>> prices) {
		int bench = 0;
		double count = 0;
		// min of weight range
		int weightMin = 1;
		// max of weight range
		int weightMax = 10;
		int price;
		int weight;
		Random rand = new Random();
		for (Entry<TimeStamp, Price> p : prices) {
			TimeStamp t = p.getKey();
			price = p.getValue().intValue();
			weight = rand.nextInt(weightMax - weightMin + 1) + weightMin;
			bench += price * weight;
			count += weight;
		}
		if (count > 0) {
			return bench / count;
		} else {
			return 0;
		}
	}
}