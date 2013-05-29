package data;

import event.*;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;
import org.apache.commons.math3.stat.descriptive.*;


public class TransactionData implements TransactionObject{
	//Private variables for multiMarketData
	double sum;
	double num;
	double min;
	boolean minset;
	double max;
	boolean maxset;
	
	//
	//Public Methods
	//
	//Constructor
	public TransactionData() {
		sum = 0;
		num = 0;
		maxset = false;
		minset = false;
	}
	
	public ArrayList<DataPoint> compute(ArrayList<PQTransaction> transactions){
		//Creating the return ArrayList
		ArrayList<DataPoint> stats = new ArrayList<DataPoint>();
		if(transactions.size() == 0) return stats; //error checking
		
		//Converting ArrayList to an array of doubles
		double[] values = new double[transactions.size()];
		for(int i=0; i < transactions.size(); ++i) {
			values[i] = transactions.get(i).price.getPrice();
		}
		
		//Adding the array to the new descriptive statistics class
		DescriptiveStatistics dp = new DescriptiveStatistics(values);
		
		//Updating multimarket data
		sum += dp.getSum();
		num += dp.getN();
		if(!maxset || dp.getMax() > max) max = dp.getMax();
		if(!minset || dp.getMin() < min) min = dp.getMin();
		
		//Adding the single market statistics to stats
		stats.add(new DataPoint("mean", dp.getMean()));		//adding the mean
		stats.add(new DataPoint("max", dp.getMax()));		//adding maximum
		stats.add(new DataPoint("min", dp.getMin()));		//adding minimum
		stats.add(new DataPoint("var", dp.getVariance()));	//adding variance
		
		return stats;
	}
	
	public ArrayList<DataPoint> multiMarketData(){
		ArrayList<DataPoint> stats = new ArrayList<DataPoint>();
		if(num == 0) return stats;	//error checking
		
		stats.add(new DataPoint("mean", sum/num));
		stats.add(new DataPoint("max", max));
		stats.add(new DataPoint("min", min));
		return stats;
	}

}
