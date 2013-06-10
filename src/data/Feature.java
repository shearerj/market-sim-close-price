package data;

import systemmanager.Consts;

import java.text.DecimalFormat;
import java.util.HashMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;


/**
 * Wrapper method to handle writing of features to the observation file.
 * All statistics-computation method return the computed metric (double).
 * 
 * Handles rounding to significant figures.
 * 
 * @author ewah
 */
public class Feature {

	private HashMap<String, Object> ft;
	private DecimalFormat df;

	// Methods
	public final static String MEAN = "mean";
	public final static String MAX = "max";
	public final static String MIN = "min";
	public final static String SUM = "sum";
	public final static String MEDIAN = "med";
	public final static String VARIANCE = "var";
	public final static String STDDEV = "std";
	public final static String RMSD = "rmsd";
	
	
	public Feature() {
		ft = new HashMap<String, Object>();
		df = new DecimalFormat("#.###");
	}
	
	public HashMap<String, Object> get() {
		return ft;
	}
	
	public void put(String key, Object obj) {
		ft.put(key, obj);
	}
	
	public void add(String prefix, String desc, String suffix,
			Object val) {
		String key = prefix;
		if (!prefix.isEmpty()) key += "_";
		key += desc;
		if (!suffix.isEmpty()) key += "_" + suffix;
		
		if (val instanceof String) {
			ft.put(key, val);
		} else {
			if (((Double) val).equals(Consts.DOUBLE_NAN)) {
				ft.put(key, Consts.NAN);
			} else {
				ft.put(key, Double.parseDouble(df.format((Double) val)));
			}
		}
	}
	
	public boolean isEmpty() {
		return ft.isEmpty();
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return ft.toString();
	}
	
	
	/********************************************
	 * Statistics
	 *******************************************/
	
	public double addMean(DescriptiveStatistics ds) {
		return addMean("", "", ds);
	}
	
	public double addMean(String pre, String suf, DescriptiveStatistics ds) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			val = ds.getMean();
		}
		add(pre, MEAN, suf, val);
		return val;
	}
	
	/**
	 * @param ds
	 * @param feat
	 * @param windowSize	most recent values contributing to mean
	 * @return
	 */
	public double addWindowMean(String pre, String suf,
			DescriptiveStatistics ds, int windowSize) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			DescriptiveStatistics copy = new DescriptiveStatistics(ds);
			copy.setWindowSize(Math.max((int) ds.getN(), windowSize));
			val = copy.getMean();
		}
		add(pre, MEAN, suf, val);
		return val;
	}

	public double addMax(DescriptiveStatistics ds) {
		return addMax("", "", ds);
	}
	
	public double addMax(String pre, String suf, DescriptiveStatistics ds) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			val = ds.getMax();
		}
		add(pre, MAX, suf, val);
		return val;
	}
	
	public double addMin(DescriptiveStatistics ds) {
		return addMin("", "", ds);
	}
	
	public double addMin(String pre, String suf, DescriptiveStatistics ds) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			val = ds.getMin();
		}
		add(pre, MIN, suf, val);
		return val;
	}
	
	public double addSum(DescriptiveStatistics ds) {
		return addSum("", "", ds);
	}
	
	public double addSum(String pre, String suf, DescriptiveStatistics ds) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			val = ds.getSum();
		}
		add(pre, SUM, suf, val);
		return val;
	}
	
	public double addVariance(DescriptiveStatistics ds) {
		return addVariance("", "", ds);
	}
	
	public double addVariance(String pre, String suf, DescriptiveStatistics ds) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			val = ds.getVariance();
		}
		add(pre, VARIANCE, suf, val);
		return val;
	}
	
	public double addMedian(DescriptiveStatistics ds) {
		return addMedian("", "", ds);
	}
	
	public double addMedian(String pre, String suf, DescriptiveStatistics ds) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			Median med = new Median();
			val = med.evaluate(ds.getValues());
		}
		add(pre, MEDIAN, suf, val);
		return val;
	}
	
	/**
	 * @param pre
	 * @param suf
	 * @param ds
	 * @param idx	cut off time series at this index
	 */
	public double addMedianUpToTime(String pre, String suf, 
			DescriptiveStatistics ds, long idx) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			Median med = new Median();
			if (idx < ds.getN()) {
				val = med.evaluate(ds.getValues(), 0, (int) idx);
			} else {
				val = med.evaluate(ds.getValues());
			}
		}
		add(pre, MEDIAN, suf, val);
		return val;
	}
	
	public double addStdDev(DescriptiveStatistics ds) {
		return addStdDev("", "", ds);
	}
	
	public double addStdDev(String pre, String suf, DescriptiveStatistics ds) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			StandardDeviation std = new StandardDeviation();
			val = std.evaluate(ds.getValues());
		}
		add(pre, STDDEV, suf, val);
		return val;
	}
	
	/**
	 * @param pre
	 * @param suf
	 * @param ds
	 * @param idx	cut off time series at this index
	 */
	public double addStdDevUpToTime(String pre, String suf, 
			DescriptiveStatistics ds, long idx) {
		double val = Consts.DOUBLE_NAN;
		if (ds.getN() > 0) {
			StandardDeviation std = new StandardDeviation();
			if (idx < ds.getN()) {
				val = std.evaluate(ds.getValues(), 0, (int) idx);
			} else {
				val = std.evaluate(ds.getValues());
			}
		}
		add(pre, STDDEV, suf, val);
		return val;
	}
	
	/**
	 * @param ds1
	 * @param ds2
	 * @return
	 */
	public double addRMSD(DescriptiveStatistics ds1, DescriptiveStatistics ds2) {
		return addRMSD("", "", ds1, ds2);
	}
	
	/**
	 * @param pre
	 * @param suf
	 * @param ds1
	 * @param ds2
	 * @return
	 */
	public double addRMSD(String pre, String suf,
			DescriptiveStatistics ds1, DescriptiveStatistics ds2) {
		double val = Consts.DOUBLE_NAN;
		if (ds1.getN() > 0 && ds2.getN() > 0) {
			val = computeRMSD(ds1.getValues(), ds2.getValues());
		}
		add(pre, RMSD, suf, val);
		return val;
	}
	
	/**
	 * Returns root mean square deviation metric.
	 * 
	 * @param x1
	 * @param x2
	 * @return
	 */
	private double computeRMSD(double [] x1, double [] x2) {
		double rmsd = 0;
		int len = Math.min(x1.length, x2.length);
		int n = 0;		// count number of non-NaN values
		// iterate through number of elements in shorter array
		for (int i = 0; i < len; i++) {
			if (!Double.isNaN(x1[i]) && !Double.isNaN(x2[i])) {
				rmsd += Math.pow(x1[i] - x2[i], 2);	// sum of squared differences
				n++;
			}
		}
		return Math.sqrt(rmsd / n);
	}

}
