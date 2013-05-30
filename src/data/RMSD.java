package data;

import event.*;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.ArrayList;

public class RMSD implements ObservationObject {
	SystemData data;
	
	public RMSD(SystemData _data) {
		data = _data;
	}
	
	@Override
	public ArrayList<DataPoint> compute(ArrayList<PQTransaction> transactions) {
		ArrayList<DataPoint> stats = new ArrayList<DataPoint>();
		if(transactions.size() == 0) return stats;
		
		double[] prices = new double[transactions.size()];
		double[] values = new double[transactions.size()];
		
		int i =0;
		for(PQTransaction tr : transactions) {
			prices[i] = tr.price.getPrice();
			values[i] = data.getFundamentalAt(tr.timestamp).getPrice();
			++i;
		}
		
		double rmsd = computeRMSD(prices, values);
		
		stats.add(new DataPoint("RMSD", rmsd));
		return null;
	}

	@Override
	public ArrayList<DataPoint> compute() {
		return new ArrayList<DataPoint>();
	}

	@Override
	public ArrayList<DataPoint> multiMarketData() {
		return new ArrayList<DataPoint>();
	}
	
	@Override
	public ArrayList<DataPoint> compute(ArrayList<Double> spreads,
			ArrayList<TimeStamp> times) {
		return new ArrayList<DataPoint>();
	}
	
	//
	//Private Methods
	//
	private double computeRMSD(double [] x1, double [] x2) {
		double rmsd = 0;
		int n = Math.min(x1.length, x2.length);
		// iterate through as many in shorter array
		for (int i = 0; i < n; i++) {
			rmsd += Math.pow(x1[i] - x2[i], 2);	// sum of squared differences
		}
		return Math.sqrt(rmsd / n);
	}
}
