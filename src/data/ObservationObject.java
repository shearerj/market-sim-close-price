package data;

import event.*;
import model.*;
import market.*;
import activity.*;
import systemmanager.*;

import java.util.ArrayList; 

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.math3.stat.descriptive.*;



/**
 * Base class for Observation function objects
 * Overload compute to return statistical observations
 * Use member variables to average across multiple markets
 * 
 * @author drhurd
 *
 */
public interface ObservationObject {
	/**
	 * Overload this function to compute single market statistics (returned by this function)
	 * Should also update any multimarket statistics (returned by multimarketdata)
	 * @return
	 */
	public ArrayList<DataPoint> compute();
	public ArrayList<DataPoint> compute(ArrayList<PQTransaction> transactions);
	public ArrayList<DataPoint> compute(ArrayList<Double> spreads, ArrayList<TimeStamp> times);
	

	/**
	 * Returns statistics that are aggregated across multiple markets 
	 * @return
	 */
	public abstract ArrayList<DataPoint> multiMarketData();

}