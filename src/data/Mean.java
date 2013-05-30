package data;

import java.util.ArrayList;

import event.TimeStamp;

import market.PQTransaction;

public class Mean implements ObservationObject {

	@Override
	public ArrayList<DataPoint> compute(ArrayList<PQTransaction> transactions) {
		ArrayList<DataPoint> stats = new ArrayList<DataPoint>();
		
		//Finding the total
		double total = new Double(0);
		for(PQTransaction itr : transactions) {
			total += itr.price.getPrice();
		}
		
		//Finding num
		double avg = (transactions.size() > 0) ? total/transactions.size() : 0;
		
		
		//Creating the DataPoint
		DataPoint temp = new DataPoint("mean", avg);
		
		//Adding mean to the ArrayList
		stats.add(temp);
		
		return stats;
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

}
