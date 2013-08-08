package data;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;

public class DSPlus extends DescriptiveStatistics {

	private static final long serialVersionUID = 4016329311188099666L;
	protected static final Median median = new Median();

	public DSPlus() {
		super();
	}

	public DSPlus(int window) {
		super(window);
	}

	public DSPlus(double[] initialDoubleArray) {
		super(initialDoubleArray);
	}

	public DSPlus(DescriptiveStatistics original) {
		super(original);
	}

	public double getRMSD(DescriptiveStatistics other) {
		double rmsd = 0;
		double[] x1 = this.getValues();
		double[] x2 = other.getValues();
		int len = Math.min(x1.length, x2.length);
		int n = 0; // count number of non-NaN values

		// iterate through number of elements in shorter array
		for (int i = 0; i < len; i++) {
			if (!Double.isNaN(x1[i]) && !Double.isNaN(x2[i])) {
				rmsd += Math.pow(x1[i] - x2[i], 2);
				n++;
			}
		}
		return Math.sqrt(rmsd / n);
	}
	
	public double[] getValuesSansNaNs() {
		int nonNans = 0;
		for (double d : getValues())
			if (!Double.isNaN(d)) nonNans++;
		double[] sansNaNs = new double[nonNans];
		int i = 0;
		for (double d : getValues())
			if (!Double.isNaN(d)) sansNaNs[i++] = d;
		return sansNaNs;
	}

	/**
	 * Get's median without NaNs, because NaN's don't make sense for a median calculation.
	 * Disregarding them is equivalent to alternatively making them positive and negative infinity.
	 */
	public double getMedian() {
		return median.evaluate(getValuesSansNaNs());
	}

}
